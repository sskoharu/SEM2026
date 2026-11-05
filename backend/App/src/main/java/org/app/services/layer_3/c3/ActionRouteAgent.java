package org.app.services.layer_3.c3;

import lombok.extern.log4j.Log4j2;
import org.app.prompts.PromptLoader;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_3.dto.ActionParameter;
import org.app.services.layer_3.dto.ActionRouteResult;
import org.app.services.layer_3.dto.ProceduralOption;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

@Service
@Log4j2
public class ActionRouteAgent {

    private final ChatClient chatClient;

    private final PromptLoader promptLoader;

    public ActionRouteAgent(ChatClient.Builder builder, PromptLoader promptLoader) {
        this.chatClient = builder.build();
        this.promptLoader = promptLoader;
    }

    @Retryable(
            value = {
                    TransientAiException.class, // Límites de tasa (429) y errores transitorios del proveedor
                    ResourceAccessException.class, // Timeouts de red/conexión
                    RuntimeException.class // Respuestas vacías o inválidas
            },
            maxRetries = 3,
            delay = 1000L,
            multiplier = 2.0
    )
    public ActionRouteResult generateActionRoutes(SessionObject session, String proceduralArticlesText) {
        log.info("Generando opciones de ruta de acción para el tipo: {}", session.getClaimType());

        String narrative = session.getConflictDetails().getDescription();
        String claimType = session.getClaimType().name();
        String collectedParams = session.getCollectedParameters().toString();

        String prompt = promptLoader.render("layer_3/action_route", PromptLoader.vars(
                "tipo_reclamo", claimType,
                "narrativa", narrative,
                "parametros_recolectados", collectedParams,
                "articulos_procedimentales", proceduralArticlesText));

        ActionRouteResult result = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(ActionRouteResult.class);

        // POST-PROCESAMIENTO DE SEGURIDAD
        if (result != null && result.getOptions() != null) {
            for (ProceduralOption option : result.getOptions()) {
                processUserInputs(option.getUserInputsRequired(), session);
                processDeterministicCalculations(option.getDeterministicCalculationsRequired(), session);
            }
        }

        return result;
    }

    /**
     * Limpia y estandariza los inputs del usuario.
     */
    private void processUserInputs(List<ActionParameter> parameters, SessionObject session) {
        if (parameters == null) return;
        for (ActionParameter param : parameters) {
            param.setDeterministic(false); // Forzar a false
            param.setParameterRole("USER_INPUT"); // Forzar rol
        }
        injectExistingParameters(parameters, session);
    }

    /**
     * Limpia, corrige y clasifica los parámetros deterministas.
     */
    private void processDeterministicCalculations(List<ActionParameter> parameters, SessionObject session) {
        if (parameters == null) return;

        for (ActionParameter param : parameters) {
            param.setDeterministic(true); // Forzar a true siempre
            param.setQuestionForUser(null); // El usuario nunca ve esto

            // Si el LLM olvidó poner el rol, lo inferimos
            if (param.getParameterRole() == null || param.getParameterRole().isEmpty()) {
                String name = param.getParameterName().toLowerCase();
                if (name.contains("fecha") || name.contains("monto") || name.contains("interes") || name.contains("total")) {
                    param.setParameterRole("DYNAMIC_OUTPUT");
                } else {
                    param.setParameterRole("STATIC_INPUT");
                }
            }

            // Si es un Output dinámico, borramos cualquier valor pre-asignado para que la Capa 4 lo calcule
            if ("DYNAMIC_OUTPUT".equals(param.getParameterRole())) {
                param.setCurrentValue(null);
            }
        }

        injectExistingParameters(parameters, session);
    }

    /**
     * Busca si el parámetro ya fue respondido en la Capa 2 y lo inyecta.
     * (Solo inyecta si el rol NO es DYNAMIC_OUTPUT).
     */
    private void injectExistingParameters(List<ActionParameter> parameters, SessionObject session) {
        if (parameters == null) return;

        for (ActionParameter param : parameters) {
            // No inyectar si es un cálculo pendiente
            if ("DYNAMIC_OUTPUT".equals(param.getParameterRole())) {
                continue;
            }

            Object existingValue = session.getCollectedParameters().get(param.getParameterName());
            if (existingValue != null) {
                param.setCurrentValue(existingValue.toString());
                log.debug("Parámetro '{}' inyectado desde Capa 2 con valor: {}", param.getParameterName(), existingValue);
            }
        }
    }
}