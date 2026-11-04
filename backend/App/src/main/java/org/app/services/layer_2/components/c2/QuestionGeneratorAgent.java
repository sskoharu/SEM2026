package org.app.services.layer_2.components.c2;

import org.app.schemas.session_object.enums.ClaimType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class QuestionGeneratorAgent {

    private final ChatClient chatClient;

    public QuestionGeneratorAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // NUEVO PARÁMETRO: narrative (el relato original)
    public String generateQuestion(String parameterName, ClaimType claimType, String previousError, String previousQuestion, String narrative) {
        String errorContext = (previousError != null && !previousError.trim().isEmpty())
                ? "ATENCIÓN: El usuario intentó responder antes, pero cometió este error: '" + previousError + "'. Disculpate brevemente."
                : "Es la primera vez que le preguntamos esto.";

        String prompt = """
            <ROL>
            Eres un asistente legal empático y servicial.
            </ROL>
            
            <ESCENARIO>
            Relato original del usuario: "%s"
            Estamos armando un reclamo de tipo: %s.
            La pregunta anterior que le hiciste fue: "%s".
            Ahora necesitamos que el usuario nos diga el dato técnico: "%s".
            %s
            </ESCENARIO>
            
            <CONSIGNA>
            Redacta UNA SOLA PREGUNTA para pedir el nuevo dato al usuario.
            
            REGLAS ESTRICTAS:
            1. NO uses jerga legal ni nombres técnicos.
            2. Sé conversacional y directo.
            3. Si hay un error previo, explica cómo debe responder.
            4. **PROHIBIDO** decir que vas a "revisar", "investigar" o "analizar" documentos.
            5. **REGLA CRÍTICA PARA PRECIOS**: Si el parámetro es un precio (como `displayedPriceWithTaxes`, `chargedPrice`, `missingUnitPrice`), DEBES especificar a qué precio te refieres. 
               - Si el relato menciona un precio de anuncio y otro cobrado, pregunta específicamente: "¿A cuál de los precios te refieres: al que decía el anuncio o al que te cobraron finalmente?"
               - Si el parámetro es `missingUnitPrice`, pregunta: "En el anuncio se mostraba un precio total, ¿te cobraron un precio unitario diferente al que esperabas?"
            6. Devuelve ÚNICAMENTE el texto de la pregunta.
            </CONSIGNA>
            """.formatted(narrative, claimType.name(), previousQuestion != null ? previousQuestion : "Ninguna", parameterName, errorContext);

        return chatClient.prompt().system(org.app.services.ModelInputPolicy.SYSTEM)
                .user(prompt)
                .call()
                .content()
                .trim();
    }
}