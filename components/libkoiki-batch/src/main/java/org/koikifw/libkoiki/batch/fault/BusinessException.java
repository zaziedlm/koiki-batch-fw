package org.koikifw.libkoiki.batch.fault;

/**
 * Controlled business failure caused by input, business rules, or a recoverable
 * business condition. Maps to exit code {@code 20}.
 */
public class BusinessException extends KoikiBatchException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
