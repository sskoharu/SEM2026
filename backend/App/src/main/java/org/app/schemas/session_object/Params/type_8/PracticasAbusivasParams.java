package org.app.schemas.session_object.Params.type_8;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class PracticasAbusivasParams {

    // ==========================================
    // 1. TRONCO COMÚN (Identificación del bien o servicio)
    // ==========================================

    /** Nombre del bien o servicio involucrado en la práctica abusiva. */
    private String productOrServiceName;

    /** Nombre del proveedor que cometió la práctica abusiva. */
    private String providerName;

    /** Fecha en que ocurrió la práctica abusiva. */
    private LocalDate eventDate;

    // ==========================================
    // 2. PARÁMETROS ESPECÍFICOS POR NUMERAL DEL ART. 55
    // ==========================================

    /** Num. 1: Condicionar la venta de un bien a la compra de otro (venta atada). */
    private VentaCondicionadaParams ventaCondicionada;

    /** Num. 2: Rehusar atender a los consumidores cuando su stock lo permita. */
    private NegativaVentaParams negativaVenta;

    /** Num. 3: Enviar productos o servicios no solicitados (se entienden como muestras gratis). */
    private EnvioNoSolicitadoParams envioNoSolicitado;

    /** Num. 4: Aprovecharse dolosamente de la edad, salud, instrucción o capacidad del consumidor. */
    private VulnerabilidadParams vulnerabilidad;

    /** Num. 5: Colocar en el mercado productos que no cumplen normas técnicas o de calidad. */
    private ProductosNoCertificadosParams productosNoCertificados;

    /** Num. 6: Aplicar fórmulas de reajuste diversas a las legales o contractuales. */
    private ReajusteIlegalParams reajusteIlegal;

    /** Num. 7: Dejar de fijar plazo para cumplir obligaciones o dejarlo a su único criterio. */
    private FaltaPlazoParams faltaPlazo;

    /** Num. 8: Redondeo de tiempos para cobrar intereses, multas o sanciones económicas. */
    private RedondeoInteresesParams redondeoIntereses;

    // ==========================================
    // 3. EVIDENCIA DOCUMENTAL (Original + Nuevos campos)
    // ==========================================

    /** ¿El usuario tiene factura, recibo o comprobante de la transacción? */
    private Boolean hasInvoiceOrReceipt;

    /** ¿Tiene publicidad, captura de pantalla o documento donde consta la práctica abusiva? */
    private Boolean hasAdvertisingOrDocumentEvidence;

    /** ¿Tiene testigos o registros de la negativa del proveedor? */
    private Boolean hasWitnessOrRefusalEvidence;

    /** URLs de los archivos subidos por el usuario. */
    private List<String> attachedEvidenceUrls = new ArrayList<>();

    // --- NUEVOS CAMPOS AGREGADOS (Basados en la LODC) ---
    private Boolean hasContract; // ¿Tiene el contrato con la cláusula abusiva?
    private Boolean hasPhotosOrVideos; // ¿Tiene fotos o videos del producto o de la publicidad?
    private Boolean hasWrittenCommunication; // ¿Tiene correos o mensajes de la negativa del proveedor?
    private Boolean hasComplaintBookRecord; // ¿Registró el reclamo en el libro de reclamos?
    private Boolean hasWitnesses; // ¿Tiene testigos presenciales?
    private String otherEvidenceDescription; // Texto libre para otras pruebas

    // ==========================================
    // 4. PARÁMETROS DINÁMICOS (Descubiertos por el RAG)
    // ==========================================
    private Map<String, Object> dynamicParams = new HashMap<>();


    // ==========================================
    // SUBCLASES ESTÁTICAS (UNA POR NUMERAL) - Sin cambios
    // ==========================================

    // --- Numeral 1: Condicionar la venta ---
    @Data
    public static class VentaCondicionadaParams {
        private Boolean tiedSaleOfGoods;
        private Boolean tiedSaleOfServices;
        private String mainProductDesired;
        private String forcedAdditionalProduct;
        private Boolean legalRequirementException;
    }

    // --- Numeral 2: Rehusar atender ---
    @Data
    public static class NegativaVentaParams {
        private Boolean refusedToServe;
        private Boolean hadStockAvailable;
        private String refusalReasonGiven;
    }

    // --- Numeral 3: Envío no solicitado ---
    @Data
    public static class EnvioNoSolicitadoParams {
        private Boolean receivedUnsolicitedProduct;
        private Boolean chargedForUnsolicitedProduct;
        private String unsolicitedProductDescription;
    }

    // --- Numeral 4: Aprovechamiento de vulnerabilidad ---
    @Data
    public static class VulnerabilidadParams {
        private Boolean exploitedAge;
        private Boolean exploitedHealth;
        private Boolean exploitedEducation;
        private Boolean exploitedCapacity;
        private String exploitationDescription;
    }

    // --- Numeral 5: Productos que no cumplen normas técnicas ---
    @Data
    public static class ProductosNoCertificadosParams {
        private Boolean missingSanitaryRegistry;
        private Boolean failsTechnicalStandards;
        private Boolean serviceFailsQualityStandards;
        private String technicalNonComplianceDescription;
    }

    // --- Numeral 6: Fórmulas de reajuste ilegales ---
    @Data
    public static class ReajusteIlegalParams {
        private Boolean appliedIllegalAdjustmentFormula;
        private Boolean adjustmentNotInContract;
        private BigDecimal originalAmount;
        private BigDecimal adjustedAmount;
    }

    // --- Numeral 7: Falta de plazo ---
    @Data
    public static class FaltaPlazoParams {
        private Boolean noDeadlineSet;
        private Boolean deadlineAtProviderDiscretion;
        private String obligationWithoutDeadline;
    }

    // --- Numeral 8: Redondeo de tiempos ---
    @Data
    public static class RedondeoInteresesParams {
        private Boolean appliedTimeRounding;
        private BigDecimal roundedAmount;
        private BigDecimal realAmount;
        private BigDecimal excessAmount;
    }
}