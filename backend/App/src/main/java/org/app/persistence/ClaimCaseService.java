package org.app.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.app.persistence.ClaimCaseRepository.ClaimCaseRow;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.EvidenceItem;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class ClaimCaseService {

    static final int SCHEMA_VERSION = 1;
    private static final int SUMMARY_MAX = 300;

    private final ClaimCaseRepository repository;
    private final SessionRepository sessionRepository;
    private final ObjectMapper mapper;

    public ClaimCaseService(ClaimCaseRepository repository, SessionRepository sessionRepository,
                            ObjectMapper baseMapper) {
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.mapper = baseMapper.copy()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public record SaveData(String idCard, String fullName, String contactInfo) {
    }

    public void save(String sessionId, SaveData data) {
        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Sesión no encontrada o expirada"));

        SessionObject.ConsumerData consumer = session.getConsumer() != null
                ? session.getConsumer() : new SessionObject.ConsumerData();
        consumer.setIdCard(data.idCard());
        if (data.fullName() != null) consumer.setFullName(data.fullName());
        if (data.contactInfo() != null) consumer.setContactInfo(data.contactInfo());
        session.setConsumer(consumer);

        repository.upsert(toRow(session));
    }

    public record Registration(String id, String status, LocalDateTime registeredAt, String claimType,
                               java.util.List<String> applicableArticles, String corpusVersion) {
    }

    /** Registro formal: solo cuando la Capa 3 terminó (hay plan de ejecución); reutiliza la persistencia de save. */
    public Registration register(String sessionId, SaveData data) {
        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Sesión no encontrada o expirada"));
        if (session.getLayer3State() != Layer3InternalState.DONE) {
            throw new IllegalStateException("El reclamo aún no está listo para registrarse: la Capa 3 no ha finalizado.");
        }
        save(sessionId, data);
        String corpusVersion = session.getRagEvidence() == null ? null : session.getRagEvidence().stream()
                .map(EvidenceItem::corpusVersion).filter(Objects::nonNull).findFirst().orElse(null);
        return new Registration(sessionId, "REGISTERED", LocalDateTime.now(), name(session.getClaimType()),
                session.getApplicableArticles(), corpusVersion);
    }

    public String detail(String id, String idCard) {
        assertOwner(id, idCard);
        return repository.findDetailJson(id).orElseThrow(() -> new NoSuchElementException("Reclamo no encontrado"));
    }

    public String list(String idCard) {
        return repository.listByIdCardJson(idCard);
    }

    public SessionObject resume(String id, String idCard) {
        assertOwner(id, idCard);
        String json = repository.findSnapshotJson(id)
                .orElseThrow(() -> new NoSuchElementException("Reclamo no encontrado"));
        try {
            SessionObject session = mapper.readValue(json, SessionObject.class);
            session.setSessionId(id);
            session.setExpiration(86400L);
            session.setUpdatedAt(LocalDateTime.now());
            return sessionRepository.save(session);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo restaurar el reclamo guardado", e);
        }
    }

    private void assertOwner(String id, String idCard) {
        String owner = repository.findIdCard(id)
                .orElseThrow(() -> new NoSuchElementException("Reclamo no encontrado"));
        if (!Objects.equals(owner, idCard)) {
            throw new NoSuchElementException("Reclamo no encontrado");
        }
    }

    private ClaimCaseRow toRow(SessionObject s) {
        SessionObject.ConsumerData c = s.getConsumer();
        SessionObject.ProviderData p = s.getProvider();
        SessionObject.ConflictDetails d = s.getConflictDetails();

        Map<String, Object> layerStates = new LinkedHashMap<>();
        layerStates.put("layer1State", s.getLayer1State());
        layerStates.put("layer2State", s.getLayer2State());
        layerStates.put("layer3State", s.getLayer3State());
        layerStates.put("ragRetryCount", s.getRagRetryCount());
        layerStates.put("clarificationMessage", s.getClarificationMessage());

        Map<String, Object> legal = new LinkedHashMap<>();
        legal.put("applicableArticles", s.getApplicableArticles());
        legal.put("requiredParameters", s.getRequiredParameters());
        legal.put("viabilityReport", s.getViabilityReport());
        legal.put("feasibility", s.getFeasibility());

        Map<String, Object> route = new LinkedHashMap<>();
        route.put("actionRouteText", s.getActionRouteText());
        route.put("actionRouteOptions", s.getActionRouteOptions());
        route.put("selectedOptionId", s.getSelectedOptionId());
        route.put("proceduralArticles", s.getProceduralArticles());
        route.put("proceduralParameters", s.getProceduralParameters());

        String description = d != null ? d.getDescription() : null;
        String summary = description == null ? null
                : description.length() > SUMMARY_MAX ? description.substring(0, SUMMARY_MAX) : description;

        return new ClaimCaseRow(
                s.getSessionId(),
                c.getIdCard(), c.getFullName(), c.getContactInfo(),
                p != null ? p.getCommercialName() : null, p != null ? p.getRuc() : null,
                name(s.getClaimType()), name(s.getSecondaryClaimType()), name(s.getCurrentPhase()),
                s.getFeasibility() != null ? name(s.getFeasibility().getFeasibilityLevel()) : null,
                d != null ? d.getEventDate() : null, d != null ? d.getInvolvedAmount() : null,
                s.getSelectedOptionId(), summary,
                json(c), json(p), json(d), json(s.getEvidence()),
                json(s.getCollectedParameters()), json(s.getSpecificClaimParameters()),
                json(layerStates), json(legal), json(route),
                json(s.getProceduralFormData()), json(s.getExecutionPlan()),
                json(s.getConversationHistory()),
                json(s), SCHEMA_VERSION);
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el reclamo", e);
        }
    }
}
