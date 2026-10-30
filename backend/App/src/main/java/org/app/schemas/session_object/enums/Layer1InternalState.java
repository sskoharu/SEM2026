package org.app.schemas.session_object.enums;

public enum Layer1InternalState {

    // ==========================================
    // FLUJO PRINCIPAL (Happy Path)
    // ==========================================

    // 1. El Orquestador inicia aquí. Evalúa la coherencia jurídica y traduce el relato a jerga legal.
    TRADUCIENDO_QUERY,

    // 2. Busca los documentos en Qdrant usando el query traducido.
    BUSCANDO_FUENTES,

    // 3. Evalúa si el score de similitud de los documentos es >= 90%.
    EVALUANDO_PRECISION,

    // 4. Si el score es alto, clasifica usando los metadatos de Qdrant (sin LLM).
    CLASIFICACION_POR_METADATA,

    // 5. La Capa 1 terminó con éxito y pasa a la Capa 2.
    DONE,

    // ==========================================
    // FLUJOS ALTERNATIVOS (Pausas y Errores)
    // ==========================================

    // Se activa si el Portero detecta falta de información, o si el RAG falla tras 3 intentos.
    SOLICITANDO_ACLARACION,

    // Ocurrió un error irrecuperable en el sistema.
    ERROR
}