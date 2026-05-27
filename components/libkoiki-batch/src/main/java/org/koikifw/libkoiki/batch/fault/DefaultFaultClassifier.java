package org.koikifw.libkoiki.batch.fault;

/**
 * Default classifier: walks the cause chain and maps the first KOIKI exception
 * found. Unclassified failures fall back to {@link FaultCategory#SYSTEM_ERROR}
 * (the safe side for operations).
 */
public class DefaultFaultClassifier implements FaultClassifier {

    private static final int MAX_DEPTH = 50;

    @Override
    public FaultCategory classify(Throwable throwable) {
        if (throwable == null) {
            return FaultCategory.NORMAL;
        }
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_DEPTH; depth++) {
            if (current instanceof BusinessException) {
                return FaultCategory.BUSINESS_ERROR;
            }
            if (current instanceof SystemException) {
                return FaultCategory.SYSTEM_ERROR;
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        return FaultCategory.SYSTEM_ERROR;
    }
}
