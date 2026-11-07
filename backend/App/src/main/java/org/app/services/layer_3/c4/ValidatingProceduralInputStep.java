package org.app.services.layer_3.c4;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.app.services.layer_2.components.c3.ValidationClassifierAgent; // Reutilizamos Capa 2
import org.app.services.layer_3.dto.ActionParameter;
import org.app.services.layer_3.dto.ProceduralOption;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class ValidatingProceduralInputStep implements Layer3Step {

    private final ValidationClassifierAgent validationAgent;

    @Override
    public Layer3InternalState getState() {
        return Layer3InternalState.VALIDATING_PROCEDURAL_INPUT;
    }

    @Override
    public Layer3InternalState execute(Layer3ExecutionContext context) {
        SessionObject session = context.getSession();
        ProceduralOption selectedOption = getSelectedOption(session);

        if (selectedOption == null) {
            log.error("No se encontró la opción seleccionada: {}", session.getSelectedOptionId());
            return Layer3InternalState.ERROR;
        }

        Map<String, Object> formData = context.getProceduralFormData();

        // CASO A: El usuario acaba de enviar el formulario (Tenemos datos)
        if (formData != null && !formData.isEmpty()) {
            log.info("Validando los datos del formulario de proceder...");
            boolean hasErrors = false;

            for (Map.Entry<String, Object> entry : formData.entrySet()) {
                String paramName = entry.getKey();
                String value = String.valueOf(entry.getValue());

                // Usamos el Agente de la Capa 2 para validar
                String result = validationAgent.validateParameter(paramName, value);

                if (result.startsWith("ERROR")) {
                    hasErrors = true;
                    updateParameterError(selectedOption, paramName, result.replace("ERROR:", "").trim());
                } else {
                    // Si es válido, lo guardamos en proceduralFormData (Map)
                    String normalizedValue = result.startsWith("OK:") ? result.replace("OK:", "").trim() : value;

                    if (session.getProceduralFormData() == null) {
                        session.setProceduralFormData(new java.util.HashMap<>());
                    }
                    session.getProceduralFormData().put(paramName, normalizedValue);

                    updateParameterValue(selectedOption, paramName, normalizedValue);
                }
            }

            if (hasErrors) {
                log.warn("El formulario tiene errores. Volviendo a pausar para corregir.");
                return Layer3InternalState.WAITING_FOR_PROCEDURAL_INPUT;
            }

            log.info("Formulario validado exitosamente. Pasando a FINALIZING_PROCEDURE.");
            return Layer3InternalState.FINALIZING_PROCEDURE; // <-- CORRECCIÓN AQUÍ
        }

        // CASO B: Es la primera vez que entramos (o no hay datos)
        // Verificamos si faltan datos por llenar
        boolean missingData = false;
        java.util.List<ActionParameter> required = selectedOption.getUserInputsRequired() != null
                ? selectedOption.getUserInputsRequired() : java.util.List.of();
        for (ActionParameter param : required) {
            if (param.getCurrentValue() == null || param.getCurrentValue().isEmpty()) {
                missingData = true;
                break;
            }
        }

        if (missingData) {
            log.info("Faltan datos del proceder. Pausando para mostrar el formulario.");
            return Layer3InternalState.WAITING_FOR_PROCEDURAL_INPUT;
        }

        log.info("Todos los datos del proceder ya estaban llenos. Pasando a FINALIZING_PROCEDURE.");
        return Layer3InternalState.FINALIZING_PROCEDURE; // <-- CORRECCIÓN AQUÍ
    }

    // --- Métodos auxiliares ---

    private ProceduralOption getSelectedOption(SessionObject session) {
        if (session.getActionRouteOptions() == null) return null;
        return session.getActionRouteOptions().getOptions().stream()
                .filter(o -> o.getOptionId().equals(session.getSelectedOptionId()))
                .findFirst()
                .orElse(null);
    }

    private void updateParameterError(ProceduralOption option, String paramName, String errorMsg) {
        if (option.getUserInputsRequired() == null) return;
        option.getUserInputsRequired().stream()
                .filter(p -> p.getParameterName().equals(paramName))
                .findFirst()
                .ifPresent(p -> p.setQuestionForUser("⚠️ Error: " + errorMsg));
    }

    private void updateParameterValue(ProceduralOption option, String paramName, String value) {
        if (option.getUserInputsRequired() == null) return;
        option.getUserInputsRequired().stream()
                .filter(p -> p.getParameterName().equals(paramName))
                .findFirst()
                .ifPresent(p -> p.setCurrentValue(value));
    }
}