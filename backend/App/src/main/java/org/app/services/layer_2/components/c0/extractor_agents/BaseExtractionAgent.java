package org.app.services.layer_2.components.c0.extractor_agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.schemas.interfaces.layer_2.ILayer2ExtractionAgent;
import org.app.services.PreExtractionResult;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

public abstract class BaseExtractionAgent implements ILayer2ExtractionAgent {

    protected final ChatClient chatClient;
    protected final ObjectMapper objectMapper;

    public BaseExtractionAgent(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Reglas de extracción específicas del tipo de reclamo.
     */
    protected abstract String getExtractionRules();

    /**
     * Reglas de filtrado (irrelevantParameters) específicas del tipo.
     */
    protected abstract String getIrrelevantRules();

    /**
     * Reglas procedimentales específicas del tipo.
     */
    protected abstract String getProceduralRules();

    @Override
    public PreExtractionResult extract(String narrative, List<String> requiredParameters) {

        // Construimos el prompt combinando las reglas generales (base) con las específicas (hijo)
        String prompt = """
            <ROL>
            Eres un analista experto en extracción, filtrado y clasificación de información legal para la LODC de Ecuador.
            </ROL>
            
            <ESCENARIO>
            Relato del usuario: "%s"
            Parámetros requeridos por la ley: %s
            </ESCENARIO>
            
            <CONSIGNA>
            Debes procesar los parámetros de la lista en 3 pasos estrictos:

            PASO 1. EXTRACCIÓN (extractedParameters): 
            Extrae los valores de los parámetros que el usuario YA MENCIONÓ en su relato. Sé muy preciso con los formatos generales:
            - Si menciona una fecha (ej. "15 de enero de 2026"), extráela como "DD/MM/AAAA" (ej. "15/01/2026").
            - Si menciona un monto (ej. "900 dólares"), extráelo como un string numérico (ej. "900").
            - Si menciona tener (o no) algo, extrae booleanos (true/false).
            
            Reglas específicas para este tipo de reclamo:
            %s

            PASO 2. FILTRADO DE RELEVANCIA (irrelevantParameters):
            Marca aquí ÚNICAMENTE los parámetros que sean **físicamente imposibles** para la naturaleza del bien.
            - Si el usuario no menciona un dato, NO lo marques como irrelevante, simplemente déjalo fuera de la extracción.
            Reglas específicas:
            %s

            PASO 3. CLASIFICACIÓN PROCEDIMENTAL (proceduralParameters):
            Marca aquí los parámetros que representan **ACCIONES, REMEDIOS, CÁLCULOS o CONCEPTOS TÉCNICOS** que NO se le deben preguntar al usuario.
            - Cualquier parámetro que comience con "claims_", "requests_", "sanction_" o que suene a "calcular" o "evaluar un contexto técnico".
            Reglas específicas:
            %s
            </CONSIGNA>
            
            <TIPO_DE_SALIDA>
            Devuelve ÚNICAMENTE este JSON válido (sin markdown):
            {
              "extractedParameters": { "param1": "valor1" },
              "irrelevantParameters": ["param2"],
              "proceduralParameters": ["param3"]
            }
            </TIPO_DE_SALIDA>
            """.formatted(
                narrative,
                requiredParameters.toString(),
                getExtractionRules(),
                getIrrelevantRules(),
                getProceduralRules()
        );

        try {
            String rawResponse = chatClient.prompt().system(org.app.services.ModelInputPolicy.SYSTEM).user(prompt).call().content();
            String cleanJson = rawResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleanJson, PreExtractionResult.class);
        } catch (Exception e) {
            return new PreExtractionResult(null, null, null);
        }
    }
}