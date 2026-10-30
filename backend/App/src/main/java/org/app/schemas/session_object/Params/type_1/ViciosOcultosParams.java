package org.app.schemas.session_object.Params.type_1;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ViciosOcultosParams {

    // ==========================================
    // 1. TRONCO COMÚN (Base transversal - Arts. 11, 20, 26, 71)
    // ==========================================

    // Identificación del bien (Art. 20 y 26)
    private String productName;
    private String productBrand;
    private String productModel;
    private Boolean isPartOfCompositeGood; // Art. 26: ¿Es una pieza de un todo?

    // Datos de la transacción (Art. 20, 71)
    private LocalDate purchaseDate; // Vital para plazos (Art. 31)
    private BigDecimal pricePaid;
    private String invoiceNumber; // Para vincular la prueba

    // Naturaleza del defecto (Art. 20)
    private String defectDescription; // ¿Qué falla?
    private Boolean isUnfitForUse; // ¿Lo hace inadecuado para su uso habitual?
    private Boolean wouldNotHaveBoughtIt; // ¿Lo habría comprado sabiendo del defecto?

    // Garantía (Art. 11 y 71)
    private Boolean hasWarranty;
    private Integer warrantyDurationDays; // Si la tiene, ¿por cuánto tiempo?
    private Boolean warrantyProviderRefused; // ¿El proveedor se negó a cumplirla? (Art. 71.1)

    // ==========================================
    // 2. PARÁMETROS ESPECÍFICOS DEL TIPO 1
    // ==========================================
    private String chosenRemedy; // "reposición", "rebaja", "rescisión" (Art. 20)
    private Boolean thirdPartyIntervention; // ¿Un tercero lo manipuló? (Excluye responsabilidad)

    // NUEVO: Objeto de plazos legales (en lugar de daysElapsed)
    private ViciosOcultosDeadlines legalDeadlines;


    @Data
    public static class ViciosOcultosDeadlines {
        // --- Art. 31: Prescripción de la acción civil (LODC) ---
        private Integer daysSincePurchase;       // Días desde purchaseDate hasta hoy
        private Boolean isActionPrescribed;      // true si daysSincePurchase > 365
        private LocalDate prescriptionDate;      // Fecha exacta en que prescribe

        // --- Art. 11: Garantía (LODC) ---
        private Integer warrantyDurationDays;    // Plazo de garantía ofrecido (ej. 90, 180, 365)
        private Boolean isWarrantyActive;        // true si daysSincePurchase <= warrantyDurationDays

        // --- Art. 71: Plazo de 30 días para cumplir el remedio (reparar, reponer o devolver) ---
        private LocalDate remedyClaimDate;              // Fecha en que el consumidor exigió formalmente el remedio
        private Integer daysSinceRemedyClaim;           // Días desde remedyClaimDate
        private Boolean isRemedyDeadlineExpired;        // true si daysSinceRemedyClaim > 30
        private Boolean providerFailedToComplyRemedy;   // ¿El proveedor no ejecutó el remedio en plazo?

        // --- Art. 71: Pretensiones adicionales ---
        private Boolean claimsIndemnification;          // ¿El consumidor solicita indemnización por daños y perjuicios?

        // --- Art. 71: Causal invocada (para fundamentar el reclamo) ---
        // 1 = Garantía incumplida (num. 1)
        // 2 = Producto no apto (num. 2)
        // 3 = Contenido neto inferior (num. 3)
        private Integer art71Causal;                    // 1, 2 o 3

        // --- Diagnóstico de viabilidad temporal ---
        private String timeViabilityStatus;
        // "VIABLE_AMBOS" (Garantía y prescripción vigentes)
        // "VIABLE_SOLO_VICIO" (Garantía expiró, pero < 365 días)
        // "ALERTA_PRESCRIPCION" (Garantía vigente, pero > 365 días)
        // "PRESCRITO" (Todo vencido)
    }
}