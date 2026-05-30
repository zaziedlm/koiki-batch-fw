package org.koikifw.libkoiki.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AuditEventTest {

    @Test
    void requiresMandatoryFields() {
        Instant now = Instant.parse("2026-05-30T00:00:00Z");
        assertThatThrownBy(() -> new AuditEvent(null, "TYPE", "msg", null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditEvent(now, null, "msg", null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditEvent(now, "TYPE", null, null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void normalizesNullAttributesToEmptyMap() {
        AuditEvent event = new AuditEvent(Instant.now(), "TYPE", "msg", null, null, null, null, null);
        assertThat(event.attributes()).isEmpty();
    }

    @Test
    void attributesAreImmutableAndIsolated() {
        Map<String, String> source = new HashMap<>();
        source.put("k", "v");
        AuditEvent event = new AuditEvent(Instant.now(), "TYPE", "msg", null, null, null, null, source);

        // Mutating the original map must not leak into the event.
        source.put("k", "v2");
        source.put("k2", "v3");
        assertThat(event.attributes()).containsExactlyEntriesOf(Map.of("k", "v"));

        // The event's attributes view must reject modification.
        assertThatThrownBy(() -> event.attributes().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void attributesCanCarryNullValuesForDefensiveFormatting() {
        Map<String, String> source = new HashMap<>();
        source.put("nullable", null);

        AuditEvent event = new AuditEvent(Instant.now(), "TYPE", "msg", null, null, null, null, source);

        assertThat(event.attributes()).containsEntry("nullable", null);
    }
}
