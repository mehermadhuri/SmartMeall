package com.smartmeal.dto;

import java.time.LocalTime;

public record PickupSlotResponse(
        Long id,
        String name,
        String mealCategory,
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        boolean active) {
}