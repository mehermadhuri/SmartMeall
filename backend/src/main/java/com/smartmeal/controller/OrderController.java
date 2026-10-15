package com.smartmeal.controller;

import com.smartmeal.dto.CreateOrderRequest;
import com.smartmeal.dto.OrderLineResponse;
import com.smartmeal.dto.OrderResponse;
import com.smartmeal.entity.Order;
import com.smartmeal.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(orderService.createOrder(authentication.getName(), request)));
    }

    @GetMapping("/orders/my")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> myOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.listOrdersForUser(authentication.getName()).stream().map(OrderController::toResponse).toList());
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> orderById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(toResponse(orderService.getOwnedOrder(id, authentication.getName(), isAdmin(authentication))));
    }

    @PutMapping("/orders/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable Long id,
            @RequestBody @Valid CreateOrderRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(toResponse(orderService.updateOrder(id, authentication.getName(), isAdmin(authentication), request)));
    }

    @DeleteMapping("/orders/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(toResponse(orderService.cancelOrder(id, authentication.getName(), isAdmin(authentication))));
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> allOrders() {
        return ResponseEntity.ok(orderService.listAllOrders().stream().map(OrderController::toResponse).toList());
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private static OrderResponse toResponse(Order order) {
        var slot = order.getPickupSlot();
        List<OrderLineResponse> items = order.getItems().stream()
                .map(item -> new OrderLineResponse(
                        item.getFoodItem().getId(),
                        item.getFoodItem().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()))))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getUser().getEmail(),
                order.getMealCategory(),
                order.getBookingDate(),
                slot == null ? null : slot.getId(),
                slot == null ? null : slot.getName(),
                slot == null ? null : slot.getStartTime().toString(),
                slot == null ? null : slot.getEndTime().toString(),
                order.getTotalAmount(),
                order.getPaymentStatus(),
                order.getStatus(),
                order.getCreatedAt(),
                items);
    }
}
