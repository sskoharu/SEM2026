package org.app.schemas.session_object.Params.type_7;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class FinancierosCreditosCobranzasParams {

    // ==========================================
    // 1. TRONCO COMÚN (Identificación del crédito)
    // ==========================================

    /**
     * Tipo de producto financiero.
     * Ayuda al Agente Redactor a contextualizar el reclamo.
     * Valores: "TARJETA_CREDITO", "PRESTAMO_PERSONAL", "CREDITO_HIPOTECARIO",
     * "CREDITO_VEHICULAR", "VENTA_A_CREDITO", "OTROS".
     */
    private String financialProductType;

    /** Nombre del banco, cooperativa, financiera o casa comercial. */
    private String providerName;

    /** Monto total del crédito o el precio de contado del bien/servicio. */
    private BigDecimal totalCreditAmount;

    /** Fecha en que se firmó el contrato o se realizó la compra a crédito. */
    private LocalDate creditStartDate;

    /** Número total de cuotas o meses pactados en el contrato. */
    private Integer totalInstallments;

    // ==========================================
    // 2. PARÁMETROS ESPECÍFICOS POR ARTÍCULO
    // ==========================================

    /** Art. 47: Deber de información, prohibición de anatocismo y recibos. */
    private InformacionCreditoParams informacionCredito;

    /** Art. 48: Derecho a pagar anticipadamente sin penalización de intereses futuros. */
    private PagoAnticipadoParams pagoAnticipado;

    /** Art. 49: Prohibición de cobranza abusiva (ridículo, difamación, amenazas). */
    private CobranzaParams cobranza;

    /** Art. 50: Igualdad de precio entre contado y tarjeta de crédito, y promociones. */
    private PagoTarjetaCreditoParams pagoTarjetaCredito;

    /** Art. 55 num. 8: Prohibición de redondear tiempos para cobrar intereses o multas. */
    private RedondeoInteresesParams redondeoIntereses;

    // ==========================================
    // 3. EVIDENCIA DOCUMENTAL (Para Capa 3 - Razonamiento)
    // ==========================================

    /** ¿El usuario tiene el contrato de crédito firmado o las condiciones del mismo? */
    private Boolean hasContractEvidence;

    /** ¿Tiene comprobantes de pago, estados de cuenta o recibos de las cuotas? */
    private Boolean hasPaymentProof;

    /** Art. 49: ¿Tiene capturas de llamadas, mensajes, correos o publicaciones difamatorias? */
    private Boolean hasCollectionEvidence;

    /** Art. 50: ¿Tiene foto del anuncio, góndola o publicidad donde se ofrecía el descuento al contado? */
    private Boolean hasPromotionEvidence;

    /** URLs de los archivos subidos por el usuario (facturas, capturas, contratos). */
    private List<String> attachedEvidenceUrls = new ArrayList<>();

    // ==========================================
    // 4. PARÁMETROS DINÁMICOS (Descubiertos por el RAG)
    // ==========================================
    private Map<String, Object> dynamicParams = new HashMap<>();


    // ==========================================
    // SUBCLASES ESTÁTICAS
    // ==========================================

    // --- Art. 47: Sistemas de Crédito ---
    @Data
    public static class InformacionCreditoParams {
        // Deber de información previa (El proveedor DEBE informar antes de firmar)

        /** ¿Le informaron el precio al contado del bien o servicio? (Art. 47 num. 1) */
        private Boolean informedCashPrice;

        /** ¿Le informaron el monto total de intereses que pagaría? (Art. 47 num. 2) */
        private Boolean informedTotalInterest;

        /** ¿Le informaron la tasa de interés aplicable? (Art. 47 num. 2) */
        private Boolean informedInterestRate;

        /** ¿Le informaron la tasa de interés moratoria por atraso? (Art. 47 num. 2) */
        private Boolean informedMoratoriumRate;

        /** ¿Le informaron el número, monto y periodicidad (mensual, quincenal) de las cuotas? (Art. 47 num. 3) */
        private Boolean informedInstallments;

        /** ¿Le informaron la suma total exacta que pagaría al final del crédito? (Art. 47 num. 4) */
        private Boolean informedTotalAmount;

        // Prohibición de anatocismo (Cobro de intereses sobre intereses)

        /** ¿El proveedor cobró intereses sobre los intereses ya generados? (Práctica prohibida) */
        private Boolean hasAnatocism;

        /** ¿El sistema recalculó los intereses sobre el saldo de capital impago en cada cuota? (Deber legal) */
        private Boolean interestCalculatedOnUnpaidCapital;

        // Recibos

        /** ¿El proveedor entrega un recibo por cada pago parcial realizado? (Deber legal) */
        private Boolean providesReceiptForEachPayment;
    }

    // --- Art. 48: Pago Anticipado ---
    @Data
    public static class PagoAnticipadoParams {

        /** ¿El usuario solicitó pagar la totalidad o realizar prepagos parciales mayores a una cuota? */
        private Boolean requestedEarlyPayment;

        /** ¿El proveedor se negó a aceptar el pago anticipado? (Violación directa al Art. 48) */
        private Boolean providerRefusedEarlyPayment;

        /** Monto que el usuario quiso prepagar o pagar en su totalidad. */
        private BigDecimal earlyPaymentAmount;

        /** ¿El proveedor cobró intereses sobre el capital total original en lugar del saldo pendiente? (Violación legal) */
        private Boolean interestChargedOnFullCapital;
    }

    // --- Art. 49: Cobranza de Créditos ---
    @Data
    public static class CobranzaParams {

        /** ¿La agencia o el banco utilizó métodos de cobranza abusivos? */
        private Boolean abusiveCollectionMethods;

        /** ¿Expusieron al consumidor al ridículo público? (Ej. carteles en la casa, publicaciones en redes) */
        private Boolean exposedToRidicule;

        /** ¿Hubo difamación hacia la persona del consumidor? */
        private Boolean defamation;

        /** ¿Hubo coacción ilícita (ej. retención de bienes, bloqueo de cuentas sin orden judicial)? */
        private Boolean illegalCoercion;

        /** ¿Hubo amenazas directas o indirectas por parte del cobrador? */
        private Boolean threats;

        /** Descripción detallada de los hechos abusivos (Ej. "Llamaron a mi jefe y le dijeron que soy un deudor"). */
        private String abusiveCollectionDescription;

        /** ¿El proveedor o la agencia contactó a familiares, vecinos, compañeros de trabajo u otros terceros ajenos a la deuda? */
        private Boolean thirdPartiesContacted;
    }

    // --- Art. 50: Pagos con Tarjeta de Crédito ---
    @Data
    public static class PagoTarjetaCreditoParams {

        /** ¿El usuario realizó el pago utilizando una tarjeta de crédito? */
        private Boolean paidWithCreditCard;

        /** El precio que el proveedor ofrecía si se pagaba en efectivo o al contado. */
        private BigDecimal cashPrice;

        /** El precio que se le cobró finalmente al usuario por usar la tarjeta de crédito. */
        private BigDecimal creditCardPrice;

        // Regla 1: El precio debe ser idéntico al de contado

        /** ¿Le cobraron un sobreprecio (recargo) por pagar con tarjeta? */
        private Boolean priceDifferenceCharged;

        /** Monto exacto del sobreprecio cobrado (diferencia entre creditCardPrice y cashPrice). */
        private BigDecimal overpriceAmount;

        // Regla 2: Las promociones al contado aplican para tarjeta

        /** ¿Le negaron una promoción, rebaja o descuento que sí aplicaba al contado? */
        private Boolean promotionDeniedForCard;

        /** Detalle de la promoción negada (Ej. "20% de descuento", "2x1"). */
        private String deniedPromotionDetails;

        /** Monto del beneficio económico que perdió el usuario por no aplicarle la promoción. */
        private BigDecimal lostBenefitAmount;

        // Excepción: El proveedor debe informar expresamente si hay restricción

        /** ¿El proveedor informó de manera oportuna, adecuada y expresa que la promoción o el precio no aplicaban para tarjeta? */
        private Boolean exceptionInformedExpressly;

        /** ¿Dónde y cómo se informó la excepción? (Ej. "En letra pequeña del contrato", "No se informó en ningún lado"). */
        private String exceptionInfoDetails;
    }

    // --- Art. 55 num. 8: Redondeo de Intereses ---
    @Data
    public static class RedondeoInteresesParams {

        /** ¿El proveedor redondeó los tiempos (días, horas) para cobrar intereses, multas o sanciones? */
        private Boolean appliedTimeRounding;

        /** Monto que el proveedor cobró aplicando el redondeo (Ej. cobró 30 días cuando fueron 5). */
        private BigDecimal roundedAmount;

        /** Monto real que debió cobrarse por el tiempo efectivo (Calculado por Capa 4 - Tool Calling). */
        private BigDecimal realAmount;

        /** Diferencia cobrada en exceso (roundedAmount - realAmount). */
        private BigDecimal excessAmount;
    }
}