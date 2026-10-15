package com.smartmeal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class CreateOrderRequest {
    @NotBlank
    private String mealCategory;

    @NotNull
    private LocalDate bookingDate;

    @NotNull
    private Long pickupSlotId;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    public String getMealCategory() { return mealCategory; }
    public void setMealCategory(String mealCategory) { this.mealCategory = mealCategory; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public Long getPickupSlotId() { return pickupSlotId; }
    public void setPickupSlotId(Long pickupSlotId) { this.pickupSlotId = pickupSlotId; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
