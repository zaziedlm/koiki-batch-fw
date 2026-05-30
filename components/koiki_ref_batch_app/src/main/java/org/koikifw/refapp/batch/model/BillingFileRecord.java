package org.koikifw.refapp.batch.model;

/**
 * A billing row read from and written to a delimited file by the
 * {@code billing-file} reference job.
 */
public record BillingFileRecord(String customerId, long amount) {
}
