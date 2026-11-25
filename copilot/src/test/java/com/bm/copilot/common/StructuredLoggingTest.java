package com.bm.copilot.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.bm.copilot.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

/** Console logs are JSON, and the correlation ID from the MDC is a top-level field. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingTest {

    private static final Logger log = LoggerFactory.getLogger("structured-logging-test");

    @Test
    void logLinesAreJsonAndCarryTheCorrelationId(CapturedOutput output) {
        MDC.put(CorrelationId.KEY, "test-correlation-4711");
        try {
            log.info("structured-logging-probe");
        } finally {
            MDC.remove(CorrelationId.KEY);
        }

        String line = output.getAll().lines()
                .filter(l -> l.contains("structured-logging-probe"))
                .findFirst()
                .orElseThrow();

        assertThat(line).startsWith("{").endsWith("}");
        assertThat(line).contains("\"@timestamp\"");
        assertThat(line).contains("\"level\":\"INFO\"");
        assertThat(line).contains("\"message\":\"structured-logging-probe\"");
        assertThat(line).contains("\"correlationId\":\"test-correlation-4711\"");
    }
}
