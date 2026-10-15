package com.smartmeal.service;

import com.smartmeal.dto.CreateFoodRequest;
import com.smartmeal.dto.FoodItemDto;
import com.smartmeal.entity.FoodItem;
import com.smartmeal.exception.ResourceNotFoundException;
import com.smartmeal.repository.FoodItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {
    private final FoodItemRepository foodItemRepository;

    public MenuService(FoodItemRepository foodItemRepository) {
        this.foodItemRepository = foodItemRepository;
    }

    public List<FoodItemDto> getAllMenuItems() {
        return foodItemRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public FoodItemDto getById(Long id) {
        return toDto(foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id " + id)));
    }

    public FoodItemDto createFood(CreateFoodRequest request) {
        FoodItem item = new FoodItem();
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        item.setImageUrl(request.getImageUrl());
        item.setMealCategory(request.getMealCategory());
        item.setAvailable(request.isAvailable());
        item.setPrepTimeMinutes(15);
        return toDto(foodItemRepository.save(item));
    }

    public FoodItemDto updateFood(Long id, CreateFoodRequest request) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id " + id));

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        item.setImageUrl(request.getImageUrl());
        item.setMealCategory(request.getMealCategory());
        item.setAvailable(request.isAvailable());
        return toDto(foodItemRepository.save(item));
    }

    private FoodItemDto toDto(FoodItem item) {
        return new FoodItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getCategory(),
                item.getImageUrl(),
                item.getMealCategory(),
                item.isAvailable(),
                item.getPrepTimeMinutes()
        );
    }
}
