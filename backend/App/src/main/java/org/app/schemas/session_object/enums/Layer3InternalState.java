package org.app.schemas.session_object.enums;

public enum Layer3InternalState {

    // 0. Compuerta: verifica los requisitos mínimos de la Defensoría antes de gastar tokens en el razonamiento.
    VALIDATING_DPE_REQUIREMENTS,

    // 0b. TERMINAL: el usuario declaró no tener prueba de consumo; debe iniciar una conversación nueva.
    BLOCKED_DPE_REQUIREMENTS,

    // 1. El Agente Razonador evalúa la lógica jurídica y genera el informe.
    EVALUATING_VIABILITY,

    // 2. Guarda el informe en la sesión y lo presenta al usuario.
    PRESENTING_REPORT,

    // 3. PAUSA: Espera que el usuario decida aceptar o enviar correcciones en lote.
    WAITING_FOR_DECISION,

    // 4. Recibe los parámetros corregidos y delega la validación de tipos a la Capa 2.
    BATCH_VALIDATING,

    // 5. El usuario aceptó. El Agente genera la ruta de acción legal (procedimiento).
    GENERATING_ACTION_ROUTE,

    WAITING_FOR_ACTION_SELECTION, // <-- NUEVO: Pausa para elegir la opción

    VALIDATING_PROCEDURAL_INPUT, // (validar datos del formulario)

    WAITING_FOR_PROCEDURAL_INPUT, // (pausa si faltan datos)

    FINALIZING_PROCEDURE, // <-- NUEVO

    // 6. La Capa 3 terminó con éxito y pasa a la Capa 4.
    DONE,

    // 7. Ocurrió un error irrecuperable.
    ERROR
}