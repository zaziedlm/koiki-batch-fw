package org.koikifw.libkoiki.batch.audit;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.koikifw.libkoiki.batch.security.Masker;
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
 * <p>When constructed with a {@link Masker} and a set of sensitive attribute
 * keys, attribute values whose key is in that set are passed through the masker
 * before being written, so configured sensitive data never reaches the audit
 * log in the clear. The {@code message} field is free-form and not key-based:
 * keep personal data out of it (or apply a text-level mask at the logging
 * layer). This publisher is SLF4J-only and does not depend on any particular
 * logging backend.</p>
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

    private final Masker masker;

    private final Set<String> sensitiveKeys;

    /** Creates a publisher that writes audit events without masking. */
    public LoggingAuditEventPublisher() {
        this(null, Set.of());
    }

    /**
     * Creates a publisher that masks attribute values whose key is listed in
     * {@code sensitiveKeys}.
     *
     * @param masker        masker applied to sensitive attribute values; when
     *                      {@code null}, no masking is performed
     * @param sensitiveKeys attribute keys whose values must be masked; a defensive
     *                      copy is taken (may be {@code null}, treated as empty)
     */
    public LoggingAuditEventPublisher(Masker masker, Set<String> sensitiveKeys) {
        this.masker = masker;
        this.sensitiveKeys = sensitiveKeys == null ? Set.of() : new LinkedHashSet<>(sensitiveKeys);
    }

    @Override
    public void publish(AuditEvent event) {
        try {
            AUDIT_LOGGER.info(AUDIT_MARKER, format(event, masker, sensitiveKeys));
        } catch (Exception ex) {
            DIAGNOSTIC_LOGGER.warn("Failed to publish audit event of type {}", safeType(event), ex);
        }
    }

    /** Formats an event without masking (used by tests and as a default). */
    static String format(AuditEvent event) {
        return format(event, null, Set.of());
    }

    static String format(AuditEvent event, Masker masker, Set<String> sensitiveKeys) {
        StringBuilder sb = new StringBuilder();
        sb.append("occurredAt=").append(event.occurredAt());
        sb.append(" eventType=").append(event.eventType());
        sb.append(" message=\"").append(event.message()).append('"');
        appendIfNotNull(sb, "jobName", event.jobName());
        appendIfNotNull(sb, "jobExecutionId", event.jobExecutionId());
        appendIfNotNull(sb, "bizDate", event.bizDate());
        appendIfNotNull(sb, "requestId", event.requestId());
        for (Map.Entry<String, String> entry : event.attributes().entrySet()) {
            String value = maskIfSensitive(entry.getKey(), entry.getValue(), masker, sensitiveKeys);
            sb.append(" attr.").append(entry.getKey()).append("=\"").append(value).append('"');
        }
        return sb.toString();
    }

    private static String maskIfSensitive(String key, String value, Masker masker, Set<String> sensitiveKeys) {
        if (masker != null && sensitiveKeys.contains(key)) {
            return masker.mask(value);
        }
        return value;
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
