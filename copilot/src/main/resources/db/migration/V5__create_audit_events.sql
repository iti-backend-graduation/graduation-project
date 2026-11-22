-- Audit module: who did what to which entity. Append-only history.
-- actor_id has no foreign key on purpose: the audit trail must outlive the user and must not couple to the identity module.
CREATE TABLE audit_events (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id       uuid,
    action         varchar(100) NOT NULL,
    entity_type    varchar(60)  NOT NULL,
    entity_id      varchar(100) NOT NULL,
    details        jsonb        NOT NULL DEFAULT '{}'::jsonb,
    correlation_id varchar(64),
    created_at     timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_events_entity ON audit_events (entity_type, entity_id);
CREATE INDEX idx_audit_events_correlation_id ON audit_events (correlation_id);
CREATE INDEX idx_audit_events_actor_id ON audit_events (actor_id);

-- Append-only, enforced by the database itself: no UPDATE and no DELETE, whatever the application code does.
CREATE FUNCTION audit_events_reject_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only: % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_events_append_only
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION audit_events_reject_change();
