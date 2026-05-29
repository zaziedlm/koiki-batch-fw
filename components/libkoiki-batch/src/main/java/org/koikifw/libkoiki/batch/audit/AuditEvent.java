package org.koikifw.libkoiki.batch.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A single business-meaningful change or control event produced by a batch job.
 *
 * <p>Audit events are kept separate from operational logs: they are intended to
 * explain, after the fact, what was changed by which batch execution. PII or
 * secret values must not be placed in {@code attributes}.</p>
 *
 * @param occurredAt      when the event occurred (required)
 * @param eventType       business-meaningful identifier such as
 *                        {@code "CUSTOMER_SYNCED"} (required)
 * @param message         human-readable summary (required)
 * @param jobName         optional Spring Batch job name
 * @param jobExecutionId  optional Spring Batch job execution id
 * @param bizDate         optional business date in {@code yyyyMMdd} form
 * @param requestId       optional scheduler request id
 * @param attributes      additional structured details (never {@code null};
 *                        immutable)
 */
public record AuditEvent(
        Instant occurredAt,
        String eventType,
        String message,
        String jobName,
        Long jobExecutionId,
        String bizDate,
        String requestId,
        Map<String, String> attributes) {

    public AuditEvent {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(message, "message must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
