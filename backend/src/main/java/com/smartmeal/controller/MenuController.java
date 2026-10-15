package com.smartmeal.controller;

import com.smartmeal.dto.CreateFoodRequest;
import com.smartmeal.dto.FoodItemDto;
import com.smartmeal.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menu")
    public ResponseEntity<List<FoodItemDto>> getMenu() {
        return ResponseEntity.ok(menuService.getAllMenuItems());
    }

    @GetMapping("/menu/{id}")
    public ResponseEntity<FoodItemDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getById(id));
    }

    @PostMapping("/admin/menu")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FoodItemDto> createFood(@Valid @RequestBody CreateFoodRequest request) {
        return ResponseEntity.ok(menuService.createFood(request));
    }

    @PutMapping("/admin/menu/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FoodItemDto> updateFood(@PathVariable Long id, @Valid @RequestBody CreateFoodRequest request) {
        return ResponseEntity.ok(menuService.updateFood(id, request));
    }
}
