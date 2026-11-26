package com.bm.copilot.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** A body over the limit is refused with 413 in the standard error format; a normal one is still processed. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RequestSizeIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void anOversizedBodyIsRefusedWith413() throws Exception {
        String hugePassword = "x".repeat(70_000);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"employee1\",\"password\":\"" + hugePassword + "\"}"))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));
    }

    @Test
    void aNormalBodyIsStillProcessed() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"size-check-user\",\"password\":\"short\"}"))
                .andExpect(status().isUnauthorized());
    }
}
