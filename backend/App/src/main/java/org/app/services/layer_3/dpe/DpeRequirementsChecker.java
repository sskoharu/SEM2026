package org.app.services.layer_3.dpe;

import lombok.RequiredArgsConstructor;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.utils.ArticleJsonDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Compara los requisitos de la Defensoría del Pueblo con lo que ya consta en la sesión.
 * La identificación no vive en la sesión (PII); se recibe aparte al momento de generar el documento.
 */
@Component
@RequiredArgsConstructor
public class DpeRequirementsChecker {

    private final DpeGuidanceService guidance;
    private final org.app.services.layer_2.components.c0.EvidenceSyncService evidenceSync;

    public record RequirementStatus(String code, String description, boolean mandatory,
                                    boolean met, String verifiedData, String suggestion) {}

    private static final int NARRATIVE_EXCERPT = 140;

    public List<RequirementStatus> check(SessionObject session, String idCard) {
        return check(session, idCard, false);
    }

    public List<RequirementStatus> check(SessionObject session, String idCard, boolean includeOptional) {
        List<RequirementStatus> result = new ArrayList<>();
        for (ArticleJsonDto req : guidance.requirements()) {
            boolean mandatory = Boolean.TRUE.equals(req.getMetadata().get("obligatorio"));
            if (!mandatory && !includeOptional) continue;

            String code = String.valueOf(req.getMetadata().get("codigo_requisito"));
            String data = verifiedData(code, session, idCard);
            boolean met = data != null;
            String suggestion = met ? null : String.valueOf(req.getMetadata().get("sugerencia_si_falta"));
            result.add(new RequirementStatus(code, req.getDocument(), mandatory, met,
                    met ? data : "No consta en su expediente", suggestion));
        }
        return result;
    }

    private static final List<String> CONSUMPTION_PROOF_PARAMS = List.of("hasInvoice", "hasPaymentReceipt", "hasContract");

    /**
     * True solo si durante la Capa 2 el usuario respondió (o se extrajo) alguno de los parámetros de prueba de consumo
     * y ninguno es afirmativo. Si nunca se preguntó, no se considera una negativa.
     */
    public boolean hasDeclaredNoConsumptionProof(SessionObject session) {
        if (session.getCollectedParameters() == null) return false;
        boolean anyAnswered = false;
        for (String param : CONSUMPTION_PROOF_PARAMS) {
            Object value = session.getCollectedParameters().get(param);
            if (value == null) continue;
            anyAnswered = true;
            if (affirmative(value)) return false;
        }
        return anyAnswered;
    }

    private boolean affirmative(Object value) {
        return evidenceSync.toBoolean(value);
    }

    /** Devuelve el dato de la sesión con el que se verificó el requisito, o null si no se cumple. */
    private String verifiedData(String code, SessionObject session, String idCard) {
        SessionObject.Evidence ev = session.getEvidence();
        return switch (code) {
            case "IDENTIFICACION" -> idCard != null && !idCard.isBlank() ? "Documento informado: " + mask(idCard.trim()) : null;
            case "PRUEBA_CONSUMO" -> consumptionProof(ev);
            case "RELATO_HECHOS" -> {
                SessionObject.ConflictDetails d = session.getConflictDetails();
                String text = d != null ? d.getDescription() : null;
                yield text == null || text.isBlank() ? null : "Relato registrado: \"" + excerpt(text.trim()) + "\"";
            }
            case "RECLAMO_PREVIO" -> ev != null && ev.isHasWrittenCommunication() ? "Declarado por usted: comunicaciones escritas con el proveedor" : null;
            default -> null;
        };
    }

    private String consumptionProof(SessionObject.Evidence ev) {
        if (ev == null) return null;
        List<String> found = new ArrayList<>();
        if (ev.isHasInvoice()) found.add("factura");
        if (ev.isHasPaymentReceipt()) found.add("comprobante de pago");
        if (ev.isHasContract()) found.add("contrato");
        return found.isEmpty() ? null : "Declarado por usted: " + String.join(", ", found);
    }

    private String mask(String id) {
        if (id.length() <= 4) return "****";
        return id.substring(0, 2) + "*".repeat(id.length() - 4) + id.substring(id.length() - 2);
    }

    private String excerpt(String text) {
        return text.length() <= NARRATIVE_EXCERPT ? text : text.substring(0, NARRATIVE_EXCERPT).trim() + "...";
    }
}
