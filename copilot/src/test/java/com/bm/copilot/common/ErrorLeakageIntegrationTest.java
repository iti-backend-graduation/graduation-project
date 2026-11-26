package com.bm.copilot.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bm.copilot.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Error responses never show class names, packages or stack traces. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ErrorLeakageIntegrationTest {

    private static final String[] INTERNALS = {"Exception", "org.springframework", "com.bm.copilot", "java.", "\tat ", "stackTrace", "trace"};

    @Autowired
    MockMvc mockMvc;

    @Test
    void malformedJsonLeaksNothing() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andReturn().getResponse().getContentAsString();

        assertNoInternals(body);
    }

    @Test
    void aWrongMethodLeaksNothing() throws Exception {
        String body = mockMvc.perform(get("/api/auth/login"))
                .andReturn().getResponse().getContentAsString();

        assertNoInternals(body);
    }

    @Test
    void anUnknownPathLeaksNothing() throws Exception {
        String body = mockMvc.perform(get("/api/does-not-exist").with(employee()))
                .andReturn().getResponse().getContentAsString();

        assertNoInternals(body);
    }

    private static void assertNoInternals(String body) {
        assertThat(body).isNotBlank();
        for (String internal : INTERNALS) {
            assertThat(body).as("response must not contain '%s'", internal).doesNotContain(internal);
        }
    }

    private static RequestPostProcessor employee() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }
}
