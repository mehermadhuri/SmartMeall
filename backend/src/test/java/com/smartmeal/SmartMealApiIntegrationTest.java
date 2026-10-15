package com.smartmeal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmeal.entity.FoodItem;
import com.smartmeal.entity.Order;
import com.smartmeal.entity.OrderItem;
import com.smartmeal.entity.PickupSlot;
import com.smartmeal.entity.Role;
import com.smartmeal.entity.User;
import com.smartmeal.repository.FoodItemRepository;
import com.smartmeal.repository.OrderRepository;
import com.smartmeal.repository.PickupSlotRepository;
import com.smartmeal.repository.UserRepository;
import com.smartmeal.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmartMealApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private FoodItemRepository foodItemRepository;
    @Autowired private PickupSlotRepository pickupSlotRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationHashesPasswordAndLoginReturnsStudentToken() throws Exception {
        String registration = """
                {"fullName":"Test Student","email":"STUDENT@example.test","studentId":"S-1",
                 "password":"strong-pass-1","confirmPassword":"strong-pass-1"}
                """;
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("STUDENT")))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String email = json.get("email").asText();
        String token = json.get("token").asText();
        User stored = userRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("strong-pass-1", stored.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("strong-pass-1", stored.getPassword()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.test\",\"password\":\"strong-pass-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
        mockMvc.perform(get("/api/orders/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void menuIsPublicButMenuManagementRequiresAdmin() throws Exception {
        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk());

        String studentToken = registerStudent("menu-student@example.test");
        mockMvc.perform(post("/api/admin/menu")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodRequest()))
                .andExpect(status().isForbidden());

        String adminToken = createAdminToken("menu-admin@example.test");
        mockMvc.perform(post("/api/admin/menu")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Meal")));
    }

    @Test
    void pickupSlotManagementRequiresAdmin() throws Exception {
        String request = """
                {"name":"Test Slot","mealCategory":"BREAKFAST","startTime":"10:00",
                 "endTime":"11:00","capacity":25,"active":true}
                """;

        mockMvc.perform(post("/api/admin/pickup-slots")
                        .header("Authorization", "Bearer " + registerStudent("slot-student@example.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/pickup-slots")
                        .header("Authorization", "Bearer " + createAdminToken("slot-admin@example.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Slot")));
    }

    @Test
    void studentCanCreateSeeAndCancelOwnBooking() throws Exception {
        String token = registerStudent("booking-student@example.test");
        FoodItem meal = foodItemRepository.findByMealCategoryIgnoreCase("BREAKFAST").get(0);
        PickupSlot slot = pickupSlotRepository.findByActiveTrueAndMealCategoryIgnoreCaseOrderByStartTimeAsc("BREAKFAST").get(0);
        String payload = bookingRequest(meal.getId(), slot.getId(), LocalDate.now().plusDays(1));

        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PLACED")))
                .andExpect(jsonPath("$.paymentStatus", is("PENDING")))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/orders/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is((int) orderId)));
        mockMvc.perform(delete("/api/orders/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
        mockMvc.perform(put("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void studentCanUpdateAPlacedBookingBeforeTheCutoff() throws Exception {
        String token = registerStudent("update-student@example.test");
        FoodItem meal = foodItemRepository.findByMealCategoryIgnoreCase("BREAKFAST").get(0);
        PickupSlot slot = pickupSlotRepository.findByActiveTrueAndMealCategoryIgnoreCaseOrderByStartTimeAsc("BREAKFAST").get(0);
        LocalDate initialDate = LocalDate.now().plusDays(1);
        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(meal.getId(), slot.getId(), initialDate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(response).get("id").asLong();
        LocalDate updatedDate = initialDate.plusDays(1);
        String updatedPayload = "{\"mealCategory\":\"BREAKFAST\",\"bookingDate\":\"" + updatedDate
                + "\",\"pickupSlotId\":" + slot.getId()
                + ",\"items\":[{\"foodId\":" + meal.getId() + ",\"quantity\":3}]}";

        mockMvc.perform(put("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingDate", is(updatedDate.toString())))
                .andExpect(jsonPath("$.items[0].quantity", is(3)));
    }

    @Test
    void studentsCannotReadAnotherStudentsBooking() throws Exception {
        String ownerToken = registerStudent("owner@example.test");
        String otherToken = registerStudent("other@example.test");
        FoodItem meal = foodItemRepository.findByMealCategoryIgnoreCase("BREAKFAST").get(0);
        PickupSlot slot = pickupSlotRepository.findByActiveTrueAndMealCategoryIgnoreCaseOrderByStartTimeAsc("BREAKFAST").get(0);
        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(meal.getId(), slot.getId(), LocalDate.now().plusDays(1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredBookingCannotBeCancelled() throws Exception {
        String token = registerStudent("late-cancel@example.test");
        User owner = userRepository.findByEmail("late-cancel@example.test").orElseThrow();
        FoodItem meal = foodItemRepository.findByMealCategoryIgnoreCase("BREAKFAST").get(0);
        PickupSlot slot = pickupSlotRepository.findByActiveTrueAndMealCategoryIgnoreCaseOrderByStartTimeAsc("BREAKFAST").get(0);
        Order order = new Order();
        order.setUser(owner);
        order.setMealCategory("BREAKFAST");
        order.setBookingDate(LocalDate.now().minusDays(1));
        order.setPickupSlot(slot);
        order.setStatus("PLACED");
        order.setPaymentStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now().minusDays(2));
        OrderItem line = new OrderItem();
        line.setFoodItem(meal);
        line.setQuantity(1);
        line.setUnitPrice(meal.getPrice());
        line.setOrder(order);
        order.getItems().add(line);
        order.setTotalAmount(meal.getPrice());
        Order saved = orderRepository.save(order);

        mockMvc.perform(delete("/api/orders/" + saved.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void demandEndpointIsAdminOnlyAndGroupsRealBookingQuantities() throws Exception {
        String studentToken = registerStudent("demand-student@example.test");
        FoodItem meal = foodItemRepository.findByMealCategoryIgnoreCase("BREAKFAST").get(0);
        PickupSlot slot = pickupSlotRepository.findByActiveTrueAndMealCategoryIgnoreCaseOrderByStartTimeAsc("BREAKFAST").get(0);
        LocalDate day = LocalDate.now().plusDays(1);
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(meal.getId(), slot.getId(), day)))
                .andExpect(status().isCreated());

        String query = "?from=" + day + "&to=" + day;
        mockMvc.perform(get("/api/admin/demand" + query).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/demand" + query)
                        .header("Authorization", "Bearer " + createAdminToken("demand-admin@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalQuantity", is(2)));
    }

    private String registerStudent(String email) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "fullName", "Test Student",
                "email", email,
                "studentId", "ST-" + email.hashCode(),
                "password", "strong-pass-1",
                "confirmPassword", "strong-pass-1"));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String createAdminToken(String email) {
        User admin = new User("Test Admin", email, passwordEncoder.encode("admin-test-password"), null, Role.ADMIN);
        userRepository.saveAndFlush(admin);
        return jwtService.generateToken(email, "ADMIN");
    }

    private String bookingRequest(Long foodId, Long slotId, LocalDate date) {
        return "{\"mealCategory\":\"BREAKFAST\",\"bookingDate\":\"" + date
                + "\",\"pickupSlotId\":" + slotId
                + ",\"items\":[{\"foodId\":" + foodId + ",\"quantity\":2}]}";
    }

    private String foodRequest() {
        return """
                {"name":"Test Meal","description":"A test meal","price":25.00,"category":"Breakfast",
                 "imageUrl":"https://example.test/meal.png","mealCategory":"BREAKFAST","available":true}
                """;
    }
}