package com.jwellkeeper.billing.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BillNumberServiceTest {

    private final BillNumberService service = new BillNumberService(null);

    @Test
    void formatsTenantGlobalSequenceWithPrefix() {
        assertThat(service.format("{PREFIX}-{SEQUENCE:000000}", "JK", 42))
                .isEqualTo("JK-000042");
    }

    @Test
    void supportsUnpaddedSequencePlaceholder() {
        assertThat(service.format("{PREFIX}/{SEQUENCE}", "SHOP", 7))
                .isEqualTo("SHOP/7");
    }
}
