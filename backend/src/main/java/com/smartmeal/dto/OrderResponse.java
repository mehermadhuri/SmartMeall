package com.smartmeal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String userEmail,
        String mealCategory,
        LocalDate bookingDate,
        Long pickupSlotId,
        String pickupSlotName,
        String pickupStartTime,
        String pickupEndTime,
        BigDecimal totalAmount,
        String paymentStatus,
        String status,
        LocalDateTime createdAt,
        List<OrderLineResponse> items) {
}