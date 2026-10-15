package com.smartmeal.dto;

import java.time.LocalDate;

public record HistoricalDemandPoint(LocalDate date, long quantity) {
}