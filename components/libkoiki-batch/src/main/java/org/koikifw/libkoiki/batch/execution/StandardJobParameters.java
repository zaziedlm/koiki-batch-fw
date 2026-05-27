package org.koikifw.libkoiki.batch.execution;

import java.time.format.DateTimeFormatter;

/**
 * Standard job parameter keys for KOIKI batch jobs.
 *
 * <p>These keys form the operation contract shared with schedulers such as JP1.</p>
 */
public final class StandardJobParameters {

    public static final String JOB_NAME = "job.name";

    public static final String BIZ_DATE = "job.bizDate";

    public static final String REQUEST_ID = "job.requestId";

    /** Business date is exchanged as a {@code yyyyMMdd} string. */
    public static final DateTimeFormatter BIZ_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private StandardJobParameters() {
    }
}
