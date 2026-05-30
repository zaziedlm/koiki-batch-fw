package org.koikifw.libkoiki.batch.execution;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * Standard job parameter keys for KOIKI batch jobs.
 *
 * <p>These keys form the operation contract shared with schedulers such as JP1.</p>
 */
public final class StandardJobParameters {

    public static final String JOB_NAME = "job.name";

    public static final String BIZ_DATE = "job.bizDate";

    public static final String REQUEST_ID = "job.requestId";

    /**
     * Business date is exchanged as a {@code yyyyMMdd} string. The formatter
     * uses {@code uuuu} (proleptic year) with {@link ResolverStyle#STRICT} so
     * that impossible calendar dates (e.g. {@code 20260230}) are rejected
     * instead of being silently rolled into a neighbouring valid date.
     */
    public static final DateTimeFormatter BIZ_DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private StandardJobParameters() {
    }
}
