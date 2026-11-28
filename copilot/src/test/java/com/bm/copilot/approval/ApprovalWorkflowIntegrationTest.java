package com.bm.copilot.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bm.copilot.TestcontainersConfiguration;
import com.bm.copilot.WorkflowTestTokens;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Supervisor review and simulated execution, end to end with real tokens and a real database.
 * Covers the brief's rules: an employee cannot approve through the API, nothing executes without approval,
 * nothing executes twice, and every decision and execution is audited.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApprovalWorkflowIntegrationTest {

    static final String CARD_REQUEST = """
            {"category":"CARD_SERVICE","customerReference":"CUST-1001","cardLastFour":"4821",
             "description":"Customer lost their debit card; block it and issue a replacement."}
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    JdbcTemplate jdbc;

    private String employee;
    private String supervisor;
    private String otherSupervisor;

    @BeforeEach
    void tokens() throws Exception {
        employee = WorkflowTestTokens.employee1(mockMvc);
        supervisor = WorkflowTestTokens.supervisor1(mockMvc);
        otherSupervisor = WorkflowTestTokens.tokenFor(jwtEncoder, UUID.randomUUID(), "supervisor2", "SUPERVISOR");
    }

    // ---- who may decide -------------------------------------------------------------------------------------

    @Test
    void anEmployeeCannotApproveRejectOrExecuteThroughTheApi() throws Exception {
        String id = pendingRequest();

        action(employee, id, "approve").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        action(employee, id, "reject", "{}").andExpect(status().isForbidden());
        action(employee, id, "edit-and-approve", "{\"customerReference\":\"CUST-9\"}")
                .andExpect(status().isForbidden());
        action(employee, id, "execute").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/approvals/queue").header("Authorization", employee))
                .andExpect(status().isForbidden());

        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
        assertThat(countRows("approval_decisions", id)).isZero();
    }

    @Test
    void aSupervisorCannotDecideOnTheirOwnRequest() throws Exception {
        String id = createdId(supervisor, CARD_REQUEST);
        submit(supervisor, id).andExpect(status().isOk());

        action(supervisor, id, "approve").andExpect(status().isForbidden());
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");

        action(otherSupervisor, id, "approve").andExpect(status().isOk());
    }

    // ---- decisions ------------------------------------------------------------------------------------------

    @Test
    void theQueueShowsPendingRequestsButNotDrafts() throws Exception {
        String pending = pendingRequest();
        String draft = createdId(employee, CARD_REQUEST);

        String body = mockMvc.perform(get("/api/approvals/queue").header("Authorization", supervisor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> ids = JsonPath.read(body, "$[*].id");
        assertThat(ids).contains(pending).doesNotContain(draft);
    }

    @Test
    void supervisorApprovesAndTheDecisionAndAuditRowAreRecorded() throws Exception {
        String id = pendingRequest();

        action(supervisor, id, "approve")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.request.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty());

        assertThat(countRows("approval_decisions", id)).isEqualTo(1);
        assertThat(auditActions(id)).contains("SERVICE_REQUEST_APPROVED");
    }

    @Test
    void approvingADraftOrApprovingTwiceIs409() throws Exception {
        String draft = createdId(employee, CARD_REQUEST);
        action(supervisor, draft, "approve").andExpect(status().isConflict());

        String id = pendingRequest();
        action(supervisor, id, "approve").andExpect(status().isOk());
        action(supervisor, id, "approve").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(countRows("approval_decisions", id)).isEqualTo(1);
    }

    @Test
    void rejectNeedsAReasonAndTheEmployeeSeesIt() throws Exception {
        String id = pendingRequest();

        action(supervisor, id, "reject", "{\"reason\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("reason"));

        action(supervisor, id, "reject", "{\"reason\":\"Customer reference does not match the ID shown\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Customer reference does not match the ID shown"));

        mockMvc.perform(get("/api/requests/" + id).header("Authorization", employee))
                .andExpect(jsonPath("$.request.status").value("REJECTED"))
                .andExpect(jsonPath("$.request.rejectionReason")
                        .value("Customer reference does not match the ID shown"));
        assertThat(auditActions(id)).contains("SERVICE_REQUEST_REJECTED");
    }

    @Test
    void editAndApproveRecordsTheSupervisorsChanges() throws Exception {
        String id = pendingRequest();

        action(supervisor, id, "edit-and-approve", "{\"customerReference\":\"CUST-2002\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("EDITED_AND_APPROVED"))
                .andExpect(jsonPath("$.request.status").value("APPROVED"))
                .andExpect(jsonPath("$.request.customerReference").value("CUST-2002"))
                .andExpect(jsonPath("$.changes.customerReference.from").value("CUST-1001"))
                .andExpect(jsonPath("$.changes.customerReference.to").value("CUST-2002"));

        mockMvc.perform(get("/api/requests/" + id).header("Authorization", employee))
                .andExpect(jsonPath("$.history[*].field",
                        Matchers.contains("status", "status", "customerReference", "status")));
        assertThat(auditActions(id)).contains("SERVICE_REQUEST_EDITED_AND_APPROVED");
    }

    @Test
    void editAndApproveWithNoRealChangeOrAnInvalidResultIs400() throws Exception {
        String id = pendingRequest();

        action(supervisor, id, "edit-and-approve", "{\"customerReference\":\"CUST-1001\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("changes"));
        // a non-card category would still carry the card digits
        action(supervisor, id, "edit-and-approve", "{\"category\":\"ACCOUNT_SERVICE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("cardLastFour"));

        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void anApprovedRequestCannotBeEditedByTheEmployee() throws Exception {
        String id = pendingRequest();
        action(supervisor, id, "approve").andExpect(status().isOk());

        mockMvc.perform(put("/api/requests/" + id).header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content(CARD_REQUEST.replace("4821", "0000")))
                .andExpect(status().isConflict());
        assertThat(statusOf(id)).isEqualTo("APPROVED");
    }

    // ---- execution ------------------------------------------------------------------------------------------

    @Test
    void nothingExecutesWithoutApproval() throws Exception {
        String draft = createdId(employee, CARD_REQUEST);
        action(supervisor, draft, "execute").andExpect(status().isConflict());

        String pending = pendingRequest();
        action(supervisor, pending, "execute").andExpect(status().isConflict());

        String rejected = pendingRequest();
        action(supervisor, rejected, "reject", "{\"reason\":\"Not eligible\"}").andExpect(status().isOk());
        action(supervisor, rejected, "execute").andExpect(status().isConflict());

        assertThat(countRows("execution_outcomes", draft)).isZero();
        assertThat(countRows("execution_outcomes", pending)).isZero();
        assertThat(countRows("execution_outcomes", rejected)).isZero();
    }

    @Test
    void executingTwiceFailsAndOnlyOneOutcomeExists() throws Exception {
        String id = approvedRequest();

        action(supervisor, id, "execute")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.message", Matchers.containsString("card ending 4821")));
        action(supervisor, id, "execute")
                .andExpect(status().isConflict());

        assertThat(countRows("execution_outcomes", id)).isEqualTo(1);
    }

    @Test
    void twoSimultaneousExecutionsCannotBothSucceed() throws Exception {
        String id = approvedRequest();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> executeCall = () -> {
            start.await();
            return action(supervisor, id, "execute").andReturn().getResponse().getStatus();
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> calls = new ArrayList<>();
            calls.add(pool.submit(executeCall));
            calls.add(pool.submit(executeCall));
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> call : calls) {
                statuses.add(call.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(countRows("execution_outcomes", id)).isEqualTo(1);
    }

    // ---- the whole flow -------------------------------------------------------------------------------------

    @Test
    void fullFlowCreateSubmitApproveExecuteWithStatusAndAuditAtEachStep() throws Exception {
        String id = createdId(employee, CARD_REQUEST);
        assertThat(statusOf(id)).isEqualTo("DRAFT");
        assertThat(auditActions(id)).containsExactly("SERVICE_REQUEST_CREATED");

        submit(employee, id).andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
        assertThat(auditActions(id)).containsExactly("SERVICE_REQUEST_CREATED", "SERVICE_REQUEST_SUBMITTED");

        action(supervisor, id, "approve").andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("APPROVED");
        assertThat(auditActions(id)).containsExactly("SERVICE_REQUEST_CREATED", "SERVICE_REQUEST_SUBMITTED",
                "SERVICE_REQUEST_APPROVED");

        action(supervisor, id, "execute").andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("EXECUTED");
        assertThat(auditActions(id)).containsExactly("SERVICE_REQUEST_CREATED", "SERVICE_REQUEST_SUBMITTED",
                "SERVICE_REQUEST_APPROVED", "SERVICE_REQUEST_EXECUTED");

        mockMvc.perform(get("/api/requests/" + id).header("Authorization", employee))
                .andExpect(jsonPath("$.history[*].newValue",
                        Matchers.contains("DRAFT", "PENDING_APPROVAL", "APPROVED", "EXECUTED")));
    }

    // ---- helpers --------------------------------------------------------------------------------------------

    private String createdId(String token, String json) throws Exception {
        String body = mockMvc.perform(post("/api/requests").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions submit(String token, String id) throws Exception {
        return mockMvc.perform(post("/api/requests/" + id + "/submit").header("Authorization", token));
    }

    private String pendingRequest() throws Exception {
        String id = createdId(employee, CARD_REQUEST);
        submit(employee, id).andExpect(status().isOk());
        return id;
    }

    private String approvedRequest() throws Exception {
        String id = pendingRequest();
        action(supervisor, id, "approve").andExpect(status().isOk());
        return id;
    }

    private ResultActions action(String token, String id, String name) throws Exception {
        return mockMvc.perform(post("/api/approvals/" + id + "/" + name).header("Authorization", token));
    }

    private ResultActions action(String token, String id, String name, String json) throws Exception {
        return mockMvc.perform(post("/api/approvals/" + id + "/" + name).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String statusOf(String id) {
        return jdbc.queryForObject("SELECT status FROM service_requests WHERE id = ?::uuid", String.class, id);
    }

    private int countRows(String table, String requestId) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE request_id = ?::uuid",
                Integer.class, requestId);
        return count == null ? 0 : count;
    }

    private List<String> auditActions(String id) {
        return jdbc.queryForList("SELECT action FROM audit_events WHERE entity_type = 'SERVICE_REQUEST' "
                + "AND entity_id = ? ORDER BY created_at", String.class, id);
    }
}
