package org.koikifw.libkoiki.batch.audit;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Default {@link AuditEventPublisher} that writes events to a dedicated audit
 * logger ({@code org.koikifw.audit}) with the {@code AUDIT} SLF4J marker, so
 * operators can route audit output to a separate appender / file via their
 * logging configuration.
 *
 * <p>Output uses a {@code key=value} line format. Optional fields that are
 * {@code null} are omitted. Free-form values ({@code message} and attribute
 * values) are double-quoted; callers must not embed double-quotes or newlines
 * in them.</p>
 *
 * <p>The publisher never throws: any failure during formatting or logging is
 * captured and reported to this class's own (non-audit) logger as a warning.</p>
 */
public class LoggingAuditEventPublisher implements AuditEventPublisher {

    /** Logger name reserved for the framework's audit output. */
    public static final String AUDIT_LOGGER_NAME = "org.koikifw.audit";

    /** SLF4J marker attached to every audit log event. */
    public static final String AUDIT_MARKER_NAME = "AUDIT";

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger(AUDIT_LOGGER_NAME);

    private static final Marker AUDIT_MARKER = MarkerFactory.getMarker(AUDIT_MARKER_NAME);

    private static final Logger DIAGNOSTIC_LOGGER = LoggerFactory.getLogger(LoggingAuditEventPublisher.class);

    @Override
    public void publish(AuditEvent event) {
        try {
            AUDIT_LOGGER.info(AUDIT_MARKER, format(event));
        } catch (Exception ex) {
            DIAGNOSTIC_LOGGER.warn("Failed to publish audit event of type {}", safeType(event), ex);
        }
    }

    static String format(AuditEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("occurredAt=").append(event.occurredAt());
        sb.append(" eventType=").append(event.eventType());
        sb.append(" message=\"").append(event.message()).append('"');
        appendIfNotNull(sb, "jobName", event.jobName());
        appendIfNotNull(sb, "jobExecutionId", event.jobExecutionId());
        appendIfNotNull(sb, "bizDate", event.bizDate());
        appendIfNotNull(sb, "requestId", event.requestId());
        for (Map.Entry<String, String> entry : event.attributes().entrySet()) {
            sb.append(" attr.").append(entry.getKey()).append("=\"").append(entry.getValue()).append('"');
        }
        return sb.toString();
    }

    private static void appendIfNotNull(StringBuilder sb, String key, Object value) {
        if (value != null) {
            sb.append(' ').append(key).append('=').append(value);
        }
    }

    private static String safeType(AuditEvent event) {
        return event == null ? "<null>" : event.eventType();
    }
}
