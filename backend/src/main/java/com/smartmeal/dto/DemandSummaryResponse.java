package com.smartmeal.dto;

import java.time.LocalDate;

public record DemandSummaryResponse(
        LocalDate bookingDate,
        String mealCategory,
        Long pickupSlotId,
        String pickupSlotName,
        long totalQuantity,
        long bookingCount) {
}