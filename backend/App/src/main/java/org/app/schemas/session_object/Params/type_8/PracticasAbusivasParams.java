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
    // 3. EVIDENCIA DOCUMENTAL (Para Capa 3 - Razonamiento)
    // ==========================================

    /** ¿El usuario tiene factura, recibo o comprobante de la transacción? */
    private Boolean hasInvoiceOrReceipt;

    /** ¿Tiene publicidad, captura de pantalla o documento donde consta la práctica abusiva? */
    private Boolean hasAdvertisingOrDocumentEvidence;

    /** ¿Tiene testigos o registros de la negativa del proveedor? */
    private Boolean hasWitnessOrRefusalEvidence;

    /** URLs de los archivos subidos por el usuario. */
    private List<String> attachedEvidenceUrls = new ArrayList<>();

    // ==========================================
    // 4. PARÁMETROS DINÁMICOS (Descubiertos por el RAG)
    // ==========================================
    private Map<String, Object> dynamicParams = new HashMap<>();


    // ==========================================
    // SUBCLASES ESTÁTICAS (UNA POR NUMERAL)
    // ==========================================

    // --- Numeral 1: Condicionar la venta ---
    @Data
    public static class VentaCondicionadaParams {
        /** ¿El proveedor condicionó la venta de un bien a la compra de otro bien? */
        private Boolean tiedSaleOfGoods;
        /** ¿Condicionó la venta a la contratación de un servicio adicional? */
        private Boolean tiedSaleOfServices;
        /** Descripción del bien o servicio principal que el usuario quería comprar. */
        private String mainProductDesired;
        /** Descripción del bien o servicio adicional que le obligaron a comprar. */
        private String forcedAdditionalProduct;
        /** ¿Existía una disposición legal que obligara al consumidor a cumplir con ese requisito? (Excepción legal) */
        private Boolean legalRequirementException;
    }

    // --- Numeral 2: Rehusar atender ---
    @Data
    public static class NegativaVentaParams {
        /** ¿El proveedor se negó a atender al consumidor? */
        private Boolean refusedToServe;
        /** ¿El proveedor tenía stock disponible del producto? */
        private Boolean hadStockAvailable;
        /** Descripción de la excusa que dio el proveedor para no atender. */
        private String refusalReasonGiven;
    }

    // --- Numeral 3: Envío no solicitado ---
    @Data
    public static class EnvioNoSolicitadoParams {
        /** ¿El proveedor envió un producto o servicio que el consumidor no solicitó? */
        private Boolean receivedUnsolicitedProduct;
        /** ¿El proveedor le está cobrando por ese producto o servicio no solicitado? */
        private Boolean chargedForUnsolicitedProduct;
        /** Descripción del producto o servicio enviado sin solicitud. */
        private String unsolicitedProductDescription;
    }

    // --- Numeral 4: Aprovechamiento de vulnerabilidad ---
    @Data
    public static class VulnerabilidadParams {
        /** ¿El proveedor se aprovechó de la edad avanzada o menor edad del consumidor? */
        private Boolean exploitedAge;
        /** ¿Se aprovechó de una condición de salud o discapacidad? */
        private Boolean exploitedHealth;
        /** ¿Se aprovechó de la falta de instrucción o educación del consumidor? */
        private Boolean exploitedEducation;
        /** ¿Se aprovechó de una capacidad intelectual disminuida? */
        private Boolean exploitedCapacity;
        /** Descripción de cómo el proveedor se aprovechó de la situación. */
        private String exploitationDescription;
    }

    // --- Numeral 5: Productos que no cumplen normas técnicas ---
    @Data
    public static class ProductosNoCertificadosParams {
        /** ¿El producto carece del registro sanitario o certificado obligatorio? */
        private Boolean missingSanitaryRegistry;
        /** ¿El producto no cumple con las normas técnicas del INEN u órgano competente? */
        private Boolean failsTechnicalStandards;
        /** ¿El servicio ofertado no cumple con las normas de calidad exigidas? */
        private Boolean serviceFailsQualityStandards;
        /** Descripción del incumplimiento técnico detectado. */
        private String technicalNonComplianceDescription;
    }

    // --- Numeral 6: Fórmulas de reajuste ilegales ---
    @Data
    public static class ReajusteIlegalParams {
        /** ¿El proveedor aplicó una fórmula de reajuste de precio distinta a la legal? */
        private Boolean appliedIllegalAdjustmentFormula;
        /** ¿El reajuste aplicado no estaba previsto en el contrato? */
        private Boolean adjustmentNotInContract;
        /** Monto original antes del reajuste. */
        private BigDecimal originalAmount;
        /** Monto después del reajuste ilegal. */
        private BigDecimal adjustedAmount;
    }

    // --- Numeral 7: Falta de plazo ---
    @Data
    public static class FaltaPlazoParams {
        /** ¿El proveedor no fijó un plazo para cumplir con su obligación? */
        private Boolean noDeadlineSet;
        /** ¿El proveedor dejó el plazo a su único criterio? */
        private Boolean deadlineAtProviderDiscretion;
        /** Descripción de la obligación que quedó sin plazo definido. */
        private String obligationWithoutDeadline;
    }

    // --- Numeral 8: Redondeo de tiempos ---
    @Data
    public static class RedondeoInteresesParams {
        /** ¿El proveedor redondeó los tiempos para cobrar intereses, multas o sanciones? */
        private Boolean appliedTimeRounding;
        /** Monto cobrado aplicando el redondeo. */
        private BigDecimal roundedAmount;
        /** Monto real que debió cobrarse (Calculado por Capa 4 - Tool Calling). */
        private BigDecimal realAmount;
        /** Diferencia cobrada en exceso (roundedAmount - realAmount). */
        private BigDecimal excessAmount;
    }
}