package com.swifteats.analytics.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParsingUtilsTest {

    @Test
    void isFailedStatus_detectsFailedOrders() {
        assertThat(CsvParsingUtils.isFailedStatus("Failed")).isTrue();
        assertThat(CsvParsingUtils.isFailedStatus("failed")).isTrue();
        assertThat(CsvParsingUtils.isFailedStatus("Delivered")).isFalse();
        assertThat(CsvParsingUtils.isFailedStatus(null)).isFalse();
    }

    @Test
    void isDelayed_whenActualAfterPromised() {
        LocalDateTime promised = LocalDateTime.of(2025, 4, 26, 23, 54);
        LocalDateTime actual = LocalDateTime.of(2025, 4, 27, 1, 0);
        assertThat(CsvParsingUtils.isDelayed(actual, promised)).isTrue();
    }

    @Test
    void isDelayed_whenActualMissingOrOnTime() {
        LocalDateTime promised = LocalDateTime.of(2025, 4, 26, 23, 54);
        assertThat(CsvParsingUtils.isDelayed(null, promised)).isFalse();
        assertThat(CsvParsingUtils.isDelayed(promised, promised)).isFalse();
        assertThat(CsvParsingUtils.isDelayed(promised.minusHours(1), promised)).isFalse();
    }

    @Test
    void blankToNull_treatsEmptyAsNull() {
        assertThat(CsvParsingUtils.blankToNull(null)).isNull();
        assertThat(CsvParsingUtils.blankToNull("  ")).isNull();
        assertThat(CsvParsingUtils.blankToNull("value")).isEqualTo("value");
    }

    @Test
    void parseLong_handlesBlankAndValues() {
        assertThat(CsvParsingUtils.parseLong(null)).isNull();
        assertThat(CsvParsingUtils.parseLong("")).isNull();
        assertThat(CsvParsingUtils.parseLong("42")).isEqualTo(42L);
    }

    @Test
    void parseInteger_handlesBlankAndValues() {
        assertThat(CsvParsingUtils.parseInteger("  ")).isNull();
        assertThat(CsvParsingUtils.parseInteger("7")).isEqualTo(7);
    }

    @Test
    void parseDecimal_handlesBlankAndValues() {
        assertThat(CsvParsingUtils.parseDecimal(null)).isNull();
        assertThat(CsvParsingUtils.parseDecimal("199.50")).isEqualByComparingTo("199.50");
    }

    @Test
    void parseDateTime_parsesAndRejectsInvalid() {
        assertThat(CsvParsingUtils.parseDateTime("2025-03-17 10:15:00"))
                .isEqualTo(LocalDateTime.of(2025, 3, 17, 10, 15));
        assertThat(CsvParsingUtils.parseDateTime(null)).isNull();
        assertThatThrownBy(() -> CsvParsingUtils.parseDateTime("bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timestamp");
    }

    @Test
    void toInstant_convertsOrNull() {
        LocalDateTime dt = LocalDateTime.of(2025, 3, 17, 10, 0);
        assertThat(CsvParsingUtils.toInstant(dt)).isNotNull();
        assertThat(CsvParsingUtils.toInstant(null)).isNull();
    }
}
