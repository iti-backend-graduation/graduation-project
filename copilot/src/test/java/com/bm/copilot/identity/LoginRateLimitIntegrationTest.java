package com.bm.copilot.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bm.copilot.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** After 5 failures the sixth attempt is refused with 429 and a Retry-After header. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginRateLimitIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void theSixthAttemptIsRefusedWithTooManyRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            attempt("rate-limit-user-a").andExpect(status().isUnauthorized());
        }

        attempt("rate-limit-user-a")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void anotherUsernameIsNotBlocked() throws Exception {
        for (int i = 0; i < 5; i++) {
            attempt("rate-limit-user-b").andExpect(status().isUnauthorized());
        }

        attempt("rate-limit-user-c").andExpect(status().isUnauthorized());
    }

    private ResultActions attempt(String username) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"));
    }
}
