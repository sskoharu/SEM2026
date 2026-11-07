package org.app.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Repository
@RequiredArgsConstructor
@Log4j2
public class ClaimCaseRepository {

    private static final String[] DDL = {
            """
            CREATE TABLE IF NOT EXISTS claim_case (
              id                VARCHAR(64) PRIMARY KEY,
              consumer_id_card  VARCHAR(13) NOT NULL,
              consumer_name     VARCHAR(200),
              consumer_contact  VARCHAR(200),
              provider_name     VARCHAR(200),
              provider_ruc      VARCHAR(13),
              claim_type        VARCHAR(60),
              secondary_type    VARCHAR(60),
              current_phase     VARCHAR(60),
              feasibility_level VARCHAR(20),
              event_date        DATE,
              involved_amount   NUMERIC(12,2),
              selected_option   VARCHAR(100),
              summary           TEXT,
              consumer          JSONB,
              provider          JSONB,
              conflict_details  JSONB,
              evidence          JSONB,
              collected_params  JSONB,
              specific_params   JSONB,
              layer_states      JSONB,
              legal_analysis    JSONB,
              action_route      JSONB,
              procedural_form   JSONB,
              execution_plan    JSONB,
              conversation      JSONB,
              session_snapshot  JSONB NOT NULL,
              schema_version    INT NOT NULL DEFAULT 1,
              created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
              updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_claim_case_id_card ON claim_case (consumer_id_card)",
            "CREATE INDEX IF NOT EXISTS idx_claim_case_type_phase ON claim_case (claim_type, current_phase)",
            "CREATE INDEX IF NOT EXISTS idx_claim_case_evidence ON claim_case USING GIN (evidence jsonb_path_ops)"
    };

    private static final String UPSERT = """
            INSERT INTO claim_case (
              id, consumer_id_card, consumer_name, consumer_contact, provider_name, provider_ruc,
              claim_type, secondary_type, current_phase, feasibility_level, event_date, involved_amount,
              selected_option, summary, consumer, provider, conflict_details, evidence, collected_params,
              specific_params, layer_states, legal_analysis, action_route, procedural_form, execution_plan,
              conversation, session_snapshot, schema_version)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,
              ?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,
              ?::jsonb,?::jsonb,?)
            ON CONFLICT (id) DO UPDATE SET
              consumer_id_card = EXCLUDED.consumer_id_card,
              consumer_name = EXCLUDED.consumer_name,
              consumer_contact = EXCLUDED.consumer_contact,
              provider_name = EXCLUDED.provider_name,
              provider_ruc = EXCLUDED.provider_ruc,
              claim_type = EXCLUDED.claim_type,
              secondary_type = EXCLUDED.secondary_type,
              current_phase = EXCLUDED.current_phase,
              feasibility_level = EXCLUDED.feasibility_level,
              event_date = EXCLUDED.event_date,
              involved_amount = EXCLUDED.involved_amount,
              selected_option = EXCLUDED.selected_option,
              summary = EXCLUDED.summary,
              consumer = EXCLUDED.consumer,
              provider = EXCLUDED.provider,
              conflict_details = EXCLUDED.conflict_details,
              evidence = EXCLUDED.evidence,
              collected_params = EXCLUDED.collected_params,
              specific_params = EXCLUDED.specific_params,
              layer_states = EXCLUDED.layer_states,
              legal_analysis = EXCLUDED.legal_analysis,
              action_route = EXCLUDED.action_route,
              procedural_form = EXCLUDED.procedural_form,
              execution_plan = EXCLUDED.execution_plan,
              conversation = EXCLUDED.conversation,
              session_snapshot = EXCLUDED.session_snapshot,
              schema_version = EXCLUDED.schema_version,
              updated_at = now()
            """;

    private static final String LIST_COLUMNS = """
            id, consumer_id_card, consumer_name, provider_name, claim_type, current_phase,
            feasibility_level, event_date, involved_amount, summary, created_at, updated_at
            """;

    private final JdbcTemplate jdbc;
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);

    public void upsert(ClaimCaseRow r) {
        ensureSchema();
        jdbc.update(UPSERT,
                r.id(), r.idCard(), r.consumerName(), r.consumerContact(), r.providerName(), r.providerRuc(),
                r.claimType(), r.secondaryType(), r.currentPhase(), r.feasibilityLevel(),
                r.eventDate() == null ? null : Date.valueOf(r.eventDate()), r.involvedAmount(),
                r.selectedOption(), r.summary(), r.consumer(), r.provider(), r.conflictDetails(), r.evidence(),
                r.collectedParams(), r.specificParams(), r.layerStates(), r.legalAnalysis(), r.actionRoute(),
                r.proceduralForm(), r.executionPlan(), r.conversation(), r.snapshot(), r.schemaVersion());
    }

    /** Detalle completo (sin el snapshot duplicado) como JSON listo para devolver. */
    public Optional<String> findDetailJson(String id) {
        ensureSchema();
        return jdbc.query("SELECT (to_jsonb(c) - 'session_snapshot')::text FROM claim_case c WHERE id = ?",
                (rs, i) -> rs.getString(1), id).stream().findFirst();
    }

    public Optional<String> findSnapshotJson(String id) {
        ensureSchema();
        return jdbc.query("SELECT session_snapshot::text FROM claim_case WHERE id = ?",
                (rs, i) -> rs.getString(1), id).stream().findFirst();
    }

    public Optional<String> findIdCard(String id) {
        ensureSchema();
        return jdbc.query("SELECT consumer_id_card FROM claim_case WHERE id = ?",
                (rs, i) -> rs.getString(1), id).stream().findFirst();
    }

    public String listByIdCardJson(String idCard) {
        ensureSchema();
        return jdbc.queryForObject(
                "SELECT COALESCE(jsonb_agg(to_jsonb(t)), '[]'::jsonb)::text FROM (SELECT " + LIST_COLUMNS
                        + " FROM claim_case WHERE consumer_id_card = ? ORDER BY updated_at DESC LIMIT 100) t",
                String.class, idCard);
    }

    private void ensureSchema() {
        if (schemaReady.get()) {
            return;
        }
        synchronized (this) {
            if (schemaReady.get()) {
                return;
            }
            for (String statement : DDL) {
                jdbc.execute(statement);
            }
            schemaReady.set(true);
            log.info("Tabla claim_case verificada");
        }
    }

    public record ClaimCaseRow(
            String id, String idCard, String consumerName, String consumerContact,
            String providerName, String providerRuc, String claimType, String secondaryType,
            String currentPhase, String feasibilityLevel, java.time.LocalDate eventDate,
            BigDecimal involvedAmount, String selectedOption, String summary,
            String consumer, String provider, String conflictDetails, String evidence,
            String collectedParams, String specificParams, String layerStates, String legalAnalysis,
            String actionRoute, String proceduralForm, String executionPlan, String conversation,
            String snapshot, int schemaVersion) {
    }
}
