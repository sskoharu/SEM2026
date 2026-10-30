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

        // Obtenemos la fecha actual para darle contexto al LLM
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int currentYear = LocalDate.now().getYear();

        String prompt = """
            <ROL>
            Eres un agente validador y normalizador de datos para un sistema legal.
            La fecha actual del sistema es: %s (Año actual: %d).
            </ROL>
            
            <ESCENARIO>
            El usuario ingresó el dato: "%s" para el parámetro: "%s".
            </ESCENARIO>
            
            <CONSIGNA>
            1. NORMALIZACIÓN: Si el parámetro es una fecha, el usuario puede haberla escrito en lenguaje natural 
               (ej. "2 de julio de este año", "el mes pasado", "30 agosto de 2026", "hace 3 días").
               Tu primera tarea es razonar y CONVERTIR esa expresión a un formato estándar DD/MM/AAAA.
            
            2. VALIDACIÓN: Una vez normalizada, usa la herramienta de validación adecuada 
               (validateDate para fechas, validateAmount para montos, validateText para textos libres).
               Pásale a la herramienta el valor YA NORMALIZADO.
            
            3. RESPUESTA:
               - Si la herramienta devuelve "OK", responde estrictamente con: "OK: [valor_normalizado]"
               - Si la herramienta devuelve "ERROR", responde con el error y una sugerencia.
            </CONSIGNA>
            """.formatted(currentDate, currentYear, userInput, parameterName);

        // Usamos Tool Calling
        return chatClient.prompt()
                .user(prompt)
                .tools(validationTools)
                .call()
                .content();
    }
}