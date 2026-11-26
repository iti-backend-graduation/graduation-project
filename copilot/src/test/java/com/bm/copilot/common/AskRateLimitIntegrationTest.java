package com.bm.copilot.common;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bm.copilot.TestcontainersConfiguration;
import com.bm.copilot.copilot.AskResponse;
import com.bm.copilot.copilot.agent.CopilotOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** The eleventh question within a minute from one user is refused with 429; other users are not affected. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AskRateLimitIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CopilotOrchestrator orchestrator;

    @BeforeEach
    void stubTheCopilot() {
        when(orchestrator.ask(anyString())).thenReturn(AskResponse.refusal());
    }

    @Test
    void theEleventhRequestIsRefusedWithTooManyRequests() throws Exception {
        for (int i = 0; i < 10; i++) {
            ask("ask-limit-user-a").andExpect(status().isOk());
        }

        ask("ask-limit-user-a")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void anotherUserIsNotAffected() throws Exception {
        for (int i = 0; i < 11; i++) {
            ask("ask-limit-user-b");
        }

        ask("ask-limit-user-c").andExpect(status().isOk());
    }

    private ResultActions ask(String userId) throws Exception {
        return mockMvc.perform(post("/api/copilot/ask")
                .with(jwt().jwt(token -> token.subject(userId)).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"How much is a card replacement?\"}"));
    }
}
