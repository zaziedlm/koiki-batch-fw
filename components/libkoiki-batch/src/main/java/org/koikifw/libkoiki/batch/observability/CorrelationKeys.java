package org.koikifw.libkoiki.batch.observability;

/**
 * MDC key names used by the KOIKI batch framework for log correlation.
 *
 * <p>Keys cover operational identifiers only (job/step name and id, business
 * date, request id, optional tenant). PII or secret values must never be
 * written to these keys.</p>
 */
public final class CorrelationKeys {

    public static final String JOB_NAME = "koiki.job.name";

    public static final String JOB_EXEC_ID = "koiki.job.execId";

    public static final String JOB_BIZ_DATE = "koiki.job.bizDate";

    public static final String JOB_REQUEST_ID = "koiki.job.requestId";

    public static final String JOB_TENANT = "koiki.job.tenant";

    public static final String STEP_NAME = "koiki.step.name";

    public static final String STEP_EXEC_ID = "koiki.step.execId";

    /** Optional job parameter key for tenant (only populated when present). */
    public static final String TENANT_PARAMETER = "job.tenant";

    private CorrelationKeys() {
    }
}
