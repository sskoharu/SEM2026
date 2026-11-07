package org.app.schemas.session_object;
import lombok.Data;
import org.app.schemas.session_object.enums.*;
import org.app.services.layer_3.dto.ActionRouteResult;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@RedisHash("session") // En Redis se guardará como "session:tu-uuid"
public class SessionObject {

    // 1. Metadatos de la Sesión y Concurrencia
    @Id
    private String sessionId; // Identificador único del usuario o chat
    private ClaimSessionState currentPhase; // Estado macro (Ej: TRIAGE_PROCESSING, LEGAL_GROUNDING,) etc.)

    @TimeToLive
    private Long expiration = 86400L; // 24 horas

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // ==========================================
    // Capa 1 (Triaje y Clasificación)
    // ==========================================
    private ClaimType claimType;
    private ClaimType secondaryClaimType; // Puede haber reclamos mixtos
    private Layer1InternalState layer1State; // Estado micro (Ej: EVALUANDO_PRECISION)
    private Integer ragRetryCount = 0; // Para limitar el bucle iterativo a 3 intentos
    private String currentSearchQuery; // El query actual para el VectorStore
    private String clarificationMessage;
    // Artículos que fundamentaron la clasificación de la Capa 1
    private List<String> applicableArticles;


    // ==========================================
    // Capa 2 (Definición de Parámetros)
    // ==========================================

    // 1. Lo que la ley exige (viene de los metadatos de Qdrant en Capa 1)
    private List<String> requiredParameters = new ArrayList<>();

    // 2. El estado actual de la máquina de la Capa 2
    private Layer2InternalState layer2State;

    // 3. La cola de preguntas pendientes (se inicializa con requiredParameters)
    private List<String> pendingParametersToAsk = new ArrayList<>();

    // 4. El parámetro que se está preguntando en este momento
    private String currentParameterBeingAsked;

    // 5. La pregunta redactada por el LLM (amigable para el usuario)
    private String currentQuestionForUser;

    // 6. Almacén temporal de respuestas validadas (String -> "15/05/2026")
    private Map<String, Object> collectedParameters = new HashMap<>();

    // 7. El objeto final estructurado (Se llenará al terminar la Capa 2)
    private SpecificClaimParameters specificClaimParameters;

    private List<String> proceduralParameters;


    // ==========================================
    // Capa 3 (Razonamiento Jurídico)
    // ==========================================

    private Layer3InternalState layer3State; // Estado de la máquina de Capa 3
    private String viabilityReport; // El informe en texto que se le muestra al usuario
    private String actionRouteText; // El texto del procedimiento legal generado (alimenta a Capa 5)
    // (Acciones)
    private ActionRouteResult actionRouteOptions; // Guarda las opciones generadas
    private String selectedOptionId; // Guarda la opción que el usuario eligió
    private List<String> proceduralArticles = new ArrayList<>();
    private Map<String, Object> proceduralFormData; // Datos que envía el usuario desde el formulario
    private Map<String, Object> executionPlan = new HashMap<>();

    // ==========================================
    // Datos del Conflicto (Acumulativos)
    // ==========================================

    private ConsumerData consumer;
    private ProviderData provider;
    private ConflictDetails conflictDetails;
    private Evidence evidence; // <-- RESTAURADO (Se había borrado en tu versión)

    // Estado de Validación Legal (Capa 3 y 4)
    private FeasibilityAssessment feasibility;
    private List<String> missingQuestions;

    // Historial de conversación (Para que el LLM sepa qué se le preguntó)
    private List<ConversationTurn> conversationHistory = new ArrayList<>();


    // ==========================================
    // Subclases
    // ==========================================

    @Data
    public static class ConsumerData {
        private String fullName;
        private String idCard;
        private String contactInfo;
    }

    @Data
    public static class ProviderData {
        private String ruc;
        private String commercialName;
        private String address;
    }

    @Data
    public static class ConflictDetails {
        private String description; // El relato original
        private LocalDate eventDate;
        private BigDecimal involvedAmount;
    }

    @Data
    public static class Evidence {

        // ==========================================
        // 1. PRUEBAS DE LA TRANSACCIÓN (Art. 21)
        // ==========================================
        private boolean hasInvoice; // ¿Tiene factura? (Art. 21 inc. 1)
        private boolean hasPaymentReceipt; // ¿Tiene comprobante de transferencia, voucher o retiro?
        private boolean hasContract; // ¿Tiene el contrato de adhesión firmado? (Art. 41)

        // ==========================================
        // 2. PRUEBAS DEL PRODUCTO/SERVICIO (Arts. 11, 20, 22, 23)
        // ==========================================
        private boolean hasWarrantyDocument; // ¿Tiene el documento físico o digital de garantía?
        private boolean hasUserManual; // ¿Tiene el manual o instrucciones de uso? (Art. 16)
        private boolean hasEntryNote; // ¿Tiene la nota de ingreso del taller? (Art. 23 - Vital)
        private boolean hasPhotosOrVideos; // ¿Tiene fotos o videos del defecto o daño? (Muy útil para vicios ocultos)
        private boolean hasBillingHistory; // ¿Tiene planillas anteriores? (Art. 39 - Servicios públicos)

        // ==========================================
        // 3. PRUEBAS DE INFORMACIÓN Y PUBLICIDAD (Arts. 6, 7, 9)
        // ==========================================
        private boolean hasAdvertisingEvidence; // ¿Tiene foto/captura del anuncio publicitario?
        private boolean hasPriceTagEvidence; // ¿Tiene foto del precio exhibido en góndola o vitrina?
        private boolean hasLabelingEvidence; // ¿Tiene foto del rotulado o etiqueta? (Arts. 14, 15)

        // ==========================================
        // 4. PRUEBAS DE COMUNICACIÓN Y RECLAMOS (Art. 35)
        // ==========================================
        private boolean hasWrittenCommunication; // ¿Tiene correos, WhatsApp o cartas dirigidas al proveedor?
        private boolean hasComplaintBookRecord; // ¿Tiene la hoja del libro de reclamos o constancia? (Art. 35)
        private boolean hasWitnesses; // ¿Tiene testigos dispuestos a declarar?

        // ==========================================
        // 5. OTROS
        // ==========================================
        private String otherEvidenceDescription; // Texto libre por si el usuario menciona otra prueba no listada.
    }

    @Data
    public static class FeasibilityAssessment {
        private ClaimFeasibility feasibilityLevel;
        private String reasoning;
        private List<String> warnings;
    }

    @Data
    public static class ConversationTurn {
        private String role; // "USER" o "ASSISTANT"
        private String message;
        private LocalDateTime timestamp;
    }
}