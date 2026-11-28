package com.bm.copilot.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.bm.copilot.TestcontainersConfiguration;
import com.bm.copilot.copilot.AskResponse;
import com.bm.copilot.copilot.agent.CopilotOrchestrator;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The permission table of the whole API: every endpoint against anonymous, employee and supervisor callers,
 * with real login tokens. "Allowed" means the request gets past security (not 401 or 403); the result of the
 * work itself is tested elsewhere. A guard fails the build when an /api endpoint is missing from the table.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityMatrixTest {

    enum Access { ALLOWED, DENIED }

    /** method + path, then what anonymous, employee and supervisor callers may do. */
    record Row(String method, String path, Access anonymous, Access employee, Access supervisor) {
        String key() {
            return method + " " + path;
        }
    }

    private static final Access A = Access.ALLOWED;
    private static final Access D = Access.DENIED;

    static final List<Row> MATRIX = List.of(
            // public
            new Row("POST", "/api/auth/login", A, A, A),
            new Row("GET", "/actuator/health", A, A, A),
            new Row("GET", "/v3/api-docs", A, A, A),
            // any logged-in user
            new Row("GET", "/api/auth/me", D, A, A),
            new Row("POST", "/api/copilot/ask", D, A, A),
            new Row("POST", "/api/requests", D, A, A),
            new Row("GET", "/api/requests/mine", D, A, A),
            new Row("GET", "/api/requests/{id}", D, A, A),
            new Row("PUT", "/api/requests/{id}", D, A, A),
            new Row("POST", "/api/requests/{id}/submit", D, A, A),
            // supervisor only
            new Row("GET", "/api/admin/ai-usage", D, D, A),
            new Row("GET", "/api/admin/documents", D, D, A),
            new Row("POST", "/api/admin/documents", D, D, A),
            new Row("GET", "/api/approvals/queue", D, D, A),
            new Row("POST", "/api/approvals/{id}/approve", D, D, A),
            new Row("POST", "/api/approvals/{id}/reject", D, D, A),
            new Row("POST", "/api/approvals/{id}/edit-and-approve", D, D, A),
            new Row("POST", "/api/approvals/{id}/execute", D, D, A));

    @Autowired
    MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    @MockitoBean
    CopilotOrchestrator orchestrator;

    private String employeeToken;
    private String supervisorToken;

    @BeforeEach
    void prepare() throws Exception {
        when(orchestrator.ask(anyString())).thenReturn(AskResponse.refusal());
        employeeToken = login("employee1", "Employee#123");
        supervisorToken = login("supervisor1", "Supervisor#123");
    }

    static Stream<Arguments> cases() {
        return MATRIX.stream().flatMap(row -> Stream.of(
                Arguments.of(row.key(), "anonymous", row.anonymous()),
                Arguments.of(row.key(), "employee", row.employee()),
                Arguments.of(row.key(), "supervisor", row.supervisor())));
    }

    @ParameterizedTest(name = "{0} as {1} is {2}")
    @MethodSource("cases")
    void everyEndpointFollowsThePermissionTable(String key, String caller, Access expected) throws Exception {
        AbstractMockHttpServletRequestBuilder<?> request = requestFor(key);
        switch (caller) {
            case "employee" -> request.header("Authorization", "Bearer " + employeeToken);
            case "supervisor" -> request.header("Authorization", "Bearer " + supervisorToken);
            default -> { }
        }

        int status = mockMvc.perform(request).andReturn().getResponse().getStatus();

        if (expected == Access.ALLOWED) {
            assertThat(status).as("%s as %s must get past security", key, caller).isNotIn(401, 403);
        } else if (caller.equals("anonymous")) {
            assertThat(status).as("%s as anonymous must be unauthenticated", key).isEqualTo(401);
        } else {
            assertThat(status).as("%s as %s must be forbidden", key, caller).isEqualTo(403);
        }
    }

    @Test
    void everyApiEndpointIsInThePermissionTable() {
        Set<String> covered = MATRIX.stream().map(Row::key).collect(Collectors.toSet());
        List<String> missing = new ArrayList<>();
        handlerMapping.getHandlerMethods().keySet().forEach(info -> {
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String path : info.getPatternValues()) {
                if (!path.startsWith("/api/")) {
                    continue;
                }
                if (methods.isEmpty()) {
                    missing.add("ANY " + path);
                }
                for (RequestMethod method : methods) {
                    if (!covered.contains(method + " " + path)) {
                        missing.add(method + " " + path);
                    }
                }
            }
        });

        assertThat(missing)
                .as("These /api endpoints are not in SecurityMatrixTest.MATRIX. Add a row saying who may call them.")
                .isEmpty();
    }

    private static AbstractMockHttpServletRequestBuilder<?> requestFor(String key)  {
        return switch (key) {
            case "POST /api/auth/login" -> post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON).content("{}");
            case "POST /api/copilot/ask" -> post("/api/copilot/ask")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"matrix check\"}");
            // an unsupported file type: the ingestion refuses it with 400 before doing any work
            case "POST /api/admin/documents" -> multipart("/api/admin/documents")
                    .file(new MockMultipartFile("file", "matrix.exe", "application/octet-stream", "x".getBytes()));
            default -> genericRequest(key);
        };
    }

    /** GET, or POST/PUT with an empty JSON body; "{id}" becomes a random id (so "allowed" usually means 404 or 400). */
    private static AbstractMockHttpServletRequestBuilder<?> genericRequest(String key) {
        String method = key.substring(0, key.indexOf(' '));
        String path = key.substring(key.indexOf(' ') + 1).replace("{id}", UUID.randomUUID().toString());
        return switch (method) {
            case "POST" -> post(path).contentType(MediaType.APPLICATION_JSON).content("{}");
            case "PUT" -> put(path).contentType(MediaType.APPLICATION_JSON).content("{}");
            default -> get(path);
        };
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
