-- Service request module (Dev D): simulated service requests and their change history.
-- Rule: never edit this file once merged - add a new migration instead.
-- requester_id and changed_by have no foreign key to users on purpose (same choice as audit_events):
-- the workflow module does not couple to the identity module's tables.

CREATE TABLE service_requests (
    id                 uuid          PRIMARY KEY,
    category           varchar(30)   NOT NULL,
    requester_id       uuid          NOT NULL,
    customer_reference varchar(40),           -- synthetic customer id, required before submit
    card_last_four     varchar(4),            -- required for CARD_SERVICE, empty otherwise
    description        varchar(2000) NOT NULL,
    status             varchar(20)   NOT NULL,
    rejection_reason   varchar(500),
    created_at         timestamptz   NOT NULL,
    updated_at         timestamptz   NOT NULL,
    submitted_at       timestamptz,
    version            bigint        NOT NULL,  -- optimistic locking (@Version)
    CONSTRAINT ck_service_requests_category
        CHECK (category IN ('CARD_SERVICE', 'DIGITAL_BANKING', 'ACCOUNT_SERVICE', 'ADDITIONAL_DOCUMENTS')),
    CONSTRAINT ck_service_requests_status
        CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'EXECUTED')),
    CONSTRAINT ck_service_requests_card_last_four
        CHECK (card_last_four IS NULL OR card_last_four ~ '^[0-9]{4}$'),
    -- a rejected request always says why
    CONSTRAINT ck_service_requests_rejection_reason
        CHECK (status <> 'REJECTED' OR rejection_reason IS NOT NULL)
);

CREATE INDEX idx_service_requests_requester ON service_requests (requester_id, created_at DESC);
CREATE INDEX idx_service_requests_status ON service_requests (status, submitted_at);

-- One row per changed field (status moves included): who, when, field, old value, new value.
CREATE TABLE service_request_changes (
    id         uuid          PRIMARY KEY,
    request_id uuid          NOT NULL REFERENCES service_requests (id) ON DELETE CASCADE,
    changed_by uuid          NOT NULL,
    changed_at timestamptz   NOT NULL,
    field      varchar(40)   NOT NULL,
    old_value  varchar(2000),
    new_value  varchar(2000)
);

CREATE INDEX idx_service_request_changes_request ON service_request_changes (request_id, changed_at);
