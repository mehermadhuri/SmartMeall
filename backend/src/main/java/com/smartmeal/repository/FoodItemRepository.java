package com.smartmeal.repository;

import com.smartmeal.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findByAvailableTrue();
    List<FoodItem> findByCategoryIgnoreCase(String category);
    List<FoodItem> findByMealCategoryIgnoreCase(String mealCategory);
}
