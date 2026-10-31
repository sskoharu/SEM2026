package org.app.services.layer_2.components.c0;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PreExtractionAgent {

    private final ChatClient chatClient;

    public PreExtractionAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String extractAndFilterParameters(String narrative, List<String> requiredParameters) {
        // Usamos placeholders personalizados para evitar conflictos con el símbolo %
        String promptTemplate = """
            <ROL>
            Eres un analista experto en extracción y filtrado de información legal.
            </ROL>
            
            <ESCENARIO>
            Relato del usuario: "[NARRATIVA]"
            Parámetros requeridos por la ley: [PARAMETROS]
            </ESCENARIO>
            
            <CONSIGNA>
            1. EXTRACCIÓN: Extrae los valores para los parámetros que YA HAYAN SIDO MENCIONADOS en el relato.
            
            2. FILTRADO DE RELEVANCIA (¡REGLA CRÍTICA!): 
               Marca como `irrelevantParameters` ÚNICAMENTE los parámetros que sean **físicamente imposibles** de aplicar a la naturaleza del bien.
               - Ejemplo VÁLIDO de irrelevante: Pedir `missingWeightOrMeasure` (peso) para un servicio de software o para un sillón (los muebles no se venden por peso).
               - Ejemplo INVÁLIDO de irrelevante: Si el usuario no mencionó si tenía un comprobante adicional, NO lo marques como irrelevante. Solo déjalo fuera de ambas listas para que el sistema se lo pregunte después.
               - REGLA DE ORO: Si un parámetro simplemente falta por preguntar, NO lo pongas en `irrelevantParameters`.
            
            3. NO inventes información. No incluyas parámetros en `extractedParameters` si no están en el texto.
            </CONSIGNA>
            
            <TIPO_DE_SALIDA>
            Devuelve ÚNICAMENTE este JSON válido (sin markdown, sin comillas triples):
            {
              "extractedParameters": {
                "invoiceProvided": false,
                "providerRefusedInvoice": true
              },
              "irrelevantParameters": []
            }
            </TIPO_DE_SALIDA>
            """;

        // Reemplazamos los placeholders de forma segura
        String prompt = promptTemplate
                .replace("[NARRATIVA]", narrative)
                .replace("[PARAMETROS]", requiredParameters.toString());

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}