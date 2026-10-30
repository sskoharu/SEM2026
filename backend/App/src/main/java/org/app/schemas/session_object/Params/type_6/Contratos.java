package org.app.schemas.session_object.Params.type_6;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Contratos {

    // ==========================================
    // 1. TRONCO COMÚN (Identificación del contrato)
    // ==========================================
    private String serviceType; // "TELEFONIA_CELULAR", "MEDICINA_PREPAGADA", "TV_SATELITAL", "TV_CABLE", "OTROS"
    private String providerName; // Nombre de la empresa
    private LocalDate contractStartDate; // Fecha de inicio del contrato
    private BigDecimal monthlyFee; // Valor de la mensualidad o tarifa

    // ==========================================
    // 2. PARÁMETROS ESPECÍFICOS DEL ART. 44
    // ==========================================

    // --- Sobre la terminación anticipada ---
    private Boolean wantsToTerminateEarly; // ¿El usuario quiere terminar el contrato antes de tiempo?
    private Boolean notifiedInWriting; // ¿Notificó por escrito su decisión de terminar?
    private LocalDate notificationDate; // Fecha en que notificó
    private Boolean respected15DaysNotice; // ¿La notificación se hizo con al menos 15 días de anticipación?
    private Boolean providerRefusedTermination; // ¿El proveedor se negó a aceptar la terminación?

    // --- Sobre la cláusula de multa o recargo (Prohibición legal) ---
    private Boolean hasEarlyTerminationPenalty; // ¿El contrato incluye una cláusula de multa o recargo por terminación anticipada?
    private BigDecimal penaltyAmount; // Monto de la multa o recargo que le quieren cobrar (si aplica)
    private Boolean penaltyIsIllegal; // ¿La multa es ilegal según el Art. 44? (El sistema lo marcará como true si se cumple lo anterior)

    // --- Sobre las obligaciones del consumidor (Saldos pendientes) ---
    private Boolean hasPendingServiceDebt; // ¿Tiene deudas por servicios ya prestados?
    private BigDecimal pendingServiceDebtAmount; // Monto de la deuda por servicios prestados
    private Boolean hasPendingEquipmentDebt; // ¿Tiene deudas por bienes (ej. celular) adquiridos?
    private BigDecimal pendingEquipmentDebtAmount; // Monto de la deuda por el bien

    // ==========================================
    // 3. PRETENSIONES (Lo que el usuario pide)
    // ==========================================
    private Boolean requestsTermination; // ¿El usuario pide que se declare terminado el contrato?
    private Boolean requestsNullityOfPenalty; // ¿El usuario pide que se declare nula la cláusula de multa?
    private Boolean requestsRefund; // ¿El usuario pide la devolución de valores pagados indebidamente?

    // ==========================================
    // 4. EVIDENCIA Y DINÁMICOS
    // ==========================================
    private List<String> attachedEvidenceUrls = new ArrayList<>();
    private Map<String, Object> dynamicParams = new HashMap<>();
}