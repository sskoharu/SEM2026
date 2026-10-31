package org.app.services.layer_1.components.c3;

import org.app.services.context.layer_1.LegalTranslationResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LegalTranslatorAgent {
    private final ChatClient chatClient;

    public LegalTranslatorAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public LegalTranslationResult translateAndEvaluate(String originalNarrative, List<Document> failedDocs, int attempt) {
        String context = (failedDocs != null && !failedDocs.isEmpty())
                ? "Artículos recuperados previamente (irrelevantes): " + failedDocs.stream().map(Document::getText).limit(2).collect(Collectors.joining(" | "))
                : "Primer intento de búsqueda.";

        String prompt = """
            <ROL>
            Eres un experto en la Ley Orgánica de Defensa del Consumidor (LODC) de Ecuador.
            </ROL>
            
            <ESCENARIO>
            Relato del usuario en lenguaje coloquial:
            <RELATO>
            %s
            </RELATO>
            
            Contexto de búsqueda anterior: %s (Intento #%d)
            </ESCENARIO>
            
            <CONSIGNA>
            1. COHERENCIA JURÍDICA: Evalúa si este relato tiene relación con la LODC (defensa del consumidor). 
               Si el usuario habla de temas penales, laborales, de familia o civiles que NO son de consumo, marca is_legally_coherent como false.
            2. TRADUCCIÓN: Si es coherente, traduce el relato a una única frase de búsqueda con terminología legal exacta de la LODC (ej. "vicios ocultos", "garantía", "suspensión de servicio", "facturación", etc.). Si no es coherente, devuelve una cadena vacía.
                <REGLA_DE_DESEMPATE>
                Si el relato menciona un producto nuevo que falló y el usuario tiene garantía, clasifícalo OBLIGATORIAMENTE como "VICIOS_OCULTOS_CALIDAD" (Tipo 1).\s
                NO lo clasifiques como "SERVICIO_TECNICO_CUSTODIA" (Tipo 2) a menos que el problema principal sea que el taller de reparación dañó el equipo o usó repuestos falsos. El simple hecho de que el usuario haya consultado a un técnico NO convierte el caso en un reclamo de servicio técnico.
                </REGLA_DE_DESEMPATE> 
                
                <REGLA_DE_DESEMPATE_CRITICA>
                Si el usuario menciona un producto nuevo que falló, y además menciona que tiene factura o garantía, clasifícalo OBLIGATORIAMENTE como "VICIOS_OCULTOS_CALIDAD" (Tipo 1).\s
                El simple hecho de que el usuario haya consultado a un "técnico independiente" para pedir una opinión NO convierte el caso en un reclamo de "SERVICIO_TECNICO_CUSTODIA". El Tipo 2 es exclusivo para cuando el taller hace una mala reparación o daña el equipo.
                </REGLA_DE_DESEMPATE_CRITICA>
            
            </CONSIGNA>
            
            
            """.formatted(originalNarrative, context, attempt);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(LegalTranslationResult.class);
    }
}