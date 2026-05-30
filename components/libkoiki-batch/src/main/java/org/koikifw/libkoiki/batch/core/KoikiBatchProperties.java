package org.koikifw.libkoiki.batch.core;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the KOIKI batch framework, bound from {@code koiki.batch.*}.
 */
@ConfigurationProperties(prefix = "koiki.batch")
public class KoikiBatchProperties {

    private final ConcurrencyGuard concurrencyGuard = new ConcurrencyGuard();

    private final ExitCode exitCode = new ExitCode();

    private final Logging logging = new Logging();

    private final Audit audit = new Audit();

    private final Security security = new Security();

    private final Transaction transaction = new Transaction();

    public ConcurrencyGuard getConcurrencyGuard() {
        return concurrencyGuard;
    }

    public ExitCode getExitCode() {
        return exitCode;
    }

    public Logging getLogging() {
        return logging;
    }

    public Audit getAudit() {
        return audit;
    }

    public Security getSecurity() {
        return security;
    }

    public Transaction getTransaction() {
        return transaction;
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

    /** Audit event publication. */
    public static class Audit {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** Security support: masking of sensitive data in logs and audit records. */
    public static class Security {

        private final Masking masking = new Masking();

        public Masking getMasking() {
            return masking;
        }

        /** Value masking applied to audit attributes (and offered to app logs). */
        public static class Masking {

            private boolean enabled = true;

            private String mask = "***";

            private Set<String> sensitiveKeys = new LinkedHashSet<>();

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getMask() {
                return mask;
            }

            public void setMask(String mask) {
                this.mask = mask;
            }

            public Set<String> getSensitiveKeys() {
                return sensitiveKeys;
            }

            public void setSensitiveKeys(Set<String> sensitiveKeys) {
                this.sensitiveKeys = sensitiveKeys;
            }
        }
    }

    /**
     * Transaction policy defaults. The framework does not create a transaction
     * manager bean (see {@code docs/batch/db-management-architecture.md}); this
     * only exposes a default commit boundary that jobs reference explicitly when
     * building chunk steps.
     */
    public static class Transaction {

        /** Default chunk commit interval (commit boundary) for chunk-oriented steps. */
        private int defaultCommitInterval = 100;

        public int getDefaultCommitInterval() {
            return defaultCommitInterval;
        }

        public void setDefaultCommitInterval(int defaultCommitInterval) {
            this.defaultCommitInterval = defaultCommitInterval;
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
