package org.app.services.layer_3.c3;

import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_3.dto.ActionParameter;
import org.app.services.layer_3.dto.ActionRouteResult;
import org.app.services.layer_3.dto.ProceduralOption;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
public class ActionRouteAgent {

    private final ChatClient chatClient;

    public ActionRouteAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public ActionRouteResult generateActionRoutes(SessionObject session, String proceduralArticlesText) {
        log.info("Generando opciones de ruta de acción para el tipo: {}", session.getClaimType());

        String narrative = session.getConflictDetails().getDescription();
        String claimType = session.getClaimType().name();
        String collectedParams = org.app.services.ModelInputPolicy.safeParameters(session.getCollectedParameters());

        String promptTemplate = """
            <ROL>
            Eres un Abogado experto en la LODC de Ecuador. Tu tarea es generar opciones de "ruta de acción" (proceder legal) claramente delimitadas para que el usuario elija.
            </ROL>
            
            <ESCENARIO>
            Tipo de Reclamo: [CLAIM_TYPE]
            Hechos del usuario: "[NARRATIVA]"
            Datos ya recolectados del usuario: [COLLECTED_PARAMS]
            
            Cada opción debe incluir sourceId y supportingQuote. supportingQuote debe ser
            una cita textual exacta de al menos diez caracteres de la fuente sourceId.
            No crees opciones sin respaldo. No decidas constantes jurídicas por memoria.
            Artículos de proceder aplicables (Sanciones/Acciones):
            [PROCEDURAL_ARTICLES]
            </ESCENARIO>
            
            <DELIMITACION_ESTRICTA>
            Es OBLIGATORIO que cada opción sea un compartimento estanco e independiente. Sigue estas reglas inquebrantables:
            
            1. ATOMICIDAD DE LA ACCIÓN: Cada objeto en la lista `options` debe contener UN ÚNICO remedio legal.
               - PROHIBIDO combinar remedios en un solo título o descripción (Ej. MAL: "Reparación o Reposición").
               - El `optionId` debe reflejar esa única acción (ej. `OPCION_REPARACION`, `OPCION_DEVOLUCION`).

            2. DELIMITACIÓN DE PARÁMETROS POR OPCIÓN: Los parámetros definidos en cada opción deben ser exclusivos para ejecutar ESA acción.
               - PROHIBIDO hacer preguntas al usuario que impliquen una elección entre remedios.
               - Solo incluye en `userInputsRequired` datos logísticos o personales.

            3. DELIMITACIÓN DE TIEMPOS: Los plazos legales (ej. 30 días) NO se le preguntan al usuario. 
               - Si la acción requiere calcular una fecha, inclúyela ÚNICAMENTE en `deterministicCalculationsRequired`.

            4. ROLES DE PARÁMETROS: Debes asignar el campo `parameterRole` a cada parámetro generado:
               - Para `userInputsRequired`: Usa siempre `"USER_INPUT"`.
               - Para `deterministicCalculationsRequired`: Distingue entre:
                 * `"STATIC_INPUT"`: Si es un valor fijo o constante legal conocida que servirá como base para un cálculo (ej. "plazo_maximo_reparacion" con valor 30).
                 * `"DYNAMIC_OUTPUT"`: Si es el resultado que el sistema debe calcular (ej. "fecha_limite_cumplimiento" o "monto_total_devolucion").
            </DELIMITACION_ESTRICTA>
            
            <CONSIGNA>
            1. Analiza los artículos de proceder.
            2. Genera las opciones aplicando estrictamente las reglas de <DELIMITACION_ESTRICTA>.
            3. Para cada opción, define su `optionId`, `title`, `description`, `basedOnArticle`, `userInputsRequired` y `deterministicCalculationsRequired`.
            4. Recuerda que los `Datos ya recolectados` se inyectarán después, NO los repitas en los inputs del usuario.
            </CONSIGNA>
            
            <TIPO_DE_SALIDA>
            Devuelve ÚNICAMENTE un JSON válido con esta estructura:
            {
              "options": [
                {
                  "optionId": "OPCION_REPARACION",
                  "title": "Reparación sin Costo Adicional",
                  "description": "Conforme al Art. 22, exijo la reparación...",
                  "basedOnArticle": "22",
                  "userInputsRequired": [
                    {
                      "parameterName": "modalidad_entrega",
                      "deterministic": false,
                      "parameterRole": "USER_INPUT",
                      "currentValue": null,
                      "questionForUser": "¿Desea llevar el equipo al taller o que lo retiren en su domicilio?"
                    }
                  ],
                  "deterministicCalculationsRequired": [
                    {
                      "parameterName": "plazo_maximo_reparacion",
                      "deterministic": true,
                      "parameterRole": "STATIC_INPUT",
                      "currentValue": "30",
                      "questionForUser": null
                    },
                    {
                      "parameterName": "fecha_limite_cumplimiento",
                      "deterministic": true,
                      "parameterRole": "DYNAMIC_OUTPUT",
                      "currentValue": null,
                      "questionForUser": null
                    }
                  ]
                }
              ]
            }
            </TIPO_DE_SALIDA>
            """;

        String prompt = promptTemplate
                .replace("[CLAIM_TYPE]", claimType)
                .replace("[NARRATIVA]", narrative)
                .replace("[COLLECTED_PARAMS]", collectedParams)
                .replace("[PROCEDURAL_ARTICLES]", proceduralArticlesText);

        ActionRouteResult result = chatClient.prompt().system(org.app.services.ModelInputPolicy.SYSTEM)
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
                log.debug("Parámetro recopilado incorporado a la opción: {}", param.getParameterName());
            }
        }
    }
}