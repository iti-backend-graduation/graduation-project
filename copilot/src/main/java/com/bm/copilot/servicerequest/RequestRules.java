package com.bm.copilot.servicerequest;

import com.bm.copilot.common.ApiError.FieldViolation;
import com.bm.copilot.common.ValidationFailedException;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rules that depend on more than one field. Breaking one is a 400 VALIDATION_FAILED, like {@code @Valid}.
 * <ul>
 *   <li>Always: a CARD_SERVICE request needs the card's last 4 digits; other categories must not carry them.</li>
 *   <li>Before review (submit, or a supervisor's edit-and-approve): the customer reference is also required.</li>
 * </ul>
 */
public final class RequestRules {

    private RequestRules() {
    }

    /** Checked whenever a draft is created or edited. */
    static void checkFields(RequestCategory category, String cardLastFour) {
        throwIfAny(fieldViolations(category, cardLastFour));
    }

    /** Checked before a request goes to, or leaves, supervisor review: the request must be complete. */
    public static void checkComplete(RequestCategory category, String customerReference, String cardLastFour) {
        List<FieldViolation> violations = fieldViolations(category, cardLastFour);
        if (isBlank(customerReference)) {
            violations.add(new FieldViolation(ServiceRequest.FIELD_CUSTOMER_REFERENCE,
                    "is required before the request can be reviewed"));
        }
        throwIfAny(violations);
    }

    private static List<FieldViolation> fieldViolations(RequestCategory category, String cardLastFour) {
        List<FieldViolation> violations = new ArrayList<>();
        if (category == RequestCategory.CARD_SERVICE && isBlank(cardLastFour)) {
            violations.add(new FieldViolation(ServiceRequest.FIELD_CARD_LAST_FOUR,
                    "is required for CARD_SERVICE requests"));
        }
        if (category != RequestCategory.CARD_SERVICE && !isBlank(cardLastFour)) {
            violations.add(new FieldViolation(ServiceRequest.FIELD_CARD_LAST_FOUR,
                    "is only allowed for CARD_SERVICE requests"));
        }
        return violations;
    }

    private static void throwIfAny(List<FieldViolation> violations) {
        if (!violations.isEmpty()) {
            throw new ValidationFailedException(violations);
        }
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
