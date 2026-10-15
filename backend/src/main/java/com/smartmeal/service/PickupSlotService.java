package com.smartmeal.service;

import com.smartmeal.dto.PickupSlotRequest;
import com.smartmeal.dto.PickupSlotResponse;
import com.smartmeal.entity.PickupSlot;
import com.smartmeal.exception.BusinessException;
import com.smartmeal.exception.ResourceNotFoundException;
import com.smartmeal.repository.PickupSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PickupSlotService {
    private final PickupSlotRepository pickupSlotRepository;

    public PickupSlotService(PickupSlotRepository pickupSlotRepository) {
        this.pickupSlotRepository = pickupSlotRepository;
    }

    @Transactional(readOnly = true)
    public List<PickupSlotResponse> getAvailableSlots(String mealCategory) {
        List<PickupSlot> slots = mealCategory == null || mealCategory.isBlank()
                ? pickupSlotRepository.findByActiveTrueOrderByMealCategoryAscStartTimeAsc()
                : pickupSlotRepository.findByActiveTrueAndMealCategoryIgnoreCaseOrderByStartTimeAsc(mealCategory.trim());
        return slots.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PickupSlotResponse> getAllSlots() {
        return pickupSlotRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public PickupSlotResponse create(PickupSlotRequest request) {
        PickupSlot slot = new PickupSlot();
        apply(slot, request);
        return toResponse(pickupSlotRepository.save(slot));
    }

    @Transactional
    public PickupSlotResponse update(Long id, PickupSlotRequest request) {
        PickupSlot slot = pickupSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup slot not found: " + id));
        apply(slot, request);
        return toResponse(pickupSlotRepository.save(slot));
    }

    private void apply(PickupSlot slot, PickupSlotRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("Pickup slot end time must be later than its start time.");
        }
        slot.setName(request.getName().trim());
        slot.setMealCategory(request.getMealCategory().trim().toUpperCase(Locale.ROOT));
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setCapacity(request.getCapacity());
        slot.setActive(request.isActive());
    }

    private PickupSlotResponse toResponse(PickupSlot slot) {
        return new PickupSlotResponse(
                slot.getId(),
                slot.getName(),
                slot.getMealCategory(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getCapacity(),
                slot.isActive());
    }
}