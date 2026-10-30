package org.app.schemas.session_object.enums;

import lombok.Getter;

@Getter
public enum ClaimSessionState {

    // --- CAPA 1: Recepción, NLP y Triaje ---
    INIT("Capa 1: Inicio de Sesión", 1),
    TRIAGE_PROCESSING("Capa 1: Extracción de Entidades y Clasificación de Intención", 1),
    LEGAL_GROUNDING("Capa 1: Fundamentación Legal con RAG", 1),
    SUFFICIENCY_CHECK("Capa 1: Verificación de Suficiencia de Datos", 1),
    GATHERING_MISSING_DATA("Capa 1: Recopilación Interactiva de Datos Faltantes", 1),

    // --- CAPA 2: Definición de Parámetros y Validación Normativa ---
    DETERMINING_PARAMETERS("Capa 2: Determinación de Parámetros según la Ley", 2),
    DEADLINE_VALIDATION("Capa 2: Validación de Plazos Legales y Prescripción", 2),
    DYNAMIC_PROMPTING("Capa 2: Formulación de Preguntas Dinámicas", 2),
    PARAMETER_VALIDATION("Capa 2: Validación Dinámica de Entradas del Usuario", 2),

    // --- CAPA 3: Simulación y Razonamiento Jurídico ---
    VIABILITY_EVALUATION("Capa 3: Evaluación de Viabilidad y Carga de la Prueba", 3),
    EVIDENCE_GAP_ANALYSIS("Capa 3: Análisis de Vacíos en la Evidencia", 3),
    ACTION_SELECTION("Capa 3: Selección de Acción Correctiva Determinista", 3),

    // --- CAPA 4: Ejecución de Cálculos Deterministas ---
    ALGORITHMIC_PROCESSING("Capa 4: Ejecución de Cálculos Algorítmicos y Tool Calling", 4),

    // --- CAPA 5: Generación de Salidas ---
    TEMPLATE_SELECTION("Capa 5: Selección de Plantilla de Salida", 5),
    DOCUMENT_GENERATION("Capa 5: Redacción de Documento Formal de Reclamo", 5),
    ESCALATION_GUIDANCE("Capa 5: Estructuración de Pasos de Escalamiento", 5),
    COMPLETED("Capa 5: Reclamo Finalizado y Documentado", 5);

    private final String description;
    private final int layerNumber;

    ClaimSessionState(String description, int layerNumber) {
        this.description = description;
        this.layerNumber = layerNumber;
    }

}