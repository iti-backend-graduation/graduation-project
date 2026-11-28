package com.bm.copilot.servicerequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bm.copilot.TestcontainersConfiguration;
import com.bm.copilot.WorkflowTestTokens;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
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

/** Employee endpoints end to end: real tokens, real PostgreSQL (Testcontainers), validation, ownership and audit. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ServiceRequestApiIntegrationTest {

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
    private String otherEmployee;
    private String supervisor;

    @BeforeEach
    void tokens() throws Exception {
        employee = WorkflowTestTokens.employee1(mockMvc);
        supervisor = WorkflowTestTokens.supervisor1(mockMvc);
        otherEmployee = WorkflowTestTokens.tokenFor(jwtEncoder, UUID.randomUUID(), "employee2", "EMPLOYEE");
    }

    @Test
    void createReturns201WithADraftOwnedByTheCaller() throws Exception {
        create(employee, CARD_REQUEST)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.startsWith("/api/requests/")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.category").value("CARD_SERVICE"))
                .andExpect(jsonPath("$.cardLastFour").value("4821"))
                .andExpect(jsonPath("$.submittedAt").doesNotExist());
    }

    @Test
    void missingCategoryOrEmptyDescriptionIs400WithTheSharedApiError() throws Exception {
        create(employee, "{\"description\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.violations[*].field",
                        Matchers.containsInAnyOrder("category", "description")));
    }

    @Test
    void badFormatsAre400() throws Exception {
        create(employee, """
                {"category":"CARD_SERVICE","customerReference":"CUST 1; DROP","cardLastFour":"48a1","description":"x"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[*].field",
                        Matchers.containsInAnyOrder("customerReference", "cardLastFour")));
    }

    @Test
    void unknownCategoryIs400() throws Exception {
        create(employee, "{\"category\":\"WIRE_MONEY\",\"description\":\"x\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void cardRequestWithoutCardDigitsIs400() throws Exception {
        create(employee, "{\"category\":\"CARD_SERVICE\",\"description\":\"Replace card\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].field").value("cardLastFour"));
    }

    @Test
    void cardDigitsOnANonCardRequestAre400() throws Exception {
        create(employee, "{\"category\":\"ACCOUNT_SERVICE\",\"cardLastFour\":\"1234\",\"description\":\"Close account\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("cardLastFour"));
    }

    @Test
    void ownerEditsADraftAndTheChangeIsInTheHistory() throws Exception {
        String id = createdId(employee, CARD_REQUEST);

        mockMvc.perform(put("/api/requests/" + id).header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CARD_REQUEST.replace("4821", "7730")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardLastFour").value("7730"));

        mockMvc.perform(get("/api/requests/" + id).header("Authorization", employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[*].field", Matchers.contains("status", "cardLastFour")))
                .andExpect(jsonPath("$.history[1].oldValue").value("4821"))
                .andExpect(jsonPath("$.history[1].newValue").value("7730"));
        assertThat(auditActions(id)).containsExactly("SERVICE_REQUEST_CREATED", "SERVICE_REQUEST_UPDATED");
    }

    @Test
    void submitMovesToPendingApprovalAndEditingIsThenRefusedWith409() throws Exception {
        String id = createdId(employee, CARD_REQUEST);

        submit(employee, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());

        mockMvc.perform(put("/api/requests/" + id).header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content(CARD_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        submit(employee, id).andExpect(status().isConflict());
        assertThat(auditActions(id)).containsExactly("SERVICE_REQUEST_CREATED", "SERVICE_REQUEST_SUBMITTED");
    }

    @Test
    void anIncompleteDraftCannotBeSubmitted() throws Exception {
        String id = createdId(employee, "{\"category\":\"DIGITAL_BANKING\",\"description\":\"Reset app login\"}");

        submit(employee, id)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("customerReference"));
        mockMvc.perform(get("/api/requests/" + id).header("Authorization", employee))
                .andExpect(jsonPath("$.request.status").value("DRAFT"));
    }

    @Test
    void anotherEmployeesRequestIs404ForReadEditAndSubmit() throws Exception {
        String id = createdId(employee, CARD_REQUEST);

        mockMvc.perform(get("/api/requests/" + id).header("Authorization", otherEmployee))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(put("/api/requests/" + id).header("Authorization", otherEmployee)
                        .contentType(MediaType.APPLICATION_JSON).content(CARD_REQUEST))
                .andExpect(status().isNotFound());
        submit(otherEmployee, id).andExpect(status().isNotFound());

        // the same answer as for an id that does not exist at all
        mockMvc.perform(get("/api/requests/" + UUID.randomUUID()).header("Authorization", otherEmployee))
                .andExpect(status().isNotFound());
    }

    @Test
    void mineListsOnlyTheCallersRequests() throws Exception {
        String mine = createdId(employee, CARD_REQUEST);
        String theirs = createdId(otherEmployee, CARD_REQUEST);

        String body = mockMvc.perform(get("/api/requests/mine").header("Authorization", otherEmployee))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> ids = JsonPath.read(body, "$[*].id");
        assertThat(ids).contains(theirs).doesNotContain(mine);
    }

    @Test
    void aSupervisorCanReadAnyRequestButNotEditIt() throws Exception {
        String id = createdId(employee, CARD_REQUEST);

        mockMvc.perform(get("/api/requests/" + id).header("Authorization", supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.id").value(id));
        mockMvc.perform(put("/api/requests/" + id).header("Authorization", supervisor)
                        .contentType(MediaType.APPLICATION_JSON).content(CARD_REQUEST))
                .andExpect(status().isNotFound());
    }

    @Test
    void aMalformedIdIs400() throws Exception {
        mockMvc.perform(get("/api/requests/not-a-uuid").header("Authorization", employee))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    private ResultActions create(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/requests").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String createdId(String token, String json) throws Exception {
        String body = create(token, json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions submit(String token, String id) throws Exception {
        return mockMvc.perform(post("/api/requests/" + id + "/submit").header("Authorization", token));
    }

    private List<String> auditActions(String id) {
        return jdbc.queryForList("SELECT action FROM audit_events WHERE entity_type = 'SERVICE_REQUEST' "
                + "AND entity_id = ? ORDER BY created_at, action", String.class, id);
    }
}
