-- Approval module (Dev D): supervisor decisions and simulated execution outcomes.
-- Rule: never edit this file once merged - add a new migration instead.

-- One decision per request: REJECTED is final and APPROVED cannot be approved again,
-- so the unique constraint also stops two supervisors deciding the same request at once.
CREATE TABLE approval_decisions (
    id            uuid         PRIMARY KEY,
    request_id    uuid         NOT NULL REFERENCES service_requests (id),
    supervisor_id uuid         NOT NULL,
    decision      varchar(30)  NOT NULL,
    reason        varchar(500),
    changes       jsonb        NOT NULL DEFAULT '{}'::jsonb,  -- edit-and-approve: field -> {from, to}
    decided_at    timestamptz  NOT NULL,
    CONSTRAINT uq_approval_decisions_request UNIQUE (request_id),
    CONSTRAINT ck_approval_decisions_decision CHECK (decision IN ('APPROVED', 'REJECTED', 'EDITED_AND_APPROVED')),
    CONSTRAINT ck_approval_decisions_reason CHECK (decision <> 'REJECTED' OR reason IS NOT NULL)
);

CREATE INDEX idx_approval_decisions_supervisor ON approval_decisions (supervisor_id, decided_at DESC);

-- Simulated execution: at most one outcome per request, enforced by the database
-- (the third layer of duplicate-execution prevention, after the status check and @Version).
CREATE TABLE execution_outcomes (
    id          uuid         PRIMARY KEY,
    request_id  uuid         NOT NULL REFERENCES service_requests (id),
    executed_by uuid         NOT NULL,
    message     varchar(500) NOT NULL,
    executed_at timestamptz  NOT NULL,
    CONSTRAINT uq_execution_outcomes_request UNIQUE (request_id)
);
