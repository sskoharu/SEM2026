package org.app.services.layer_4.c1;

import lombok.extern.log4j.Log4j2;
import org.app.prompts.PromptLoader;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.tools.CalculationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
@Log4j2
public class CalculationPlannerAgent {

    private final ChatClient chatClient;
    private final CalculationTools calculationTools;

    private final PromptLoader promptLoader;

    public CalculationPlannerAgent(ChatClient.Builder builder, CalculationTools calculationTools, PromptLoader promptLoader) {
        this.chatClient = builder.build();
        this.calculationTools = calculationTools;
        this.promptLoader = promptLoader;
    }

    /**
     * Analiza el executionPlan y decide qué herramientas de cálculo ejecutar.
     */
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
    public CalculationPlan planCalculations(SessionObject session, java.util.Set<String> pendingVariables) {
        log.info("Planificando cálculos deterministas para la sesión: {}", session.getSessionId());

        String executionPlan = session.getExecutionPlan().toString();
        String pending = String.join(", ", pendingVariables);

        String prompt = promptLoader.render("layer_4/calculation_planner", PromptLoader.vars(
                "plan_ejecucion", executionPlan,
                "variables_pendientes", pending));

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