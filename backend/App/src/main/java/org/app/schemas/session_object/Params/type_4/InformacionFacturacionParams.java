package org.app.schemas.session_object.Params.type_4;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class InformacionFacturacionParams {

    // ==========================================
    // 1. TRONCO COMÚN (Base transversal - Arts. 6, 7, 8, 9)
    // ==========================================
    private String productOrServiceName;
    private String informationChannel; // "PUBLICIDAD", "ETIQUETA_ROTULADO", "PRECIO_GÓNDOLA", "FACTURACION"

    private String advertisedInfo;
    private String actualInfoReceived;
    private String omissionDescription;

    private Boolean isAbusiveAdvertising;
    private String advertisingInfractionType; // "ORIGEN_TECNOLOGIA", "BENEFICIOS_PRECIOS", "CARACTERISTICAS_BASICAS", "RECONOCIMIENTOS"
    private Boolean providerFailedToJustifyAd;
    private Boolean hasAdvertisingEvidence;

    private Boolean invoiceProvided;
    private BigDecimal displayedPriceWithTaxes;
    private BigDecimal chargedPrice;
    private Boolean missingWeightOrMeasure;
    private Boolean missingUnitPrice;

    // ==========================================
    // 2. PARÁMETROS ESPECÍFICOS DEL TIPO 4
    // ==========================================

    // --- Para Productos Usados o Deficientes (Art. 12) ---
    private Boolean isUsedOrDeficientProduct;
    private Boolean missingUsedDeficientWarning;

    // --- Para Productos Transgénicos (Art. 13) ---
    private Boolean isGeneticallyModified;
    private Boolean missingGmoWarning;

    // --- Para Rotulado de Alimentos (Art. 14) ---
    private RotuladoAlimentosParams rotuladoAlimentos;

    // --- Para Rotulado de Medicamentos (Art. 15) ---
    private RotuladoMedicamentosParams rotuladoMedicamentos;

    // Para Bienes de Naturaleza Durable (Art. 16)
    private BienDurableParams bienDurable;

    // Para la Obligación General de Información (Art. 17)
    private ObligacionInformacionParams obligacionInformacion;

    // Para Indicación del Precio (Art. 19)
    private PrecioExhibidoParams precioExhibido;

    // Para Facturación (Art. 21 inciso 1)
    private FacturaParams facturacion;

    // --- NUEVO: Para Servicios Profesionales (Art. 27) ---
    private ServicioProfesionalParams servicioProfesional;

    // --- NUEVO: Pretensión para Sanción Publicitaria (Art. 72) ---
    private Boolean requestsAdvertisingRectification; // ¿El usuario pide la rectificación pública del anuncio?

    // --- NUEVO: Para Servicio de Telefonía (Art. 91) ---
    private TelefoniaBillingParams telefonia;

    // --- NUEVO: Para Intereses por Mora (Art. 93) ---
    private InteresesMoraParams interesesMora;

    // --- Para Servicios Profesionales (Art. 27) ---
    private Boolean feesNotDisclosedInitially;


    // ==========================================
    // 3. EVIDENCIA DOCUMENTAL (NUEVOS CAMPOS AGREGADOS)
    // ==========================================

    // --- Transacción ---
    private Boolean hasInvoice; // ¿Tiene la factura física o digital?
    private Boolean hasPaymentReceipt; // ¿Tiene comprobante de transferencia o voucher?
    private Boolean hasContract; // ¿Tiene el contrato firmado?

    // --- Producto/Servicio ---
    private Boolean hasWarrantyDocument; // ¿Tiene el documento de garantía?
    private Boolean hasUserManual; // ¿Tiene el manual o instrucciones?
    private Boolean hasPhotosOrVideos; // ¿Tiene fotos del defecto o del producto?

    // --- Información/Publicidad ---
    private Boolean hasPriceTagEvidence; // ¿Tiene foto del precio en góndola?
    private Boolean hasLabelingEvidence; // ¿Tiene foto del rotulado o etiqueta?

    // --- Comunicación ---
    private Boolean hasWrittenCommunication; // ¿Tiene correos o chats con el proveedor?
    private Boolean hasComplaintBookRecord; // ¿Tiene la copia del libro de reclamos?
    private Boolean hasWitnesses; // ¿Tiene testigos?
    private String otherEvidenceDescription; // Texto libre para otras pruebas


    // ==========================================
    // SUBCLASES ESTÁTICAS (ENCAPSULAMIENTO) - Sin cambios
    // ==========================================

    // Art. 14
    @Data
    public static class RotuladoAlimentosParams {
        private Boolean isFoodProduct;
        private Boolean missingExpirationDate; // literal h)
        private Boolean missingSanitaryRegistry; // literal f)
        private Boolean missingIngredientsList; // literal i)
        private List<String> otherMissingItems = new ArrayList<>();
    }

    // Art. 15
    @Data
    public static class RotuladoMedicamentosParams {
        private Boolean isMedicineOrNaturalProduct;
        private Boolean missingExpirationDate; // literal g)
        private Boolean missingSanitaryRegistry; // literal f)
        private Boolean missingContraindications; // literal k) - CRÍTICO para salud
        private List<String> otherMissingItems = new ArrayList<>();
    }

    // Art. 16
    @Data
    public static class BienDurableParams {
        private Boolean isDurableGood;
        private Boolean missingSafetyInstructions;
        private Boolean missingHandlingInstructions;
        private Boolean missingWarnings;
    }

    // Art. 17
    @Data
    public static class ObligacionInformacionParams {
        private String deficiencyType;
        private String contextDescription;
    }

    // NUEVA SUBCLASE: Art. 19
    @Data
    public static class PrecioExhibidoParams {
        private BigDecimal displayedPriceWithTaxes;
        private BigDecimal chargedPrice;
        private BigDecimal surchargePercentage;
        private String surchargeDescription;
        private Boolean priceNotDisplayed;
        private Boolean priceNotClearlyVisible;
        private Boolean notInLegalCurrency;
        private Boolean isPharmacy;
        private Boolean pharmacyMissingOfficialList;
    }

    // NUEVA SUBCLASE: Art. 21 inc. 1
    @Data
    public static class FacturaParams {
        private Boolean invoiceProvided;
        private Boolean providerRefusedInvoice;
        private Boolean invoiceLacksTaxRequirements;
    }

    // NUEVA SUBCLASE: Art. 27
    @Data
    public static class ServicioProfesionalParams {
        private Boolean isProfessionalService;
        private Boolean feesDisclosedInitially;
        private BigDecimal agreedFee;
        private String feeParameterDescription;
        private Boolean ethicalOrQualityBreach;
        private String ethicalBreachDescription;
    }

    // NUEVA SUBCLASE: Art. 91
    @Data
    public static class TelefoniaBillingParams {
        private Boolean isTelecomService;
        private Boolean appliedRounding;
        private Integer realTimeUsedSeconds;
        private Integer billedTimeSeconds;
        private BigDecimal chargedAmountForCall;
        private BigDecimal realAmountForCall;
    }

    // NUEVA SUBCLASE: Art. 93
    @Data
    public static class InteresesMoraParams {
        private Boolean lateInterestCharged;
        private BigDecimal principalAmount;
        private Integer actualDaysLate;
        private BigDecimal appliedInterestRate;
        private BigDecimal legalInterestRate;
        private BigDecimal interestAmountCharged;
        private BigDecimal excessAmount;
    }
}