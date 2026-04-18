package com.jwellkeeper.common.util;

import com.jwellkeeper.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyValidatorTest {

    @Test
    void normalizesValidCurrency() {
        assertThat(CurrencyValidator.requireIsoCurrency("cad")).isEqualTo("CAD");
    }

    @Test
    void rejectsInvalidCurrency() {
        assertThatThrownBy(() -> CurrencyValidator.requireIsoCurrency("XYZ"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ISO-4217");
    }
}
