package org.koikifw.refapp.batch.model;

/**
 * A customer row read from {@code customer_input} and written to {@code customer}
 * by the {@code customer-import} chunk job.
 */
public record CustomerRecord(Long id, String externalId, String email) {
}
