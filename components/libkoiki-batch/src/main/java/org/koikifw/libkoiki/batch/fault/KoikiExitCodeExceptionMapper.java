package org.koikifw.libkoiki.batch.fault;

import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.boot.ExitCodeExceptionMapper;

/**
 * Maps exceptions that escape to application startup (for example a job
 * parameter validation failure thrown before any job execution exists) onto the
 * KOIKI process exit codes {@code 0/10/20/30}.
 *
 * <p>Without this, Spring Boot reports its default exit code {@code 1} for such
 * failures, which is outside the JP1 return-code contract. Job parameter errors
 * are treated as input errors (business, {@code 20}); anything else is
 * classified by {@link FaultClassifier} (unknown failures default to system,
 * {@code 30}).</p>
 */
public class KoikiExitCodeExceptionMapper implements ExitCodeExceptionMapper {

    private static final int MAX_DEPTH = 50;

    private final FaultClassifier faultClassifier;

    public KoikiExitCodeExceptionMapper(FaultClassifier faultClassifier) {
        this.faultClassifier = faultClassifier;
    }

    @Override
    public int getExitCode(Throwable exception) {
        if (containsInvalidJobParameters(exception)) {
            return FaultCategory.BUSINESS_ERROR.exitCode();
        }
        return faultClassifier.classify(exception).exitCode();
    }

    private boolean containsInvalidJobParameters(Throwable throwable) {
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_DEPTH; depth++) {
            if (current instanceof InvalidJobParametersException) {
                return true;
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        return false;
    }
}
