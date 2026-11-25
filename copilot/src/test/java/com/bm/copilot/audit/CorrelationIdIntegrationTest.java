package com.bm.copilot.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bm.copilot.TestcontainersConfiguration;
import com.bm.copilot.common.CorrelationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** The filter inside the real application: the ID must appear in headers and in error bodies, even for 401. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CorrelationIdIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void aRejectedRequestCarriesTheCallersIdInTheHeaderAndTheErrorBody() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(CorrelationId.HEADER, "trace-401-test"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CorrelationId.HEADER, "trace-401-test"))
                .andExpect(jsonPath("$.correlationId").value("trace-401-test"));
    }

    @Test
    void aValidationErrorGetsAGeneratedIdInHeaderAndBody() throws Exception {
        String header = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").exists())
                .andReturn().getResponse().getHeader(CorrelationId.HEADER);

        assertThat(header).isNotBlank();
    }
}
