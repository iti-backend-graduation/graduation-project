package com.bm.copilot.servicerequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** The state machine, with no Spring and no database: every legal move works, every illegal one throws. */
class ServiceRequestTest {

    private static final UUID EMPLOYEE = UUID.randomUUID();
    private static final UUID SUPERVISOR = UUID.randomUUID();

    static ServiceRequest draft() {
        return new ServiceRequest(EMPLOYEE, RequestCategory.CARD_SERVICE, "CUST-1001", "4821",
                "Replace a lost debit card");
    }

    static ServiceRequest inStatus(RequestStatus status) {
        ServiceRequest request = draft();
        switch (status) {
            case DRAFT -> { }
            case PENDING_APPROVAL -> request.submit(EMPLOYEE);
            case APPROVED -> {
                request.submit(EMPLOYEE);
                request.approve(SUPERVISOR);
            }
            case REJECTED -> {
                request.submit(EMPLOYEE);
                request.reject(SUPERVISOR, "Missing customer signature");
            }
            case EXECUTED -> {
                request.submit(EMPLOYEE);
                request.approve(SUPERVISOR);
                request.markExecuted(SUPERVISOR);
            }
        }
        return request;
    }

    @Test
    void aNewRequestIsADraftAndItsCreationIsInTheHistory() {
        ServiceRequest request = draft();

        assertThat(request.getStatus()).isEqualTo(RequestStatus.DRAFT);
        assertThat(request.isOwnedBy(EMPLOYEE)).isTrue();
        assertThat(request.getSubmittedAt()).isNull();
        assertThat(request.getChanges())
                .extracting(RequestChange::getField, RequestChange::getOldValue, RequestChange::getNewValue)
                .containsExactly(tuple("status", null, "DRAFT"));
    }

    @Test
    void happyPathDraftToExecutedRecordsEveryStatusMove() {
        ServiceRequest request = draft();

        request.submit(EMPLOYEE);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING_APPROVAL);
        assertThat(request.getSubmittedAt()).isNotNull();

        request.approve(SUPERVISOR);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);

        request.markExecuted(SUPERVISOR);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.EXECUTED);

        assertThat(request.getChanges())
                .extracting(RequestChange::getChangedBy, RequestChange::getNewValue)
                .containsExactly(
                        tuple(EMPLOYEE, "DRAFT"),
                        tuple(EMPLOYEE, "PENDING_APPROVAL"),
                        tuple(SUPERVISOR, "APPROVED"),
                        tuple(SUPERVISOR, "EXECUTED"));
    }

    @Test
    void rejectKeepsTheReason() {
        ServiceRequest request = inStatus(RequestStatus.PENDING_APPROVAL);

        request.reject(SUPERVISOR, "  Customer reference does not match  ");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(request.getRejectionReason()).isEqualTo("Customer reference does not match");
    }

    @Test
    void rejectWithoutAReasonIsRefused() {
        ServiceRequest request = inStatus(RequestStatus.PENDING_APPROVAL);

        assertThatThrownBy(() -> request.reject(SUPERVISOR, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING_APPROVAL);
    }

    @Test
    void editRecordsOnlyTheFieldsThatChanged() {
        ServiceRequest request = draft();

        List<RequestChange> made = request.edit(EMPLOYEE, RequestCategory.CARD_SERVICE, "CUST-1001", "4821",
                "Replace a stolen debit card");

        assertThat(made)
                .extracting(RequestChange::getField, RequestChange::getOldValue, RequestChange::getNewValue)
                .containsExactly(tuple("description", "Replace a lost debit card", "Replace a stolen debit card"));
        assertThat(request.getDescription()).isEqualTo("Replace a stolen debit card");
        assertThat(request.getChanges()).hasSize(2);
    }

    @Test
    void editWithNoDifferenceChangesNothing() {
        ServiceRequest request = draft();

        List<RequestChange> made = request.edit(EMPLOYEE, RequestCategory.CARD_SERVICE, "CUST-1001", "4821",
                "Replace a lost debit card");

        assertThat(made).isEmpty();
        assertThat(request.getChanges()).hasSize(1);
    }

    @Test
    void editAndApproveRecordsTheSupervisorChangesThenApproves() {
        ServiceRequest request = inStatus(RequestStatus.PENDING_APPROVAL);

        List<RequestChange> made = request.editAndApprove(SUPERVISOR, null, "CUST-2002", null, null);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getCustomerReference()).isEqualTo("CUST-2002");
        assertThat(request.getCardLastFour()).isEqualTo("4821");
        assertThat(made)
                .extracting(RequestChange::getChangedBy, RequestChange::getField, RequestChange::getNewValue)
                .containsExactly(tuple(SUPERVISOR, "customerReference", "CUST-2002"));
    }

    static Stream<Arguments> illegalMoves() {
        Consumer<ServiceRequest> edit = r -> r.edit(EMPLOYEE, RequestCategory.CARD_SERVICE, null, "1111", "x");
        Consumer<ServiceRequest> submit = r -> r.submit(EMPLOYEE);
        Consumer<ServiceRequest> approve = r -> r.approve(SUPERVISOR);
        Consumer<ServiceRequest> editAndApprove = r -> r.editAndApprove(SUPERVISOR, null, "CUST-9", null, null);
        Consumer<ServiceRequest> reject = r -> r.reject(SUPERVISOR, "no");
        Consumer<ServiceRequest> execute = r -> r.markExecuted(SUPERVISOR);
        return Stream.of(
                Arguments.of("approve a DRAFT", RequestStatus.DRAFT, approve),
                Arguments.of("edit-and-approve a DRAFT", RequestStatus.DRAFT, editAndApprove),
                Arguments.of("reject a DRAFT", RequestStatus.DRAFT, reject),
                Arguments.of("execute a DRAFT", RequestStatus.DRAFT, execute),
                Arguments.of("edit a PENDING request", RequestStatus.PENDING_APPROVAL, edit),
                Arguments.of("submit twice", RequestStatus.PENDING_APPROVAL, submit),
                Arguments.of("execute a PENDING request", RequestStatus.PENDING_APPROVAL, execute),
                Arguments.of("edit an APPROVED request", RequestStatus.APPROVED, edit),
                Arguments.of("approve twice", RequestStatus.APPROVED, approve),
                Arguments.of("reject an APPROVED request", RequestStatus.APPROVED, reject),
                Arguments.of("execute a REJECTED request", RequestStatus.REJECTED, execute),
                Arguments.of("approve a REJECTED request", RequestStatus.REJECTED, approve),
                Arguments.of("submit a REJECTED request", RequestStatus.REJECTED, submit),
                Arguments.of("execute an EXECUTED request", RequestStatus.EXECUTED, execute),
                Arguments.of("edit an EXECUTED request", RequestStatus.EXECUTED, edit));
    }

    @ParameterizedTest(name = "cannot {0}")
    @MethodSource("illegalMoves")
    void illegalMovesThrowAndChangeNothing(String name, RequestStatus start, Consumer<ServiceRequest> move) {
        ServiceRequest request = inStatus(start);
        int historyBefore = request.getChanges().size();

        assertThatThrownBy(() -> move.accept(request)).isInstanceOf(InvalidTransitionException.class);

        assertThat(request.getStatus()).isEqualTo(start);
        assertThat(request.getChanges()).hasSize(historyBefore);
    }
}
