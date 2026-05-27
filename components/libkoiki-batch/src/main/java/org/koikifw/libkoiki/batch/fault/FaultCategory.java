package org.koikifw.libkoiki.batch.fault;

/**
 * Stable fault classification mapped to the KOIKI return code convention.
 *
 * <p>Codes match {@code ops/jp1/jobs/return-code-mapping.md}.</p>
 */
public enum FaultCategory {

    NORMAL(0),
    WARNING(10),
    BUSINESS_ERROR(20),
    SYSTEM_ERROR(30);

    private final int exitCode;

    FaultCategory(int exitCode) {
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
