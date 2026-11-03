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
import java.util.List;

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

        List<ActionParameter> required = selectedOption.getUserInputsRequired() == null
                ? List.of() : selectedOption.getUserInputsRequired();
        Map<String, Object> formData = context.getProceduralFormData();

        if (formData != null && !formData.isEmpty()) {
            log.info("Validando los datos del formulario de proceder...");
            boolean hasErrors = false;
            for (Map.Entry<String, Object> entry : formData.entrySet()) {
                String paramName = entry.getKey();
                ActionParameter parameter = required.stream()
                        .filter(p -> p.getParameterName().equals(paramName))
                        .findFirst().orElse(null);
                if (parameter == null) {
                    log.warn("Se recibió un parámetro no solicitado en el formulario.");
                    hasErrors = true;
                    continue;
                }
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue()).trim();
                if (value.isEmpty()) {
                    updateParameterError(selectedOption, paramName, "El campo es obligatorio.");
                    hasErrors = true;
                    continue;
                }
                String result = validationAgent.validateParameter(paramName, value);
                if (result == null || !result.startsWith("OK:")) {
                    hasErrors = true;
                    String error = result != null && result.startsWith("ERROR:")
                            ? result.substring("ERROR:".length()).trim()
                            : "No se pudo validar este dato.";
                    updateParameterError(selectedOption, paramName, error);
                    continue;
                }
                String normalizedValue = result.substring("OK:".length()).trim();
                if (normalizedValue.isEmpty()) {
                    hasErrors = true;
                    updateParameterError(selectedOption, paramName, "El dato validado está vacío.");
                    continue;
                }
                if (session.getProceduralFormData() == null) {
                    session.setProceduralFormData(new java.util.HashMap<>());
                }
                session.getProceduralFormData().put(paramName, normalizedValue);
                updateParameterValue(selectedOption, paramName, normalizedValue);
            }
            if (hasErrors) {
                return Layer3InternalState.WAITING_FOR_PROCEDURAL_INPUT;
            }
        }

        boolean missingData = required.stream().anyMatch(param ->
                param.getCurrentValue() == null || param.getCurrentValue().isBlank());
        if (missingData) {
            log.info("Faltan datos obligatorios del proceder.");
            return Layer3InternalState.WAITING_FOR_PROCEDURAL_INPUT;
        }
        return Layer3InternalState.FINALIZING_PROCEDURE;
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
        option.getUserInputsRequired().stream()
                .filter(p -> p.getParameterName().equals(paramName))
                .findFirst()
                .ifPresent(p -> p.setQuestionForUser("⚠️ Error: " + errorMsg));
    }

    private void updateParameterValue(ProceduralOption option, String paramName, String value) {
        option.getUserInputsRequired().stream()
                .filter(p -> p.getParameterName().equals(paramName))
                .findFirst()
                .ifPresent(p -> p.setCurrentValue(value));
    }
}