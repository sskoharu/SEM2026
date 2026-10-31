package org.app.services.layer_2.components.c3;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ValidationClassifierAgent {
    private final ChatClient chatClient;
    private final ValidationTools validationTools;

    public ValidationClassifierAgent(ChatClient.Builder builder, ValidationTools validationTools) {
        this.chatClient = builder.build();
        this.validationTools = validationTools;
    }

    public String validateParameter(String parameterName, String userInput) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int currentYear = LocalDate.now().getYear();

        String prompt = """
            <ROL>
            Eres un agente validador y normalizador de datos.
            La fecha actual es: %s (Año actual: %d).
            </ROL>
            
            <ESCENARIO>
            El usuario ingresó el dato: "%s" para el parámetro: "%s".
            </ESCENARIO>
            
            <CONSIGNA>
            1. NORMALIZACIÓN: Si el parámetro es una fecha, conviértela a DD/MM/AAAA.
            2. VALIDACIÓN: Usa la herramienta adecuada (validateDate, validateAmount, validateText).
            3. RESPUESTA:
               - Si es "OK", responde: "OK: [valor_normalizado]"
               - Si es "ERROR", responde con el error y una sugerencia.
               
            REGLA ESTRICTA: NUNCA menciones que vas a "revisar documentos" o "evidencias". 
            Solo evalúa el texto ingresado.
            </CONSIGNA>
            """.formatted(currentDate, currentYear, userInput, parameterName);

        return chatClient.prompt()
                .user(prompt)
                .tools(validationTools)
                .call()
                .content();
    }
}