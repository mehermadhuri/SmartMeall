package com.smartmeal.dto;

public record AdminSummaryResponse(
        long totalStudents,
        long bookingsToday,
        long activeBookings,
        String peakMealCategory) {
}
