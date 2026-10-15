package com.smartmeal.dto;

import java.time.LocalDate;

public record ForecastResponse(
        String mealCategory,
        LocalDate targetDate,
        double predictedQuantity,
        String method,
        int sampleCount,
        String explanation) {
}