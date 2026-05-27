package org.koikifw.libkoiki.batch.fault;

/**
 * Technical failure caused by infrastructure, an unexpected exception, or an
 * unrecoverable condition. Maps to exit code {@code 30}.
 */
public class SystemException extends KoikiBatchException {

    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
