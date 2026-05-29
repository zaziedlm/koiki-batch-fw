package org.koikifw.libkoiki.batch.audit;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.koikifw.libkoiki.batch.execution.StandardJobParameters;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;

/**
 * Fluent builder for {@link AuditEvent}. Use {@link #builder()} to obtain an
 * instance; the underlying attribute map is copied at {@link #build()} time so
 * later mutation of the builder cannot affect a previously built event.
 */
public final class AuditEventBuilder {

    private Instant occurredAt;

    private String eventType;

    private String message;

    private String jobName;

    private Long jobExecutionId;

    private String bizDate;

    private String requestId;

    private final Map<String, String> attributes = new LinkedHashMap<>();

    private AuditEventBuilder() {
    }

    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }

    public AuditEventBuilder occurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
        return this;
    }

    public AuditEventBuilder eventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public AuditEventBuilder message(String message) {
        this.message = message;
        return this;
    }

    public AuditEventBuilder jobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    public AuditEventBuilder jobExecutionId(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
        return this;
    }

    public AuditEventBuilder bizDate(String bizDate) {
        this.bizDate = bizDate;
        return this;
    }

    public AuditEventBuilder requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public AuditEventBuilder attribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Populates {@code jobName} / {@code jobExecutionId} / {@code bizDate} /
     * {@code requestId} from the given Spring Batch {@link JobExecution} (and
     * its {@link JobParameters}), matching the keys defined in
     * {@code StandardJobParameters}.
     */
    public AuditEventBuilder context(JobExecution jobExecution) {
        this.jobName = jobExecution.getJobInstance().getJobName();
        this.jobExecutionId = jobExecution.getId();
        JobParameters parameters = jobExecution.getJobParameters();
        this.bizDate = parameters.getString(StandardJobParameters.BIZ_DATE);
        this.requestId = parameters.getString(StandardJobParameters.REQUEST_ID);
        return this;
    }

    public AuditEvent build() {
        Instant ts = occurredAt != null ? occurredAt : Instant.now();
        return new AuditEvent(ts, eventType, message, jobName, jobExecutionId, bizDate, requestId, attributes);
    }
}
