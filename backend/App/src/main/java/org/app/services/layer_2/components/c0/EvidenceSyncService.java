package org.app.services.layer_2.components.c0;

import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Mantiene sincronizados los parámetros probatorios (collectedParameters) con el objeto Evidence.
 * La respuesta explícita del usuario prevalece sobre lo inferido del relato.
 */
@Component
@Log4j2
public class EvidenceSyncService {

    private static final Set<String> EVIDENCE_PARAMS = Set.of(
            "hasInvoice", "hasPaymentReceipt", "hasContract", "hasWarrantyDocument", "hasUserManual",
            "hasPhotosOrVideos", "hasPriceTagEvidence", "hasLabelingEvidence", "hasWrittenCommunication",
            "hasComplaintBookRecord", "hasWitnesses", "hasEntryNote", "hasAdvertisingEvidence",
            "hasBillingHistory", "otherEvidenceDescription");

    private static final Pattern NEGATIVE = Pattern.compile("^(no|ninguno|ninguna|nunca|false)\\b|\\bno (tengo|cuento|poseo|conservo|dispongo|hay)\\b|\\bperdi\\b|\\bsin\\b");
    private static final Pattern POSITIVE = Pattern.compile("^(si|sip|claro|true|yes|ok)\\b|\\b(tengo|cuento|poseo|conservo|dispongo|guardo)\\b");

    public boolean isEvidenceParameter(String name) {
        return name != null && EVIDENCE_PARAMS.contains(name);
    }

    public void applyAll(SessionObject session, Map<String, Object> evidenceParams) {
        evidenceParams.forEach((key, value) -> apply(session, key, value));
    }

    public void apply(SessionObject session, String key, Object value) {
        if (!isEvidenceParameter(key) || value == null) return;
        if (session.getEvidence() == null) session.setEvidence(new SessionObject.Evidence());
        SessionObject.Evidence ev = session.getEvidence();

        if ("otherEvidenceDescription".equals(key)) {
            ev.setOtherEvidenceDescription(value.toString());
            return;
        }

        boolean b = toBoolean(value);
        switch (key) {
            case "hasInvoice" -> ev.setHasInvoice(b);
            case "hasPaymentReceipt" -> ev.setHasPaymentReceipt(b);
            case "hasContract" -> ev.setHasContract(b);
            case "hasWarrantyDocument" -> ev.setHasWarrantyDocument(b);
            case "hasUserManual" -> ev.setHasUserManual(b);
            case "hasPhotosOrVideos" -> ev.setHasPhotosOrVideos(b);
            case "hasPriceTagEvidence" -> ev.setHasPriceTagEvidence(b);
            case "hasLabelingEvidence" -> ev.setHasLabelingEvidence(b);
            case "hasWrittenCommunication" -> ev.setHasWrittenCommunication(b);
            case "hasComplaintBookRecord" -> ev.setHasComplaintBookRecord(b);
            case "hasWitnesses" -> ev.setHasWitnesses(b);
            case "hasEntryNote" -> ev.setHasEntryNote(b);
            case "hasAdvertisingEvidence" -> ev.setHasAdvertisingEvidence(b);
            case "hasBillingHistory" -> ev.setHasBillingHistory(b);
            default -> log.warn("Parámetro de evidencia desconocido: {}", key);
        }
        log.info("Evidence sincronizada: {} = {} (valor original: '{}')", key, b, value);
    }

    /** Interpreta respuestas como "Si, lo conservo", "No tengo", true/false. Lo ambiguo cuenta como false. */
    public boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value == null) return false;
        String text = Normalizer.normalize(value.toString().toLowerCase().trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        if (NEGATIVE.matcher(text).find()) return false;
        return POSITIVE.matcher(text).find();
    }
}
