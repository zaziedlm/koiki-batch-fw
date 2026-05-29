package org.koikifw.libkoiki.batch.audit;

/**
 * Boundary for emitting {@link AuditEvent}s. The framework provides a logging
 * reference implementation; persistent backends (database, queue, etc.) can be
 * supplied by replacing the bean.
 *
 * <p>Publishers must not throw to the caller — audit emission must never break
 * a business job. Internal failures are logged and otherwise swallowed.</p>
 */
public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
