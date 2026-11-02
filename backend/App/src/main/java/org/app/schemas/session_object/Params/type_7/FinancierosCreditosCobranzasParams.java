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
    // 3. EVIDENCIA DOCUMENTAL (Original)
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
    // 4. NUEVOS CAMPOS DE EVIDENCIA AGREGADOS (Basados en LODC)
    // ==========================================
    private Boolean hasInvoice; // ¿Tiene la factura física o digital?
    private Boolean hasPaymentReceipt; // ¿Tiene comprobante de transferencia o voucher?
    private Boolean hasWrittenCommunication; // ¿Tiene correos o chats con el banco/proveedor?
    private Boolean hasComplaintBookRecord; // ¿Registró el reclamo en el libro de reclamos?
    private Boolean hasPhotosOrVideos; // ¿Tiene fotos de la publicidad o documentos?
    private Boolean hasWitnesses; // ¿Tiene testigos?
    private String otherEvidenceDescription; // Texto libre para otras pruebas

    // ==========================================
    // 5. PARÁMETROS DINÁMICOS (Descubiertos por el RAG)
    // ==========================================
    private Map<String, Object> dynamicParams = new HashMap<>();


    // ==========================================
    // SUBCLASES ESTÁTICAS
    // ==========================================

    // --- Art. 47: Sistemas de Crédito ---
    @Data
    public static class InformacionCreditoParams {
        private Boolean informedCashPrice;
        private Boolean informedTotalInterest;
        private Boolean informedInterestRate;
        private Boolean informedMoratoriumRate;
        private Boolean informedInstallments;
        private Boolean informedTotalAmount;
        private Boolean hasAnatocism;
        private Boolean interestCalculatedOnUnpaidCapital;
        private Boolean providesReceiptForEachPayment;
    }

    // --- Art. 48: Pago Anticipado ---
    @Data
    public static class PagoAnticipadoParams {
        private Boolean requestedEarlyPayment;
        private Boolean providerRefusedEarlyPayment;
        private BigDecimal earlyPaymentAmount;
        private Boolean interestChargedOnFullCapital;
    }

    // --- Art. 49: Cobranza de Créditos ---
    @Data
    public static class CobranzaParams {
        private Boolean abusiveCollectionMethods;
        private Boolean exposedToRidicule;
        private Boolean defamation;
        private Boolean illegalCoercion;
        private Boolean threats;
        private String abusiveCollectionDescription;
        private Boolean thirdPartiesContacted;
    }

    // --- Art. 50: Pagos con Tarjeta de Crédito ---
    @Data
    public static class PagoTarjetaCreditoParams {
        private Boolean paidWithCreditCard;
        private BigDecimal cashPrice;
        private BigDecimal creditCardPrice;
        private Boolean priceDifferenceCharged;
        private BigDecimal overpriceAmount;
        private Boolean promotionDeniedForCard;
        private String deniedPromotionDetails;
        private BigDecimal lostBenefitAmount;
        private Boolean exceptionInformedExpressly;
        private String exceptionInfoDetails;
    }

    // --- Art. 55 num. 8: Redondeo de Intereses ---
    @Data
    public static class RedondeoInteresesParams {
        private Boolean appliedTimeRounding;
        private BigDecimal roundedAmount;
        private BigDecimal realAmount;
        private BigDecimal excessAmount;
    }
}