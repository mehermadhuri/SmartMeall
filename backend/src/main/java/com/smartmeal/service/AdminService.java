package com.smartmeal.service;

import com.smartmeal.dto.AdminSummaryResponse;
import com.smartmeal.dto.DemandSummaryResponse;
import com.smartmeal.entity.Order;
import com.smartmeal.entity.OrderItem;
import com.smartmeal.entity.Role;
import com.smartmeal.repository.OrderRepository;
import com.smartmeal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public AdminService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        List<Order> todayOrders = orderRepository.findByBookingDateBetweenAndStatusNot(today, today, "CANCELLED");
        Map<String, Long> quantityByMeal = new HashMap<>();
        long activeBookingCount = orderRepository.countByStatus("PLACED");
        for (Order order : todayOrders) {
            long quantity = order.getItems().stream().mapToLong(OrderItem::getQuantity).sum();
            quantityByMeal.merge(order.getMealCategory(), quantity, Long::sum);
        }
        String peakMeal = quantityByMeal.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NONE");
        return new AdminSummaryResponse(
                userRepository.countByRole(Role.STUDENT),
                todayOrders.size(),
                activeBookingCount,
                peakMeal);
    }

    @Transactional(readOnly = true)
    public List<DemandSummaryResponse> demand(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || from.isBefore(to.minusDays(366))) {
            throw new com.smartmeal.exception.BusinessException("Demand dates must be valid and span no more than 367 days.");
        }
        Map<DemandKey, DemandTotals> grouped = new HashMap<>();
        for (Order order : orderRepository.findByBookingDateBetweenAndStatusNot(from, to, "CANCELLED")) {
            var slot = order.getPickupSlot();
            DemandKey key = new DemandKey(
                    order.getBookingDate(),
                    order.getMealCategory(),
                    slot == null ? null : slot.getId(),
                    slot == null ? "Unassigned" : slot.getName());
            DemandTotals totals = grouped.computeIfAbsent(key, ignored -> new DemandTotals());
            totals.bookings++;
            totals.quantity += order.getItems().stream().mapToLong(OrderItem::getQuantity).sum();
        }
        return grouped.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<DemandKey, DemandTotals> entry) -> entry.getKey().date())
                        .thenComparing(entry -> entry.getKey().meal())
                        .thenComparing(entry -> entry.getKey().slotName()))
                .map(entry -> new DemandSummaryResponse(
                        entry.getKey().date(),
                        entry.getKey().meal(),
                        entry.getKey().slotId(),
                        entry.getKey().slotName(),
                        entry.getValue().quantity,
                        entry.getValue().bookings))
                .toList();
    }

    private record DemandKey(LocalDate date, String meal, Long slotId, String slotName) {
    }

    private static final class DemandTotals {
        private long quantity;
        private long bookings;
    }
}
