package com.bm.copilot.audit;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bm.copilot.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Who may read AI usage, and that the correlation ID filter works. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AiUsageControllerTest {

    private static final String URL = "/api/admin/ai-usage";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AiUsageService service;

    @AfterEach
    void clearLoggingContext() {
        MDC.remove(DatabaseAiUsageService.CORRELATION_ID_KEY);
    }

    @Test
    void requestWithoutATokenIsRejected() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void employeeIsForbidden() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + login("employee1", "Employee#123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void supervisorSeesOnlyTheCallsOfOneCorrelationId() throws Exception {
        String wanted = UUID.randomUUID().toString();
        MDC.put(DatabaseAiUsageService.CORRELATION_ID_KEY, wanted);
        service.record(new AiUsageCommand("RETRIEVAL_AGENT", "test-model", 10, 5));
        service.record(new AiUsageCommand("ANSWER_GENERATION", "test-model", 20, 10));
        MDC.put(DatabaseAiUsageService.CORRELATION_ID_KEY, UUID.randomUUID().toString());
        service.record(new AiUsageCommand("ANSWER_GENERATION", "test-model", 99, 99));

        mockMvc.perform(get(URL).param("correlationId", wanted)
                        .header("Authorization", "Bearer " + login("supervisor1", "Supervisor#123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].step", containsInAnyOrder("RETRIEVAL_AGENT", "ANSWER_GENERATION")))
                .andExpect(jsonPath("$[0].correlationId").value(wanted))
                .andExpect(jsonPath("$[0].estimatedCost").exists());
    }

    @Test
    void supervisorWithoutAFilterGetsTheLatestCalls() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + login("supervisor1", "Supervisor#123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private String login(String username, String password) throws Exception {
        String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
