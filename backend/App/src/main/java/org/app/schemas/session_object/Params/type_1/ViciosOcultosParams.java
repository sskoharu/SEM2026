package org.app.schemas.session_object.Params.type_1;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
public class ViciosOcultosParams {

    // ==========================================
    // 1. TRONCO COMÚN (Base transversal - Arts. 11, 20, 26, 71)
    // ==========================================
    private String productName;
    private String productBrand;
    private String productModel;
    private Boolean isPartOfCompositeGood;

    private LocalDate purchaseDate;
    private BigDecimal pricePaid;
    private String invoiceNumber; // El número de factura es un parámetro, la factura física va en Evidence.

    private String defectDescription;
    private Boolean isUnfitForUse;
    private Boolean wouldNotHaveBoughtIt;

    private Boolean hasWarranty; // Es un hecho (¿le prometieron garantía?), no el documento.
    private Integer warrantyDurationDays;
    private Boolean warrantyProviderRefused;

    // ==========================================
    // 2. PARÁMETROS ESPECÍFICOS DEL TIPO 1
    // ==========================================
    private String chosenRemedy;
    private Boolean thirdPartyIntervention;

    private ViciosOcultosDeadlines legalDeadlines;

    @Data
    public static class ViciosOcultosDeadlines {
        private Integer daysSincePurchase;
        private Boolean isActionPrescribed;
        private LocalDate prescriptionDate;

        private Integer warrantyDurationDays;
        private Boolean isWarrantyActive;

        private LocalDate remedyClaimDate;
        private Integer daysSinceRemedyClaim;
        private Boolean isRemedyDeadlineExpired;
        private Boolean providerFailedToComplyRemedy;

        private Boolean claimsIndemnification;
        private Integer art71Causal;
        private String timeViabilityStatus;
    }
}