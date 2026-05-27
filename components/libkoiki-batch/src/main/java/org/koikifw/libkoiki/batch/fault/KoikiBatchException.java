package org.koikifw.libkoiki.batch.fault;

/**
 * Base type for KOIKI batch framework exceptions.
 */
public abstract class KoikiBatchException extends RuntimeException {

    protected KoikiBatchException(String message) {
        super(message);
    }

    protected KoikiBatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
