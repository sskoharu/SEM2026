package org.app.services.layer_4;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.services.layer_4.c1.CalculationPlannerAgent;
import org.app.services.layer_4.c2.CalculationExecutorService;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.resolver.CatalogResolverService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Log4j2
public class Layer4Orchestrator {

    private final CatalogResolverService catalogResolver;
    private final CalculationPlannerAgent plannerAgent;
    private final CalculationExecutorService executorService;
    private final SessionRepository sessionRepository;

    public SessionObject process(SessionObject session) {
        log.info("Iniciando Capa 4 (Cálculos Deterministas) para sesión: {}", session.getSessionId());

        // 1. Resolución determinista con el catálogo (contexto, constantes, tools, marcadores)
        Set<String> unmatched = catalogResolver.resolve(session);

        // 2. Respaldo: el LLM solo planifica las variables que el catálogo no conoce
        if (!unmatched.isEmpty()) {
            CalculationPlan plan = plannerAgent.planCalculations(session, unmatched);
            if (plan != null && plan.getTasks() != null) {
                plan.setTasks(plan.getTasks().stream()
                        .filter(t -> t.getTargetVariable() != null && unmatched.contains(t.getTargetVariable()))
                        .toList());
            }
            executorService.executeCalculations(session, plan);
        }

        // 3. Actualizamos el estado macro para pasar a la Capa 5
        session.setCurrentPhase(ClaimSessionState.DOCUMENT_GENERATION);
        sessionRepository.save(session);

        log.info("Capa 4 finalizada. executionPlan actualizado: {}", session.getExecutionPlan());
        return session;
    }
}
