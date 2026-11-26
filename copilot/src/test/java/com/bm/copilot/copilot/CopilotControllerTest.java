package com.bm.copilot.copilot;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bm.copilot.TestcontainersConfiguration;
import com.bm.copilot.copilot.agent.CopilotOrchestrator;
import com.bm.copilot.copilot.agent.RequestDraft;
import com.bm.copilot.retrieval.Citation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CopilotControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CopilotOrchestrator orchestrator;

    @Test
    void askWithoutATokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/copilot/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How much is a card replacement?\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeGetsTheAnswerWithCitations() throws Exception {
        when(orchestrator.ask(anyString())).thenReturn(new AskResponse("The fee is 100 EGP.",
                List.of(new Citation("16-synthetic-fee-schedule.pdf", 2, "Card fees")), false, true,
                new RequestDraft("CARD_SERVICE", "Card replacement", List.of("customerReference")), null));

        mockMvc.perform(post("/api/copilot/ask").with(employee()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How much is a card replacement?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("The fee is 100 EGP."))
                .andExpect(jsonPath("$.citations[0].documentName").value("16-synthetic-fee-schedule.pdf"))
                .andExpect(jsonPath("$.citations[0].page").value(2))
                .andExpect(jsonPath("$.refused").value(false))
                .andExpect(jsonPath("$.suggestServiceRequest").value(true))
                .andExpect(jsonPath("$.requestDraft.category").value("CARD_SERVICE"));
    }

    @Test
    void blankQuestionIsAValidationError() throws Exception {
        mockMvc.perform(post("/api/copilot/ask").with(employee()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor employee() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }
}
