package com.bm.copilot.audit;

import java.util.Map;
import java.util.UUID;

/**
 * What a caller tells the audit module: who did what to which entity.
 * The correlation id and timestamp are added by the audit module itself.
 *
 * @param actorId    user who performed the action (null for system actions)
 * @param action     what happened, as UPPER_SNAKE_CASE, e.g. SERVICE_REQUEST_APPROVED
 * @param entityType what it happened to, e.g. SERVICE_REQUEST
 * @param entityId   id of that entity, as text
 * @param details    extra facts, such as before and after values (stored as JSON)
 */
public record AuditEventCommand(
        UUID actorId,
        String action,
        String entityType,
        String entityId,
        Map<String, Object> details) {

    public AuditEventCommand {
        details = (details == null) ? Map.of() : Map.copyOf(details);
    }
}