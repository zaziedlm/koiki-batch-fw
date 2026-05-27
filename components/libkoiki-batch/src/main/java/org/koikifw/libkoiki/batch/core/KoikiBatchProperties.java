package org.koikifw.libkoiki.batch.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the KOIKI batch framework, bound from {@code koiki.batch.*}.
 */
@ConfigurationProperties(prefix = "koiki.batch")
public class KoikiBatchProperties {

    private final ConcurrencyGuard concurrencyGuard = new ConcurrencyGuard();

    private final ExitCode exitCode = new ExitCode();

    private final Logging logging = new Logging();

    public ConcurrencyGuard getConcurrencyGuard() {
        return concurrencyGuard;
    }

    public ExitCode getExitCode() {
        return exitCode;
    }

    public Logging getLogging() {
        return logging;
    }

    /** Concurrency guard against multiple running executions of the same job. */
    public static class ConcurrencyGuard {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** Mapping of batch outcomes to KOIKI process exit codes. */
    public static class ExitCode {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** Structured logging support. */
    public static class Logging {

        private final Correlation correlation = new Correlation();

        public Correlation getCorrelation() {
            return correlation;
        }

        public static class Correlation {

            private boolean enabled = true;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }
}
