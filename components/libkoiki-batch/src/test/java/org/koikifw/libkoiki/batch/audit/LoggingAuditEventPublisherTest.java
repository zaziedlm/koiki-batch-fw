package org.koikifw.libkoiki.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.koikifw.libkoiki.batch.security.RedactingMasker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

class LoggingAuditEventPublisherTest {

    private final LoggingAuditEventPublisher publisher = new LoggingAuditEventPublisher();

    private ListAppender<ILoggingEvent> appender;

    private Logger auditLogger;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        auditLogger = (Logger) LoggerFactory.getLogger(LoggingAuditEventPublisher.AUDIT_LOGGER_NAME);
        auditLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        auditLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void formatIncludesAllProvidedFields() {
        AuditEvent event = new AuditEvent(
                Instant.parse("2026-05-30T01:23:45Z"),
                "CUSTOMER_SYNCED",
                "Daily customer sync completed",
                "customer-daily-sync",
                42L,
                "20260530",
                "req-1",
                Map.of("customerCount", "1234"));

        String line = LoggingAuditEventPublisher.format(event);

        assertThat(line)
                .contains("occurredAt=2026-05-30T01:23:45Z")
                .contains("eventType=CUSTOMER_SYNCED")
                .contains("message=\"Daily customer sync completed\"")
                .contains("jobName=customer-daily-sync")
                .contains("jobExecutionId=42")
                .contains("bizDate=20260530")
                .contains("requestId=req-1")
                .contains("attr.customerCount=\"1234\"");
    }

    @Test
    void formatOmitsNullOptionalFields() {
        AuditEvent event = new AuditEvent(
                Instant.parse("2026-05-30T01:23:45Z"),
                "TYPE",
                "msg",
                null, null, null, null, null);

        String line = LoggingAuditEventPublisher.format(event);

        assertThat(line)
                .doesNotContain("jobName=")
                .doesNotContain("jobExecutionId=")
                .doesNotContain("bizDate=")
                .doesNotContain("requestId=")
                .doesNotContain("attr.");
    }

    @Test
    void publishWritesToAuditLoggerWithMarkerAndInfoLevel() {
        AuditEvent event = AuditEventBuilder.builder()
                .eventType("CUSTOMER_SYNCED")
                .message("done")
                .build();

        publisher.publish(event);

        assertThat(appender.list).hasSize(1);
        ILoggingEvent logged = appender.list.get(0);
        assertThat(logged.getLevel()).isEqualTo(Level.INFO);
        assertThat(logged.getLoggerName()).isEqualTo(LoggingAuditEventPublisher.AUDIT_LOGGER_NAME);
        assertThat(logged.getMarkerList()).anySatisfy(marker ->
                assertThat(marker.getName()).isEqualTo(LoggingAuditEventPublisher.AUDIT_MARKER_NAME));
        assertThat(logged.getFormattedMessage())
                .contains("eventType=CUSTOMER_SYNCED")
                .contains("message=\"done\"");
    }

    @Test
    void publishDoesNotThrowOnBadInput() {
        assertThatCode(() -> publisher.publish(null)).doesNotThrowAnyException();
        // The audit appender must not have received anything for the failed call.
        assertThat(appender.list).isEmpty();
    }

    @Test
    void masksSensitiveAttributeValuesAndLeavesOthersClear() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("customerCount", "1234");
        attributes.put("accountId", "ACC-9876");
        AuditEvent event = AuditEventBuilder.builder()
                .eventType("CUSTOMER_SYNCED")
                .message("done")
                .build();
        // Build an event carrying both attributes via the record directly.
        AuditEvent withAttrs = new AuditEvent(event.occurredAt(), event.eventType(), event.message(),
                null, null, null, null, attributes);

        String line = LoggingAuditEventPublisher.format(
                withAttrs, new RedactingMasker(), Set.of("accountId"));

        assertThat(line)
                .contains("attr.customerCount=\"1234\"")
                .contains("attr.accountId=\"" + RedactingMasker.DEFAULT_MASK + "\"")
                .doesNotContain("ACC-9876");
    }

    @Test
    void formatEscapesQuotedValuesAndKeepsOneLogicalLine() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("note", "a\"b\\c\r\n\tend");
        attributes.put("nullable", null);
        AuditEvent event = new AuditEvent(
                Instant.parse("2026-05-30T01:23:45Z"),
                "TYPE",
                "line1\nline2 \"quoted\" \\ path",
                null, null, null, null, attributes);

        String line = LoggingAuditEventPublisher.format(event);

        assertThat(line)
                .contains("message=\"line1\\nline2 \\\"quoted\\\" \\\\ path\"")
                .contains("attr.note=\"a\\\"b\\\\c\\r\\n\\tend\"")
                .contains("attr.nullable=\"\"")
                .doesNotContain("\r")
                .doesNotContain("\n")
                .doesNotContain("\t");
    }

    @Test
    void formatEscapesMaskedAndUnmaskedValues() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("accountId", "ACC-\n9876");
        attributes.put("description", "raw\nvalue");
        AuditEvent event = new AuditEvent(
                Instant.parse("2026-05-30T01:23:45Z"),
                "TYPE",
                "msg",
                null, null, null, null, attributes);

        String line = LoggingAuditEventPublisher.format(
                event, new RedactingMasker("mask\nvalue"), Set.of("accountId"));

        assertThat(line)
                .contains("attr.accountId=\"mask\\nvalue\"")
                .contains("attr.description=\"raw\\nvalue\"")
                .doesNotContain("ACC-")
                .doesNotContain("\n");
    }

    @Test
    void doesNotMaskWhenNoMaskerConfigured() {
        AuditEvent event = new AuditEvent(
                Instant.parse("2026-05-30T01:23:45Z"), "TYPE", "msg",
                null, null, null, null, Map.of("accountId", "ACC-9876"));

        assertThat(LoggingAuditEventPublisher.format(event, null, Set.of("accountId")))
                .contains("attr.accountId=\"ACC-9876\"");
    }

    @Test
    void publishWithMaskerMasksSensitiveAttribute() {
        LoggingAuditEventPublisher maskingPublisher =
                new LoggingAuditEventPublisher(new RedactingMasker(), Set.of("accountId"));

        AuditEvent event = new AuditEvent(
                Instant.parse("2026-05-30T01:23:45Z"), "CUSTOMER_SYNCED", "done",
                null, null, null, null, Map.of("accountId", "ACC-9876"));

        maskingPublisher.publish(event);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage())
                .contains("attr.accountId=\"" + RedactingMasker.DEFAULT_MASK + "\"")
                .doesNotContain("ACC-9876");
    }

    @Test
    void publishSwallowsExceptionFromLoggerInfrastructure() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        TurboFilter throwingFilter = new TurboFilter() {
            @Override
            public FilterReply decide(Marker marker, Logger logger, ch.qos.logback.classic.Level level,
                    String format, Object[] params, Throwable t) {
                if (LoggingAuditEventPublisher.AUDIT_LOGGER_NAME.equals(logger.getName())) {
                    throw new RuntimeException("turbo filter boom");
                }
                return FilterReply.NEUTRAL;
            }
        };
        throwingFilter.start();
        context.addTurboFilter(throwingFilter);
        try {
            AuditEvent event = AuditEventBuilder.builder().eventType("TYPE").message("msg").build();
            assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
        } finally {
            context.getTurboFilterList().remove(throwingFilter);
            throwingFilter.stop();
        }
    }
}
