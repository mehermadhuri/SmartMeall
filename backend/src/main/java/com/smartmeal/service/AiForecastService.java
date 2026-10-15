package com.smartmeal.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartmeal.dto.ForecastResponse;
import com.smartmeal.dto.HistoricalDemandPoint;
import com.smartmeal.entity.Order;
import com.smartmeal.entity.OrderItem;
import com.smartmeal.exception.BusinessException;
import com.smartmeal.exception.UpstreamServiceException;
import com.smartmeal.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AiForecastService {
    private final OrderRepository orderRepository;
    private final RestClient aiRestClient;

    public AiForecastService(OrderRepository orderRepository, RestClient aiRestClient) {
        this.orderRepository = orderRepository;
        this.aiRestClient = aiRestClient;
    }

    @Transactional(readOnly = true)
    public ForecastResponse forecast(String mealCategory, LocalDate targetDate) {
        if (mealCategory == null || mealCategory.isBlank() || targetDate == null) {
            throw new BusinessException("Meal category and forecast date are required.");
        }
        String meal = mealCategory.trim().toUpperCase(Locale.ROOT);
        if (!meal.equals("BREAKFAST") && !meal.equals("LUNCH")) {
            throw new BusinessException("Forecast meal category must be BREAKFAST or LUNCH.");
        }

        LocalDate from = targetDate.minusDays(90);
        LocalDate to = targetDate.minusDays(1);
        Map<LocalDate, Long> byDate = new TreeMap<>();
        if (!to.isBefore(from)) {
            for (Order order : orderRepository.findByBookingDateBetweenAndStatusNot(from, to, "CANCELLED")) {
                if (meal.equalsIgnoreCase(order.getMealCategory())) {
                    long quantity = order.getItems().stream().mapToLong(OrderItem::getQuantity).sum();
                    byDate.merge(order.getBookingDate(), quantity, Long::sum);
                }
            }
        }

        var historicalData = byDate.entrySet().stream()
                .map(entry -> new HistoricalDemandPoint(entry.getKey(), entry.getValue()))
                .toList();
        try {
            ForecastResponse result = aiRestClient.post()
                    .uri("/forecast")
                    .body(new ForecastRequest(meal, targetDate, historicalData))
                    .retrieve()
                    .body(ForecastResponse.class);
            if (result == null) {
                throw new UpstreamServiceException("AI forecast service returned an empty response.", null);
            }
            return result;
        } catch (RestClientException ex) {
            throw new UpstreamServiceException("AI forecast service is currently unavailable.", ex);
        }
    }

    private record ForecastRequest(
            @JsonProperty("meal_category") String mealCategory,
            @JsonProperty("target_date") LocalDate targetDate,
            @JsonProperty("historical_data") java.util.List<HistoricalDemandPoint> historicalData) {
    }
}