package com.smartmeal.controller;

import com.smartmeal.dto.PickupSlotRequest;
import com.smartmeal.dto.PickupSlotResponse;
import com.smartmeal.service.PickupSlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PickupSlotController {
    private final PickupSlotService pickupSlotService;

    public PickupSlotController(PickupSlotService pickupSlotService) {
        this.pickupSlotService = pickupSlotService;
    }

    @GetMapping("/pickup-slots")
    public ResponseEntity<List<PickupSlotResponse>> availableSlots(
            @RequestParam(required = false) String mealCategory) {
        return ResponseEntity.ok(pickupSlotService.getAvailableSlots(mealCategory));
    }

    @GetMapping("/admin/pickup-slots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PickupSlotResponse>> allSlots() {
        return ResponseEntity.ok(pickupSlotService.getAllSlots());
    }

    @PostMapping("/admin/pickup-slots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PickupSlotResponse> create(@Valid @RequestBody PickupSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pickupSlotService.create(request));
    }

    @PutMapping("/admin/pickup-slots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PickupSlotResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PickupSlotRequest request) {
        return ResponseEntity.ok(pickupSlotService.update(id, request));
    }
}