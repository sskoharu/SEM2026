package org.app.services.layer_4.c1;

import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.tools.CalculationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class CalculationPlannerAgent {

    private final ChatClient chatClient;
    private final CalculationTools calculationTools;

    public CalculationPlannerAgent(ChatClient.Builder builder, CalculationTools calculationTools) {
        this.chatClient = builder.build();
        this.calculationTools = calculationTools;
    }

    /**
     * Analiza el executionPlan y decide qué herramientas de cálculo ejecutar.
     */
    public CalculationPlan planCalculations(SessionObject session) {
        log.info("Planificando cálculos deterministas para la sesión: {}", session.getSessionId());

        String executionPlan = session.getExecutionPlan().toString();

        String promptTemplate = """
            <ROL>
            Eres un Planificador de Cálculos Matemáticos. Tu tarea es leer un "Plan de Ejecución" y generar las instrucciones exactas para calcular los valores que están pendientes.
            </ROL>
            
            <ESCENARIO>
            Plan de Ejecución actual (JSON):
            [EXECUTION_PLAN]
            
            Tienes a tu disposición una serie de herramientas (Tools) para hacer cálculos exactos (fechas, intereses, porcentajes).
            </ESCENARIO>
            
            <CONSIGNA>
            1. Analiza el Plan de Ejecución. Busca las variables que tengan el valor `"PENDIENTE_CALCULO"` o que estén vacías (`null`) y requieran un cálculo.
            2. Para cada variable pendiente, selecciona la herramienta adecuada.
            3. Extrae los parámetros necesarios para la herramienta desde el mismo Plan de Ejecución (ej. si necesitas días, busca el `plazo_maximo`).
            4. Genera las instrucciones con el formato exacto de salida.
            
            REGLAS ESTRICTAS:
            - Usa SOLO las herramientas que se te proporcionan. No inventes herramientas ni fórmulas.
            - Los argumentos deben coincidir exactamente con los parámetros requeridos por la herramienta.
            - Si una variable pendiente no se puede calcular con las herramientas disponibles, ignórala.
            </CONSIGNA>
            
            <TIPO_DE_SALIDA>
            Devuelve ÚNICAMENTE un JSON válido con la estructura de cálculo:
            {
              "tasks": [
                {
                  "toolName": "nombre_exacto_de_la_herramienta",
                  "arguments": {
                    "parametro1": valor1,
                    "parametro2": valor2
                  },
                  "targetVariable": "nombre_de_la_variable_a_actualizar"
                }
              ]
            }
            </TIPO_DE_SALIDA>
            """;

        String prompt = promptTemplate
                .replace("[EXECUTION_PLAN]", executionPlan);

        // 1. Llamamos al LLM con las herramientas inyectadas
        CalculationPlan plan = chatClient.prompt()
                .user(prompt)
                .tools(calculationTools) // Spring AI expone los @Tool al LLM aquí
                .call()
                .entity(CalculationPlan.class);

        log.info("Plan de cálculos generado con {} tareas.",
                (plan != null && plan.getTasks() != null) ? plan.getTasks().size() : 0);

        return plan;
    }
}