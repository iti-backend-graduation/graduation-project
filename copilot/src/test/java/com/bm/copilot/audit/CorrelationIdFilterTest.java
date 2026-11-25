package com.bm.copilot.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bm.copilot.common.CorrelationId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Plain unit tests: no Spring, no database. */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void aSafeHeaderIsKeptAndVisibleWhileTheRequestRuns() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "abc-123_XYZ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInside = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seenInside.set(CorrelationId.current()));

        assertThat(seenInside.get()).isEqualTo("abc-123_XYZ");
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("abc-123_XYZ");
    }

    @Test
    void aMissingHeaderGetsAGeneratedUuid() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> { });

        String id = response.getHeader(CorrelationId.HEADER);
        assertThat(id).isNotNull();
        assertThat(UUID.fromString(id)).isNotNull(); // throws if it is not a UUID
    }

    @Test
    void anUnsafeHeaderIsReplaced() throws Exception {
        for (String unsafe : new String[] {"id with spaces", "line1\nFAKE LOG LINE", "x".repeat(65), "<script>"}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(CorrelationId.HEADER, unsafe);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (req, res) -> { });

            String id = response.getHeader(CorrelationId.HEADER);
            assertThat(id).isNotEqualTo(unsafe);
            assertThat(UUID.fromString(id)).isNotNull();
        }
    }

    @Test
    void theIdIsRemovedAfterTheRequest() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> { });

        assertThat(MDC.get(CorrelationId.KEY)).isNull();
    }
}
