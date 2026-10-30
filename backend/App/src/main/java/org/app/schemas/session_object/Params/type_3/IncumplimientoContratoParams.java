package org.app.schemas.session_object.Params.type_3;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
public class IncumplimientoContratoParams {

    // ==========================================
    // 1. TRONCO COMÚN (Base transversal - Arts. 18, 30, 46, 148)
    // ==========================================

    private String agreementType; // "Compraventa", "Prestación de servicios", "Promoción", "Oferta pública"
    private String promisedGoodOrService; // ¿Qué se suponía que iban a entregar? (Art. 18, 46)
    private BigDecimal amountPaid; // ¿Cuánto dinero entregó? (Art. 30 para resolución)

    private LocalDate agreementDate; // Fecha en que se pactó
    private LocalDate promisedDeliveryDate; // Fecha en que se prometió entregar/prestar (Art. 18)
    private Boolean providerSetNoDeadline; // ¿El proveedor no fijó plazo o lo dejó a su criterio? (Art. 55 num. 7)

    private String breachDescription; // ¿Qué pasó? (Ej: "No entregaron el producto", "El servicio fue suspendido sin razón")

    // ==========================================
    // 2. PARÁMETROS ESPECÍFICOS DEL TIPO 3
    // ==========================================

    // --- Para Incumplimiento de Documentación (Art. 21) ---
    private Boolean providerRefusedInvoice; // ¿Se negó a entregar la factura?
    private Boolean providerRefusedAdditionalReceipt; // ¿Se negó a firmar el comprobante adicional?
    private Boolean hasAdditionalReceipt; // Si se lo dieron, ¿el comprobante tiene lugar, fecha y consecuencias?
    private LocalDate newPromisedDate; // Fecha que pusieron en ese comprobante
    private Boolean providerFailedNewDeadline; // ¿El proveedor volvió a incumplir la nueva fecha?
    private Boolean isServiceContract; // ¿El contrato es de prestación de servicios?
    private Boolean providerRefusedServiceDetails; // ¿Se negó a detallar componentes y mano de obra? (Art. 21 inc. 3)

    // --- Para Ofertas y Promociones (Arts. 46 y 148) ---
    private Boolean isOfferOrPromotion; // ¿El incumplimiento se basa en una oferta o promoción?
    private String offerDetails; // ¿Qué prometía la oferta?
    private Boolean isPublicOffer; // ¿Fue en catálogo, circular, proforma o aviso público? (Art. 148)

    // --- Para Suspensión de Servicios (Art. 77) ---

    // Hecho Sancionable
    private Boolean isServiceSuspension; // ¿El proveedor suspendió, paralizó o no prestó el servicio?
    private Boolean wasPaidUpToDate; // ¿El consumidor estaba al día en los pagos?
    private String suspensionReasonGiven; // ¿Qué excusa dio el proveedor? (Para evaluar si es "sin justificación")

    // NUEVO: Condición de pago específico (Conexión, instalación, mantenimiento, etc.)
    // Valores esperados: "CONEXION", "INSTALACION", "INCORPORACION", "MANTENIMIENTO", "TARIFA_CONSUMO", "NINGUNO"
    private String paidServiceRightType;
    private Boolean paidRequiredServiceFees; // ¿Realizó el pago inicial o de mantenimiento requerido por el contrato?

    // NUEVO: Naturaleza del proveedor (Para el segundo inciso del Art. 77)
    private Boolean isStateOrConcessionaire; // ¿Es una empresa pública, municipal o concesionaria del Estado?

    // NUEVO: Descripción de los daños civiles por la suspensión
    private String civilDamagesDescription; // Ej: "Pérdida de $200 en alimentos por corte de energía", "Pérdida de clientes por caída de internet"
    private BigDecimal civilDamagesEstimatedAmount; // Estimación económica del daño

    // --- Para Espectáculos Públicos (Art. 76) ---
    private Boolean isEventOrSpectacle; // ¿Es un concierto, partido, evento artístico?
    private Boolean eventCapacityExceeded; // ¿Vendieron más boletos que la capacidad del recinto?
    private Boolean wasDeniedEntry; // ¿No le permitieron el ingreso por sobreventa?

    // ==========================================
    // 3. EVIDENCIA DOCUMENTAL (NUEVO)
    // ==========================================
    // Indican al Agente Evaluador (Capa 3) qué tan sólido es el caso.

    private Boolean hasContractEvidence; // ¿Tiene el contrato firmado o las bases de la promoción?
    private Boolean hasOfferEvidence; // ¿Tiene foto del catálogo, captura de la web o proforma? (Art. 148)
    private Boolean hasPaymentProof; // ¿Tiene transferencia, depósito o recibo de pago?

    // ==========================================
    // 4. PRETENSIONES (Lo que el usuario pide)
    // ==========================================

    private String requestedRemedy; // "RESOLUCION" (devolución) o "CUMPLIMIENTO" (exigir entrega)
    private Boolean claimsIndemnification;
    private String damagesDescription;
    private BigDecimal indemnificationEstimatedAmount;

    // ==========================================
    // 5. PARÁMETROS DINÁMICOS
    // ==========================================
    private Map<String, Object> dynamicParams = new HashMap<>();
}
