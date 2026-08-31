package com.multigenesys.booking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multigenesys.booking.dto.request.LoginRequest;
import com.multigenesys.booking.dto.request.ReservationRequest;
import com.multigenesys.booking.dto.request.ReservationStatusUpdateRequest;
import com.multigenesys.booking.dto.request.ResourceRequest;
import com.multigenesys.booking.entity.ReservationStatus;
import com.multigenesys.booking.entity.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceAndReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainAccessToken("admin@example.com", "Admin@123");
        user1Token = obtainAccessToken("user1@example.com", "User@123");
        user2Token = obtainAccessToken("user2@example.com", "User@123");
    }

    private String obtainAccessToken(String usernameOrEmail, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(usernameOrEmail, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return "Bearer " + responseNode.get("token").asText();
    }

    @Test
    @DisplayName("RBAC: Regular USER cannot create a resource (403 Forbidden)")
    void testUserCannotCreateResource() throws Exception {
        ResourceRequest resourceRequest = ResourceRequest.builder()
                .name("Unauthorized Resource")
                .description("Created by user")
                .type(ResourceType.EQUIPMENT)
                .pricePerHour(new BigDecimal("25.00"))
                .isAvailable(true)
                .build();

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: ADMIN can create resource, USER can read resource")
    void testAdminCreatesResourceAndUserReads() throws Exception {
        ResourceRequest resourceRequest = ResourceRequest.builder()
                .name("Podcast Studio Room")
                .description("Soundproof audio recording studio")
                .type(ResourceType.ROOM)
                .pricePerHour(new BigDecimal("75.00"))
                .isAvailable(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/resources")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Podcast Studio Room")))
                .andReturn();

        JsonNode resourceNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long resourceId = resourceNode.get("id").asLong();

        // User can view the newly created resource
        mockMvc.perform(get("/api/resources/" + resourceId)
                        .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Podcast Studio Room")));
    }

    @Test
    @DisplayName("Full Reservation Lifecycle: Creation, Overlap Detection, Ownership Isolation & Dynamic Filtering")
    void testReservationWorkflowAndOwnershipIsolation() throws Exception {
        // 1. Admin creates a bookable resource
        ResourceRequest resourceRequest = ResourceRequest.builder()
                .name("Boardroom Alpha")
                .description("Executive meeting room")
                .type(ResourceType.ROOM)
                .pricePerHour(new BigDecimal("100.00"))
                .isAvailable(true)
                .build();

        MvcResult resourceResult = mockMvc.perform(post("/api/resources")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long resourceId = objectMapper.readTree(resourceResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. User 1 books a 2-hour reservation (Total = 2 * $100 = $200.00)
        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest reservationRequest = ReservationRequest.builder()
                .resourceId(resourceId)
                .startTime(start)
                .endTime(end)
                .build();

        MvcResult reservationResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username", is("user1")))
                .andExpect(jsonPath("$.totalPrice", is(200.0)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn();

        long reservationId = objectMapper.readTree(reservationResult.getResponse().getContentAsString()).get("id").asLong();

        // 3. User 2 attempts to book overlapping time slot on the same resource -> 409 Conflict
        ReservationRequest overlapRequest = ReservationRequest.builder()
                .resourceId(resourceId)
                .startTime(start.plusMinutes(30))
                .endTime(end.plusMinutes(30))
                .build();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlapRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));

        // 4. Ownership check: User 2 attempts to view User 1's reservation -> 403 Forbidden
        mockMvc.perform(get("/api/reservations/" + reservationId)
                        .header("Authorization", user2Token))
                .andExpect(status().isForbidden());

        // 5. Admin views User 1's reservation -> 200 OK
        mockMvc.perform(get("/api/reservations/" + reservationId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) reservationId)))
                .andExpect(jsonPath("$.username", is("user1")));

        // 6. Dynamic Filtering & Pagination: User 1 filters with status, minPrice, maxPrice
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", user1Token)
                        .param("status", "PENDING")
                        .param("minPrice", "150.00")
                        .param("maxPrice", "250.00")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "totalPrice")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].id", is((int) reservationId)))
                .andExpect(jsonPath("$.content[0].username", is("user1")));

        // 7. Status Update: User 1 cancels their own reservation
        ReservationStatusUpdateRequest cancelReq = new ReservationStatusUpdateRequest(ReservationStatus.CANCELLED);
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/status")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("Edge Case: Invalid sortBy field in resources endpoint should return 400 Bad Request")
    void testInvalidSortByInResourcesReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", user1Token)
                        .param("sortBy", "nonExistentField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid sortBy field")));
    }

    @Test
    @DisplayName("Edge Case: Invalid sortBy field in reservations endpoint should return 400 Bad Request")
    void testInvalidSortByInReservationsReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", user1Token)
                        .param("sortBy", "invalidSortColumn"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid sortBy field")));
    }

    @Test
    @DisplayName("Edge Case: Invalid sortDirection in reservations endpoint should return 400 Bad Request")
    void testInvalidSortDirectionReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", user1Token)
                        .param("sortDirection", "INVALID_DIR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid sortDirection")));
    }
}
