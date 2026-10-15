package com.smartmeal.controller;

import com.smartmeal.dto.AdminSummaryResponse;
import com.smartmeal.dto.DemandSummaryResponse;
import com.smartmeal.dto.ForecastResponse;
import com.smartmeal.service.AdminService;
import com.smartmeal.service.AiForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final AiForecastService aiForecastService;

    public AdminController(AdminService adminService, AiForecastService aiForecastService) {
        this.adminService = adminService;
        this.aiForecastService = aiForecastService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> summary() {
        return ResponseEntity.ok(adminService.summary());
    }

    @GetMapping("/demand")
    public ResponseEntity<List<DemandSummaryResponse>> demand(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(adminService.demand(from, to));
    }

    @GetMapping("/forecast")
    public ResponseEntity<ForecastResponse> forecast(
            @RequestParam String mealCategory,
            @RequestParam(required = false) LocalDate targetDate) {
        LocalDate date = targetDate == null ? LocalDate.now().plusDays(1) : targetDate;
        return ResponseEntity.ok(aiForecastService.forecast(mealCategory, date));
    }
}
