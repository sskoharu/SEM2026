package org.app.schemas.session_object.Params.type_2;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
public class ServicioTecnicoParams {

    // ==========================================
    // 1. TRONCO COMÚN (Base transversal - Arts. 22, 23, 24, 25, 75)
    // ==========================================

    private String itemDescription; // Ej: "Televisor LG 55 pulgadas", "Lavadora Samsung"
    private String brand;           // Marca del bien
    private String model;           // Modelo específico

    private LocalDate entryDate;    // Fecha en que dejó el bien en el taller (Art. 23)
    private LocalDate deliveryDate; // Fecha en que se lo devolvieron (Art. 22)
    private BigDecimal repairCost;  // Cuánto pagó por la mano de obra/servicio (Art. 75)
    private String originalProblem; // Qué fallaba antes de llevarlo al taller
    private String currentDefectDescription; // Qué está mal ahora (ej: "no enciende", "pantalla rota")

    // ==========================================
    // 2. PARÁMETROS ESPECÍFICOS DEL TIPO 2
    // ==========================================

    // --- Para Reparación Defectuosa y Plazos (Art. 22) ---
    private LocalDate repairDeliveryDate;     // Fecha en que se recibió el bien reparado
    private Integer daysSinceRepairDelivery;  // Calculado automáticamente por la Capa 4 (Límite: 90 días)
    private Boolean isRepairWarrantyExpired;  // true si daysSinceRepairDelivery > 90
    private Boolean providerRefusedToFix;     // ¿Se niega a volver a revisarlo?

    // --- Para Custodia y Deterioro (Art. 23 y 73) ---
    private String custodyDamageDescription;  // Ej: "Rayones en la carcasa", "faltan tornillos"
    private BigDecimal itemDeclaredValue;     // El valor del bien según la nota de ingreso
    private Boolean hasEntryNote;             // ¿Tiene la nota de ingreso firmada donde consta el estado?
    private Boolean providerRefusedIndemnification; // ¿El taller se niega a pagar por el daño causado? (Activa Art. 73)

    // --- Para Repuestos en la Reparación (Art. 24) ---
    private Boolean usedInadequateParts;      // ¿Usaron repuestos usados/genéricos en la reparación sin avisar?

    // --- Para Suministro de Repuestos y Servicio Técnico Postventa (Art. 25) ---
    private Boolean requiresSparePartOrService;  // ¿El usuario necesita un repuesto o servicio técnico?
    private Boolean providerRefusedSupply;       // ¿El fabricante/distribuidor se niega a suministrarlo?
    private Boolean productWithinUsefulLife;     // ¿El bien está dentro de su vida útil? (Según manual o INEN)
    private Boolean lackOfPartsMakesGoodUseless; // ¿La falta de repuesto inutiliza el bien por completo?

    // --- Para Servicios Defectuosos (Art. 75) ---
    // Evaluamos los 4 supuestos del Art. 75
    private Boolean serviceManifestlyDefective; // ¿Fue manifiestamente defectuoso?
    private Boolean serviceIneffective;         // ¿Fue ineficaz (no solucionó nada)?
    private Boolean serviceCausedDamage;        // ¿Causó daño (ej. quemó otra pieza)?
    private Boolean serviceNotAsAgreed;         // ¿No se ajustó a lo expresamente acordado (ej. cobro extra)?

    // ==========================================
    // 3. PRETENSIONES (Lo que el usuario pide)
    // ==========================================

    // El derecho del Art. 75: Restitución del valor cancelado (repairCost)
    private Boolean claimsRefundOfServiceCost;  // ¿El usuario pide que le devuelvan lo que pagó por la reparación?

    // Indemnización por daños y perjuicios (Art. 75)
    private Boolean claimsIndemnification;      // ¿El usuario solicita que se le indemnice por daños y perjuicios?
    private String damagesDescription;          // ¿En qué consistieron esos daños extra? (Ej: "tuve que pagar lavandería", "el técnico rayó la carcasa")
    private BigDecimal indemnificationEstimatedAmount; // Monto estimado por el usuario (opcional, el juez lo determinará)

}