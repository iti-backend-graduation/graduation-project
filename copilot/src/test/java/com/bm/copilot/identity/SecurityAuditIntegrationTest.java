package com.bm.copilot.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bm.copilot.TestcontainersConfiguration;
import com.bm.copilot.common.CorrelationId;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Login and refusal events reach the audit trail, with the request's correlation ID, and leak nothing. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityAuditIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UserRepository users;

    @Test
    void successfulLoginIsAudited() throws Exception {
        String correlationId = UUID.randomUUID().toString();

        login("employee1", "Employee#123", correlationId).andExpect(status().isOk());

        Map<String, Object> event = singleEventOf(correlationId);
        assertThat(event.get("action")).isEqualTo("LOGIN_SUCCEEDED");
        assertThat(event.get("actor_id")).isEqualTo(userId("employee1"));
        assertThat(event.get("entity_id")).isEqualTo(userId("employee1").toString());
    }

    @Test
    void wrongPasswordIsAuditedWithoutStoringThePasswordAndTheResponseIsUnchanged() throws Exception {
        String correlationId = UUID.randomUUID().toString();

        login("employee1", "wrong-password-xyz-4821", correlationId)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication failed"));

        Map<String, Object> event = singleEventOf(correlationId);
        assertThat(event.get("action")).isEqualTo("LOGIN_FAILED");
        assertThat(event.get("actor_id")).isEqualTo(userId("employee1"));
        assertThat((String) event.get("details")).contains("WRONG_PASSWORD").doesNotContain("wrong-password-xyz-4821");
    }

    @Test
    void unknownUserIsAuditedWithoutStoringTheTypedUsername() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        String typedName = "ghost-login-name-9137";

        login(typedName, "whatever-password", correlationId)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication failed"));

        Map<String, Object> event = singleEventOf(correlationId);
        assertThat(event.get("action")).isEqualTo("LOGIN_FAILED");
        assertThat(event.get("actor_id")).isNull();
        assertThat(event.get("entity_id")).isEqualTo("UNKNOWN");
        assertThat((String) event.get("details")).contains("UNKNOWN_USER");

        Integer traces = jdbc.queryForObject(
                "SELECT count(*) FROM audit_events WHERE details::text LIKE ? OR entity_id LIKE ?",
                Integer.class, "%" + typedName + "%", "%" + typedName + "%");
        assertThat(traces).isZero();
    }

    @Test
    void aRefusedEmployeeIsAudited() throws Exception {
        String token = JsonPath.read(login("employee1", "Employee#123", UUID.randomUUID().toString())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        String correlationId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/admin/documents")
                        .header("Authorization", "Bearer " + token)
                        .header(CorrelationId.HEADER, correlationId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        Map<String, Object> event = singleEventOf(correlationId);
        assertThat(event.get("action")).isEqualTo("ACCESS_DENIED");
        assertThat(event.get("actor_id")).isEqualTo(userId("employee1"));
        assertThat(event.get("entity_id")).isEqualTo("/api/admin/documents");
        assertThat((String) event.get("details")).contains("EMPLOYEE").contains("GET");
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password,
                                                                     String correlationId) throws Exception {
        String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        return mockMvc.perform(post("/api/auth/login")
                .header(CorrelationId.HEADER, correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Map<String, Object> singleEventOf(String correlationId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT action, actor_id, entity_type, entity_id, details::text AS details "
                        + "FROM audit_events WHERE correlation_id = ?", correlationId);
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }

    private UUID userId(String username) {
        return users.findByUsername(username).orElseThrow().getId();
    }
}
