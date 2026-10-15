package com.smartmeal.config;

import com.smartmeal.entity.FoodItem;
import com.smartmeal.entity.Role;
import com.smartmeal.entity.User;
import com.smartmeal.entity.PickupSlot;
import com.smartmeal.repository.FoodItemRepository;
import com.smartmeal.repository.PickupSlotRepository;
import com.smartmeal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DataSeeder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Bean
    public CommandLineRunner seedData(FoodItemRepository foodItemRepository,
                                     PickupSlotRepository pickupSlotRepository,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminEmail.isBlank() != adminPassword.isBlank()) {
                throw new IllegalStateException("Configure both ADMIN_EMAIL and ADMIN_PASSWORD to seed an administrator.");
            }
            if (!adminEmail.isBlank() && adminPassword.length() < 12) {
                throw new IllegalStateException("ADMIN_PASSWORD must contain at least 12 characters.");
            }
            String normalizedAdminEmail = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);
            if (!normalizedAdminEmail.isBlank() && userRepository.findByEmail(normalizedAdminEmail).isEmpty()) {
                User admin = new User("SmartMeal Admin", normalizedAdminEmail, passwordEncoder.encode(adminPassword), null, Role.ADMIN);
                userRepository.save(admin);
            } else if (adminEmail.isBlank()) {
                LOGGER.info("No initial administrator configured. Set ADMIN_EMAIL and ADMIN_PASSWORD through the deployment environment.");
            }

            if (pickupSlotRepository.count() == 0) {
                pickupSlotRepository.saveAll(List.of(
                        slot("Breakfast 1", "BREAKFAST", "08:00", "09:00", 100),
                        slot("Breakfast 2", "BREAKFAST", "09:00", "10:00", 100),
                        slot("Lunch 1", "LUNCH", "11:30", "12:30", 120),
                        slot("Lunch 2", "LUNCH", "12:30", "13:30", 120)));
            }

            if (foodItemRepository.count() == 0) {
                List<FoodItem> foods = List.of(
                        new FoodItem("Idly", "Soft steamed rice cakes served with sambar and coconut chutney.", new BigDecimal("35.00"), "Breakfast", "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80", "BREAKFAST", true),
                        new FoodItem("Poori", "Golden poori with potato masala and chutney.", new BigDecimal("45.00"), "Breakfast", "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=900&q=80", "BREAKFAST", true),
                        new FoodItem("Plain Dosa", "Crisp rice-and-lentil dosa with podi and sambar.", new BigDecimal("55.00"), "Breakfast", "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=900&q=80", "BREAKFAST", true),
                        new FoodItem("Masala Dosa", "Classic masala dosa packed with spiced potato filling.", new BigDecimal("70.00"), "Breakfast", "https://images.unsplash.com/photo-1665842404224-257b1bb95377?auto=format&fit=crop&w=900&q=80", "BREAKFAST", true),
                        new FoodItem("Vada", "Crispy medu vada with coconut chutney and sambar.", new BigDecimal("40.00"), "Breakfast", "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=900&q=80", "BREAKFAST", true),
                        new FoodItem("Upma", "Light semolina upma with vegetables and mustard tempering.", new BigDecimal("42.00"), "Breakfast", "https://images.unsplash.com/photo-1589302168068-964664d93dc0?auto=format&fit=crop&w=900&q=80", "BREAKFAST", true),
                        new FoodItem("Pongal", "Comforting ghee pongal with black pepper and cashews.", new BigDecimal("60.00"), "Breakfast", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=900&q=80", "BREAKFAST", true),
                        new FoodItem("Chapati", "Soft whole wheat chapati with vegetable curry.", new BigDecimal("52.00"), "Lunch", "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80", "LUNCH", true),
                        new FoodItem("Veg Meals", "Rice, sambar, poriyal, rasam, and curd served hot.", new BigDecimal("120.00"), "Lunch", "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=900&q=80", "LUNCH", true),
                        new FoodItem("Rice and Curry", "Steamed rice with spicy curry and vegetables.", new BigDecimal("95.00"), "Lunch", "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=900&q=80", "LUNCH", true),
                        new FoodItem("Curd Rice", "Cooling curd rice with mustard seasoning and curry leaves.", new BigDecimal("65.00"), "Lunch", "https://images.unsplash.com/photo-1546793665-c74683f339c1?auto=format&fit=crop&w=900&q=80", "LUNCH", true),
                        new FoodItem("Lemon Rice", "Tangy lemon rice with peanuts and curry leaves.", new BigDecimal("70.00"), "Lunch", "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=900&q=80", "LUNCH", true),
                        new FoodItem("Veg Biryani", "Rich veg biryani with vegetables, herbs, and raita.", new BigDecimal("145.00"), "Lunch", "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=900&q=80", "LUNCH", true)
                );
                foodItemRepository.saveAll(foods);
            }
        };
    }

    private static PickupSlot slot(String name, String meal, String start, String end, int capacity) {
        PickupSlot slot = new PickupSlot();
        slot.setName(name);
        slot.setMealCategory(meal);
        slot.setStartTime(LocalTime.parse(start));
        slot.setEndTime(LocalTime.parse(end));
        slot.setCapacity(capacity);
        slot.setActive(true);
        return slot;
    }
}
