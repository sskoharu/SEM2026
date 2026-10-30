package org.app.schemas.session_object.Params.type_4;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    // SUBCLASES ESTÁTICAS (ENCAPSULAMIENTO)
    // ==========================================

    // Art. 14
    @Data
    public static class RotuladoAlimentosParams {
        // ¿Es un producto alimenticio para consumo humano?
        private Boolean isFoodProduct;

        // Literales críticos del Art. 14
        private Boolean missingExpirationDate; // literal h)
        private Boolean missingSanitaryRegistry; // literal f)
        private Boolean missingIngredientsList; // literal i)

        // Lista para los demás literales faltantes (a, b, c, d, e, g, j, k, l)
        private List<String> otherMissingItems = new ArrayList<>();
    }

    // Art. 15
    @Data
    public static class RotuladoMedicamentosParams {
        // ¿Es un medicamento o producto natural procesado?
        private Boolean isMedicineOrNaturalProduct;

        // Literales críticos del Art. 15
        private Boolean missingExpirationDate; // literal g)
        private Boolean missingSanitaryRegistry; // literal f)
        private Boolean missingContraindications; // literal k) - CRÍTICO para salud

        // Lista para los demás literales faltantes (a, b, c, d, e, h, i, j, l)
        private List<String> otherMissingItems = new ArrayList<>();
    }

    // Art. 16
    @Data
    public static class BienDurableParams {
        private Boolean isDurableGood; // ¿Es un electrodoméstico, vehículo, electrónico, etc.?
        private Boolean missingSafetyInstructions; // ¿Falta información sobre seguridad de uso?
        private Boolean missingHandlingInstructions; // ¿Faltan instrucciones de manejo adecuado?
        private Boolean missingWarnings; // ¿Faltan advertencias de precaución?
    }

    // Art. 17
    @Data
    public static class ObligacionInformacionParams {
        // Valores esperados: "FALSA", "INSUFICIENTE", "POCO_CLARA", "INCOMPLETA", "TARDIA", "NINGUNA"
        private String deficiencyType;
        private String contextDescription; // Breve explicación de por qué se considera deficiente
    }

    // NUEVA SUBCLASE: Art. 19
    @Data
    public static class PrecioExhibidoParams {
        private BigDecimal displayedPriceWithTaxes; // El valor final que vio el usuario
        private BigDecimal chargedPrice; // El valor realmente cobrado
        private Boolean priceNotDisplayed; // ¿Omitieron el precio por completo?
        private Boolean priceNotClearlyVisible; // ¿El precio no era visible antes de pagar?
        private Boolean notInLegalCurrency; // ¿El precio estaba en otra moneda o sin impuestos?
        private Boolean isPharmacy; // ¿Es farmacia, botica?
        private Boolean pharmacyMissingOfficialList; // ¿No exhibía la lista oficial de medicamentos básicos?
    }

    // NUEVA SUBCLASE: Art. 21 inc. 1
    @Data
    public static class FacturaParams {
        private Boolean invoiceProvided; // ¿Le entregaron factura?
        private Boolean providerRefusedInvoice; // ¿Se negaron explícitamente a dar factura?
        private Boolean invoiceLacksTaxRequirements; // ¿La factura entregada no tiene RUC, fecha o datos legales?
    }

    // NUEVA SUBCLASE: Art. 27
    @Data
    public static class ServicioProfesionalParams {
        private Boolean isProfessionalService; // ¿Es médico, abogado, contador, etc.?
        private Boolean feesDisclosedInitially; // ¿Informaron honorarios al inicio de la gestión?
        private BigDecimal agreedFee; // Monto acordado
        private String feeParameterDescription; // Parámetro (ej. "por hora", "por consulta")
        private Boolean ethicalOrQualityBreach; // ¿Hubo falta de ética o calidad en la atención?
        private String ethicalBreachDescription; // Descripción de la falta ética o de calidad
    }

    // NUEVA SUBCLASE: Art. 91
    @Data
    public static class TelefoniaBillingParams {
        private Boolean isTelecomService; // ¿Es telefonía fija o móvil?
        private Boolean appliedRounding; // ¿El proveedor aplicó redondeo de minutos o segundos?
        private Integer realTimeUsedSeconds; // Tiempo real de uso en segundos
        private Integer billedTimeSeconds; // Tiempo que el proveedor facturó en segundos
        private BigDecimal chargedAmountForCall; // Monto cobrado por esa llamada/consumo
        private BigDecimal realAmountForCall; // Monto que debió cobrarse (calculado por Capa 4)
    }

    // NUEVA SUBCLASE: Art. 93
    @Data
    public static class InteresesMoraParams {
        private Boolean lateInterestCharged; // ¿Le cobraron intereses por mora?
        private BigDecimal principalAmount; // Capital o monto base de la deuda
        private Integer actualDaysLate; // Días reales de retraso
        private BigDecimal appliedInterestRate; // Tasa que aplicó el proveedor
        private BigDecimal legalInterestRate; // Tasa legal vigente (para comparar)
        private BigDecimal interestAmountCharged; // Monto cobrado por concepto de intereses
        private BigDecimal excessAmount; // Diferencia cobrada en exceso (calculado por Capa 4)
    }

}