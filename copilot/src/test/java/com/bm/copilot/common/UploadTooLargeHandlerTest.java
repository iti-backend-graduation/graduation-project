package com.bm.copilot.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** An upload over the multipart limit gives 413 in the standard format, not a generic 500. */
class UploadTooLargeHandlerTest {

    @Test
    void anOversizedUploadIsAnswered413() {
        ResponseEntity<ApiError> response = new GlobalExceptionHandler().handlePayloadTooLarge(
                new MaxUploadSizeExceededException(10_485_760L),
                new MockHttpServletRequest("POST", "/api/admin/documents"));

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody().code()).isEqualTo("PAYLOAD_TOO_LARGE");
        assertThat(response.getBody().path()).isEqualTo("/api/admin/documents");
    }
}
