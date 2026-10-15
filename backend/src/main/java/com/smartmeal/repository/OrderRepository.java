package com.smartmeal.repository;

import com.smartmeal.entity.Order;
import com.smartmeal.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Override
    @EntityGraph(attributePaths = {"items", "items.foodItem", "pickupSlot", "user"})
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"items", "items.foodItem", "pickupSlot", "user"})
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = {"items", "items.foodItem", "pickupSlot", "user"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"items", "items.foodItem", "pickupSlot", "user"})
    List<Order> findByBookingDateBetweenAndStatusNot(LocalDate from, LocalDate to, String status);

    @EntityGraph(attributePaths = {"items", "items.foodItem", "pickupSlot", "user"})
    List<Order> findByBookingDateAndPickupSlot_IdAndStatusNot(LocalDate bookingDate, Long slotId, String status);

    long countByStatus(String status);
}
