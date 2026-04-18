package com.jwellkeeper.shop.dto;

import java.time.LocalDate;

public record ShopStateChangeRequest(
        LocalDate businessDate
) {
}
