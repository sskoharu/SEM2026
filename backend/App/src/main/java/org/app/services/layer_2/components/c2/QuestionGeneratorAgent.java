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

    public String generateQuestion(String parameterName, ClaimType claimType, String previousError) {
        String errorContext = (previousError != null && !previousError.trim().isEmpty())
                ? "ATENCIÓN: El usuario intentó responder antes, pero cometió este error: '" + previousError + "'. Disculpate y explica cómo corregirlo."
                : "Es la primera vez que le preguntamos esto.";

        String prompt = """
            <ROL>
            Eres un asistente legal empático y servicial.
            </ROL>
            <ESCENARIO>
            Reclamo: %s.
            Parámetro faltante: "%s".
            %s
            </ESCENARIO>
            <CONSIGNA>
            Redacta UNA ÚNICA PREGUNTA para pedir este dato.
            
            REGLAS ESTRICTAS:
            1. NO uses jerga legal ni nombres técnicos.
            2. Sé conversacional y directo.
            3. Devuelve SOLO el texto de la pregunta, sin comillas ni explicaciones.
            4. NO hagas más de una pregunta en el mismo texto.
            </CONSIGNA>
            """.formatted(claimType.name(), parameterName, errorContext);

        return chatClient.prompt().user(prompt).call().content().trim();
    }
}