package com.smartmeal.dto;

import java.math.BigDecimal;

public record OrderLineResponse(
        Long foodId,
        String name,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}