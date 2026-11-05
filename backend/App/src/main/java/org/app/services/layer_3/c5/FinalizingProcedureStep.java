package org.app.services.layer_3.c5;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.exceptions.ClientException;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.app.services.layer_3.dto.ActionParameter;
import org.app.services.layer_3.dto.ProceduralOption;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class FinalizingProcedureStep implements Layer3Step {

    @Override
    public Layer3InternalState getState() {
        return Layer3InternalState.FINALIZING_PROCEDURE;
    }

    @Override
    public Layer3InternalState execute(Layer3ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> FINALIZING_PROCEDURE | Consolidando datos para la Capa 4...");

        // 1. Obtener la opción que el usuario eligió
        ProceduralOption selectedOption = session.getActionRouteOptions().getOptions().stream()
                .filter(o -> o.getOptionId().equals(session.getSelectedOptionId()))
                .findFirst()
                .orElseThrow(() -> ClientException.error("La opción seleccionada no existe: " + session.getSelectedOptionId()));

        // 2. Crear el Plan de Ejecución
        Map<String, Object> plan = new HashMap<>();

        // Agregamos el artículo base (ej. "71") y el título
        plan.put("base_article", selectedOption.getBasedOnArticle());
        plan.put("action_title", selectedOption.getTitle());

        // 3. Inyectar los parámetros que llenó el usuario
        if (session.getProceduralFormData() != null) {
            plan.putAll(session.getProceduralFormData());
        }

        // 4. Inyectar las tareas de cálculo determinista (CORREGIDO)
        List<ActionParameter> deterministicTasks = selectedOption.getDeterministicCalculationsRequired();
        if (deterministicTasks != null && !deterministicTasks.isEmpty()) {
            for (ActionParameter task : deterministicTasks) {

                // Si es un STATIC_INPUT, guardamos su valor real (ej. "30")
                if ("STATIC_INPUT".equals(task.getParameterRole())) {
                    String staticValue = task.getCurrentValue() != null ? task.getCurrentValue() : "0";
                    plan.put(task.getParameterName(), staticValue);
                    log.debug("Parámetro estático '{}' inyectado con valor: {}", task.getParameterName(), staticValue);
                }
                // Si es un DYNAMIC_OUTPUT (o no tiene rol definido), lo marcamos como pendiente
                else {
                    plan.put(task.getParameterName(), "PENDIENTE_CALCULO");
                    log.debug("Parámetro dinámico '{}' marcado como PENDIENTE_CALCULO.", task.getParameterName());
                }
            }
        }

        // 5. Guardar el plan en la sesión
        session.setExecutionPlan(plan);
        log.info("Plan de ejecución generado para Capa 4: {}", plan);

        // 6. Terminamos la Capa 3
        return Layer3InternalState.DONE;
    }
}