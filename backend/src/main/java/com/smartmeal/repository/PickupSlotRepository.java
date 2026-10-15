package com.smartmeal.repository;

import com.smartmeal.entity.PickupSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickupSlotRepository extends JpaRepository<PickupSlot, Long> {
    List<PickupSlot> findByActiveTrueOrderByMealCategoryAscStartTimeAsc();
    List<PickupSlot> findByActiveTrueAndMealCategoryIgnoreCaseOrderByStartTimeAsc(String mealCategory);
}