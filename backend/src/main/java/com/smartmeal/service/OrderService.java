package com.smartmeal.service;

import com.smartmeal.dto.CreateOrderRequest;
import com.smartmeal.dto.OrderItemRequest;
import com.smartmeal.entity.FoodItem;
import com.smartmeal.entity.Order;
import com.smartmeal.entity.OrderItem;
import com.smartmeal.entity.PickupSlot;
import com.smartmeal.entity.User;
import com.smartmeal.exception.BusinessException;
import com.smartmeal.exception.ConflictException;
import com.smartmeal.exception.ResourceNotFoundException;
import com.smartmeal.repository.FoodItemRepository;
import com.smartmeal.repository.OrderRepository;
import com.smartmeal.repository.PickupSlotRepository;
import com.smartmeal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final PickupSlotRepository pickupSlotRepository;
    private final long cutoffMinutes;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        FoodItemRepository foodItemRepository,
                        PickupSlotRepository pickupSlotRepository,
                        @Value("${app.booking.cutoff-minutes:30}") long cutoffMinutes) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.foodItemRepository = foodItemRepository;
        this.pickupSlotRepository = pickupSlotRepository;
        this.cutoffMinutes = cutoffMinutes;
    }

    @Transactional
    public Order createOrder(String email, CreateOrderRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String mealCategory = normalizeMeal(request.getMealCategory());
        PickupSlot slot = getBookableSlot(request.getPickupSlotId(), mealCategory, request.getBookingDate());
        validateCapacity(request.getBookingDate(), slot, request.getItems(), null);

        Order order = new Order();
        order.setUser(user);
        order.setMealCategory(mealCategory);
        order.setBookingDate(request.getBookingDate());
        order.setPickupSlot(slot);
        order.setStatus("PLACED");
        order.setPaymentStatus("PENDING");

        addItems(order, request.getItems(), mealCategory);
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrder(Long id, String email, boolean isAdmin, CreateOrderRequest request) {
        Order order = getOwnedOrder(id, email, isAdmin);
        if (!"PLACED".equals(order.getStatus())) {
            throw new ConflictException("Only a placed booking can be updated.");
        }
        requireBeforeCutoff(order.getBookingDate(), order.getPickupSlot());

        String mealCategory = normalizeMeal(request.getMealCategory());
        PickupSlot slot = getBookableSlot(request.getPickupSlotId(), mealCategory, request.getBookingDate());
        validateCapacity(request.getBookingDate(), slot, request.getItems(), order.getId());

        order.setMealCategory(mealCategory);
        order.setBookingDate(request.getBookingDate());
        order.setPickupSlot(slot);
        order.getItems().clear();
        order.setTotalAmount(BigDecimal.ZERO);
        addItems(order, request.getItems(), mealCategory);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long id, String email, boolean isAdmin) {
        Order order = getOwnedOrder(id, email, isAdmin);
        requireBeforeCutoff(order.getBookingDate(), order.getPickupSlot());
        if (!"PLACED".equals(order.getStatus())) {
            throw new ConflictException("Only a placed booking can be cancelled.");
        }
        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> listOrdersForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Order> listAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public Order getOwnedOrder(Long id, String email, boolean isAdmin) {
        Order order = getOrderById(id);
        if (!isAdmin && !order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only access your own bookings.");
        }
        return order;
    }

    private void addItems(Order order, List<OrderItemRequest> itemRequests, String mealCategory) {
        Set<Long> foodIds = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            if (itemRequest.getFoodId() == null || itemRequest.getQuantity() == null || itemRequest.getQuantity() < 1) {
                throw new BusinessException("Each booking item needs a food item and a quantity of at least one.");
            }
            if (!foodIds.add(itemRequest.getFoodId())) {
                throw new BusinessException("A food item can only appear once in a booking.");
            }
            FoodItem foodItem = foodItemRepository.findById(itemRequest.getFoodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Food item not found: " + itemRequest.getFoodId()));
            if (!foodItem.isAvailable()) {
                throw new BusinessException("Food item is currently unavailable: " + foodItem.getName());
            }
            if (!mealCategory.equalsIgnoreCase(foodItem.getMealCategory())) {
                throw new BusinessException("All selected food items must match the selected meal category.");
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setFoodItem(foodItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(foodItem.getPrice());
            orderItem.setOrder(order);
            orderItems.add(orderItem);
            total = total.add(foodItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }
        if (orderItems.isEmpty()) {
            throw new BusinessException("A booking must contain at least one item.");
        }
        order.getItems().addAll(orderItems);
        order.setTotalAmount(total);
    }

    private PickupSlot getBookableSlot(Long slotId, String mealCategory, LocalDate bookingDate) {
        if (bookingDate == null || bookingDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Bookings must be for today or a future date.");
        }
        PickupSlot slot = pickupSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup slot not found: " + slotId));
        if (!slot.isActive() || !slot.getMealCategory().equalsIgnoreCase(mealCategory)) {
            throw new BusinessException("The selected pickup slot is not available for this meal.");
        }
        requireBeforeCutoff(bookingDate, slot);
        return slot;
    }

    private void validateCapacity(LocalDate bookingDate, PickupSlot slot, List<OrderItemRequest> requests, Long currentOrderId) {
        long requestedQuantity = requests.stream()
                .map(OrderItemRequest::getQuantity)
                .filter(quantity -> quantity != null && quantity > 0)
                .mapToLong(Integer::longValue)
                .sum();
        long bookedQuantity = orderRepository
                .findByBookingDateAndPickupSlot_IdAndStatusNot(bookingDate, slot.getId(), "CANCELLED")
                .stream()
                .filter(order -> currentOrderId == null || !order.getId().equals(currentOrderId))
                .flatMap(order -> order.getItems().stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();
        if (bookedQuantity + requestedQuantity > slot.getCapacity()) {
            throw new ConflictException("The selected pickup slot has insufficient remaining capacity.");
        }
    }

    private void requireBeforeCutoff(LocalDate bookingDate, PickupSlot slot) {
        if (bookingDate == null || slot == null) {
            throw new BusinessException("This booking has no valid pickup date or slot and cannot be changed.");
        }
        LocalDateTime cutoff = LocalDateTime.of(bookingDate, slot.getStartTime()).minusMinutes(cutoffMinutes);
        if (!LocalDateTime.now().isBefore(cutoff)) {
            throw new BusinessException("The booking cutoff has passed; changes and cancellation are no longer allowed.");
        }
    }

    private String normalizeMeal(String mealCategory) {
        if (mealCategory == null || mealCategory.isBlank()) {
            throw new BusinessException("Meal category is required.");
        }
        return mealCategory.trim().toUpperCase(Locale.ROOT);
    }
}
