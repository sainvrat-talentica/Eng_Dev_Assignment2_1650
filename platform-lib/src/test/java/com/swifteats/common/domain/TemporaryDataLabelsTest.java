package com.swifteats.common.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryDataLabelsTest {

    @Test
    void prefix_addsMarkerWhenMissing() {
        assertThat(TemporaryDataLabels.prefix("Misal House")).isEqualTo("[T] Misal House");
    }

    @Test
    void prefix_isIdempotent() {
        assertThat(TemporaryDataLabels.prefix("[T] Misal House")).isEqualTo("[T] Misal House");
    }
}
