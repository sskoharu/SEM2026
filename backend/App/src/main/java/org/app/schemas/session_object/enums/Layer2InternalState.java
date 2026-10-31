package org.app.schemas.session_object.enums;

public enum Layer2InternalState {
    PRE_POPULATING_PARAMETERS,
    DETERMINING_PARAMETERS,   // 1. Lee el ClaimType y define qué datos faltan
    GENERATING_QUESTIONS,     // 3. El LLM redacta la pregunta amigable
    WAITING_FOR_USER,         // 4. PAUSA: Espera que el usuario responda en el Frontend
    VALIDATING_INPUT,         // 5. Verifica que la respuesta del usuario sea lógica
    DONE,                     // 6. Terminó la recolección
    SOLICITANDO_ACLARACION,   // 7. La respuesta fue inválida, se repite la pregunta
    ERROR
}