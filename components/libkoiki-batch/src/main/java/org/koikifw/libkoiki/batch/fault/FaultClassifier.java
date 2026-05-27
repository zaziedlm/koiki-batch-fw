package org.koikifw.libkoiki.batch.fault;

/**
 * Classifies a throwable into a stable {@link FaultCategory}.
 */
public interface FaultClassifier {

    FaultCategory classify(Throwable throwable);
}
