package org.app.services.layer_5.c1;

import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_5.dto.DraftingResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@Log4j2
public class DocumentDraftingAgent {

    private final ChatClient chatClient;

    public DocumentDraftingAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * Redacta las secciones de hechos y pretensiones para el documento final.
     */
    public DraftingResult draftSections(SessionObject session) {
        log.info("Iniciando redacción de secciones para la sesión: {}", session.getSessionId());

        String rawNarrative = session.getConflictDetails().getDescription();

        // Convertimos los parámetros recolectados a un texto legible
        String collectedParams = org.app.services.ModelInputPolicy.safeParameters(session.getCollectedParameters());

        // Extraemos el plan de ejecución (que tiene la acción elegida y los cálculos)
        String executionPlan = org.app.services.ModelInputPolicy.safeParameters(session.getExecutionPlan());

        String viability = session.getViabilityReport();

        String promptTemplate = """
            <ROL>
            Eres un Abogado experto en redacción de documentos legales para la LODC de Ecuador.
            Tu tarea es redactar dos secciones de un reclamo formal: los HECHOS y las PRETENSIONES.
            </ROL>
            
            <ESCENARIO>
            Relato original del usuario: "[RAW_NARRATIVE]"
            
            Datos adicionales recolectados:
            [COLLECTED_PARAMS]
            
            Plan de Ejecución (Acción elegida por el usuario y cálculos):
            [EXECUTION_PLAN]
            
            Informe de viabilidad:
            [VIABILITY]
            </ESCENARIO>
            
            <CONSIGNA>
            1. HECHOS: Redacta un párrafo cronológico y formal. Relata cómo el usuario adquirió el bien o servicio, cuál fue el problema y cómo el proveedor falló. Usa un tono jurídico (ej. "El día X, el compareciente adquirió...").
            2. PRETENSIONES: Redacta un párrafo solicitando formalmente la acción que el usuario eligió (basándote en el `action_title` y `base_article` del Plan de Ejecución). Incluye los montos calculados si existen.
            
            REGLAS ESTRICTAS:
            - NO uses placeholders como [nombre] o [fecha]. No añadas nombres: el servidor incorpora la identificación en la plantilla local.
            - NO inventes hechos que no estén en el relato o en los parámetros.
            - Mantén un lenguaje formal, en primera persona (ej. "solicito", "manifiesto").
            </CONSIGNA>
            
            <TIPO_DE_SALIDA>
            Devuelve ÚNICAMENTE este JSON válido:
            {
              "hechos_redactados": "El día 10 de enero de 2026, el compareciente adquirió...",
              "pretensiones_redactadas": "En virtud de lo expuesto, solicito formalmente..."
            }
            </TIPO_DE_SALIDA>
            """;

        String prompt = promptTemplate
                .replace("[RAW_NARRATIVE]", rawNarrative)
                .replace("[COLLECTED_PARAMS]", collectedParams)
                .replace("[EXECUTION_PLAN]", executionPlan)
                .replace("[VIABILITY]", viability != null ? viability : "No disponible");

        DraftingResult result = chatClient.prompt().system(org.app.services.ModelInputPolicy.SYSTEM)
                .user(prompt)
                .call()
                .entity(DraftingResult.class);

        log.info("Redacción completada exitosamente.");
        return result;
    }
}