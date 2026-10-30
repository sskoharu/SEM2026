package org.app.schemas.session_object.Params.type_5;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ServiciosPublicosParams {

    // ==========================================
    // 1. TRONCO COMÚN (Identificación del servicio)
    // ==========================================
    private String serviceType; // "AGUA", "LUZ", "GAS", "TELEFONIA_FIJA", "INTERNET", "TV_CABLE", "ALCANTARILLADO"
    private String supplyNumber;
    private String providerName;

    // ==========================================
    // 2. PARÁMETROS ESPECÍFICOS (Arts. 32, 33, 34)
    // ==========================================

    // --- Art. 32: Obligaciones Generales ---
    private Boolean serviceNotEfficient;
    private Boolean serviceNotQuality;
    private Boolean serviceNotTimely;
    private Boolean serviceNotContinuous;
    private Boolean serviceNotPermanent;
    private Boolean priceNotJust;
    private String serviceDeficiencyDescription;

    // --- Art. 33: Información y Seguros ---
    private Boolean lacksWrittenContract;
    private Boolean infoNotAvailableAtOffices;
    private SegurosAccesoriosParams segurosAccesorios;
    private Boolean negligenceCausedDamages;
    private String damagesDescription;

    // --- Art. 34: Reciprocidad ---
    private Boolean recargoPorMoraAplicado;
    private Boolean reintegroNoAplicado;
    private String reciprocityDetails;

    // ==========================================
    // 3. NUEVOS SUB-OBJETOS (Arts. 35, 36, 37, 38, 39, 40)
    // ==========================================

    private RegistroReclamosParams registroReclamos;
    private SeguridadInstalacionesParams seguridadInstalaciones;
    private MedicionFacturacionParams medicionFacturacion;
    private InterrupcionServicioParams interrupcionServicio;

    // ==========================================
    // 4. EVIDENCIA Y DINÁMICOS
    // ==========================================
    private List<String> attachedEvidenceUrls = new ArrayList<>();
    private Map<String, Object> dynamicParams = new HashMap<>();


    // ==========================================
    // SUBCLASES ESTÁTICAS
    // ==========================================

    @Data
    public static class SegurosAccesoriosParams {
        private Boolean hasAccessoryInsurance;
        private Boolean informedAboutInsurance;
        private Boolean isLifeInsurance;
        private BigDecimal lifeInsuranceAmount;
        private Boolean belowLegalMinimum;
    }

    // --- Art. 35: Registro de Reclamos ---
    @Data
    public static class RegistroReclamosParams {
        private Boolean hasComplaintOffice; // ¿Existe oficina de reclamos?
        private Boolean hasComplaintRegistry; // ¿Existe registro de reclamaciones?
        private Boolean complaintNotRegistered; // ¿Se negaron a registrar el reclamo?
        private Boolean complaintResolvedInTime; // ¿Subsanaron el reclamo en el plazo del Reglamento?
    }

    // --- Art. 36: Seguridad de Instalaciones ---
    @Data
    public static class SeguridadInstalacionesParams {
        private Boolean missingSafetyInfo; // ¿No informaron sobre condiciones de seguridad?
        private Boolean safetyAccidentOccurred; // ¿Hubo un accidente por falta de información?
        private String accidentDescription; // Descripción del accidente (si aplica)
    }

    // --- Arts. 37, 39 y 40: Medición y Facturación ---
    @Data
    public static class MedicionFacturacionParams {
        // Art. 37
        private Boolean measurementInstrumentNotCertified; // ¿El medidor no está legalmente reconocido?
        private Boolean deniedIndividualConsumptionControl; // ¿No garantizan el control individual?
        private Boolean invoiceDeliveredLate; // ¿La factura llegó con menos de 10 días de anticipación?

        // Art. 39: Consumo Excesivo
        private Boolean excessiveBillingClaimed; // ¿El consumidor considera que hay facturación excesiva?
        private BigDecimal averageOfLastSixMonths; // Promedio de los 6 meses anteriores
        private Boolean billingHistoryProvided; // ¿Presentó las facturas de los 6 meses?
        private Boolean providerJustifiedConsumption; // ¿La empresa acreditó el consumo en 30 días?
        private Boolean creditOwedToConsumer; // ¿Se le debe un crédito por pago en exceso?

        // Art. 40: Valores de las Planillas
        private Boolean hasUnauthorizedCharges; // ¿Hay rubros adicionales no permitidos?
        private Boolean isEstimatedBilling; // ¿Es un planillaje presuntivo o estimativo?
        private Boolean meterDamaged; // ¿El medidor está dañado o es inaccesible?
        private Boolean providerFailedToRepairMeter; // ¿La empresa no reparó ni notificó la falla?
        private Boolean technicalLossesCharged; // ¿Se trasladaron pérdidas técnicas al consumidor?
    }

    // --- Art. 38 y Art. 78: Interrupción del Servicio y Cobro Indebido ---
    @Data
    public static class InterrupcionServicioParams {
        // Hechos de la interrupción (Art. 38)
        private Boolean serviceInterrupted; // ¿Hubo interrupción del servicio?
        private Boolean interruptionCausedByProvider; // ¿Fue por causas imputables al proveedor?
        private LocalDate interruptionDate; // Fecha de la interrupción
        private Integer daysWithoutService; // Días sin servicio

        // Reintegro (Art. 38)
        private Boolean refundRequested; // ¿Se solicitó el reintegro?
        private Boolean refundProcessed; // ¿Procesaron el reintegro en 30 días?

        // Daños y perjuicios (Art. 38)
        private Boolean damagesCausedByInterruption; // ¿Hubo daños y perjuicios?
        private String interruptionDamagesDescription; // Descripción de los daños

        // NUEVOS CAMPOS: Cobro durante la suspensión (Art. 78)
        private Boolean chargedDuringInterruption; // ¿Le cobraron el servicio durante los días de interrupción?
        private Boolean refundOrDiscountApplied; // ¿Le aplicaron el descuento o reembolso del valor no devengado?
        private BigDecimal undevolvedAmount; // Monto del valor pagado y no devengado (para cálculo determinista)
    }
}