package org.app.services.layer_3.c3;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.app.services.layer_1.components.c2.VectorSearchTool;
import org.app.services.layer_3.dto.ActionRouteResult;
import org.app.services.layer_3.dto.ProceduralOption;
import org.app.services.layer_3.dto.ActionParameter;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Log4j2
public class GeneratingActionRouteStep implements Layer3Step {

    private final ActionRouteAgent actionRouteAgent;
    private final VectorSearchTool vectorSearchTool;

    @Override
    public Layer3InternalState getState() {
        return Layer3InternalState.GENERATING_ACTION_ROUTE;
    }

    @Override
    public Layer3InternalState execute(Layer3ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> GENERATING_ACTION_ROUTE | Generando propuestas de proceder legal...");

        // 1. Obtener los artículos procedimentales
        List<String> proceduralArticleNumbers = session.getProceduralArticles();

        // Fallback: Si no hay procedimentales explícitos, usamos los aplicables
        if (proceduralArticleNumbers == null || proceduralArticleNumbers.isEmpty()) {
            log.warn("No se encontraron artículos procedimentales. Usando los aplicables como respaldo.");
            proceduralArticleNumbers = session.getApplicableArticles();
        }

        // 2. Buscar el texto completo en Qdrant
        List<Document> docs = vectorSearchTool.getArticlesByNumbers(proceduralArticleNumbers);
        String proceduralText = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 3. Llamar al Agente para generar las opciones
        ActionRouteResult result = actionRouteAgent.generateActionRoutes(session, proceduralText);

        // 4. VALIDACIÓN Y LIMPIEZA (Evitar duplicidad de tiempo/plazos)
        if (result != null && result.getOptions() != null) {
            for (ProceduralOption option : result.getOptions()) {
                removeDuplicateParameters(option);
            }
            session.setActionRouteOptions(result);
            log.info("Se generaron {} opciones de proceder legal.", result.getOptions().size());
        } else {
            log.error("El ActionRouteAgent no devolvió opciones válidas.");
            session.setClarificationMessage("No se pudieron generar las opciones de proceder legal.");
            return Layer3InternalState.ERROR;
        }

        // 5. Pausar el flujo para que el usuario elija
        return Layer3InternalState.WAITING_FOR_ACTION_SELECTION;
    }

    /**
     * Elimina parámetros duplicados (ej. que el LLM pregunte por el plazo legal
     * cuando ya lo está calculando como determinista).
     */
    private void removeDuplicateParameters(ProceduralOption option) {
        if (option.getDeterministicCalculationsRequired() == null || option.getUserInputsRequired() == null) {
            return;
        }

        // Obtenemos los nombres de los parámetros que el sistema va a calcular
        Set<String> deterministicNames = option.getDeterministicCalculationsRequired().stream()
                .map(ActionParameter::getParameterName)
                .collect(Collectors.toSet());

        // Filtramos los inputs del usuario para eliminar los que coincidan
        // o los que sean de tiempo si ya hay un cálculo determinista de fecha.
        option.getUserInputsRequired().removeIf(userParam ->
                deterministicNames.contains(userParam.getParameterName()) ||
                        (userParam.getParameterName().contains("plazo") && deterministicNames.stream().anyMatch(d -> d.contains("fecha")))
        );

        // Si la lista queda vacía, la seteamos a null para limpiar el JSON
        if (option.getUserInputsRequired().isEmpty()) {
            option.setUserInputsRequired(null);
        }
    }
}