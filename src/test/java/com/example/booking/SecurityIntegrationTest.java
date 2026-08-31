package com.example.booking;

import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    @MockitoBean private ReservationRepository reservationRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setEmail("admin@booking.com");
        admin.setPassword("encoded");
        admin.setRole(Role.ADMIN);

        User user = new User();
        user.setId(2L);
        user.setName("User");
        user.setEmail("user@booking.com");
        user.setPassword("encoded");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("admin@booking.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("user@booking.com")).thenReturn(Optional.of(user));

        adminToken = "Bearer " + jwtUtil.generateToken("admin@booking.com", "ADMIN");
        userToken = "Bearer " + jwtUtil.generateToken("user@booking.com", "USER");
    }

    // ─── Unauthenticated — should 401 ─────────────────────────────────────────

    @Test
    void getResources_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllReservations_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isUnauthorized());
    }

    // ─── USER role — forbidden on ADMIN endpoints ──────────────────────────────

    @Test
    void createResource_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(post("/resources")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Room\",\"type\":\"room\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateResource_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(put("/resources/1")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"type\":\"room\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteResource_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/resources/1")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllReservations_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/reservations")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReservationStatus_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/reservations/1/status")
                        .header("Authorization", userToken)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReservation_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/reservations/1")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReservation_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(put("/reservations/1")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":1,\"startTime\":\"2026-09-10T10:00:00\"," +
                                 "\"endTime\":\"2026-09-10T12:00:00\",\"price\":500.00}"))
                .andExpect(status().isForbidden());
    }

    // ─── USER role — allowed endpoints ────────────────────────────────────────

    @Test
    void getAllResources_asUser_shouldReturn200() throws Exception {
        when(resourceRepository.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/resources")
                        .header("Authorization", userToken))
                .andExpect(status().isOk());
    }

    // ─── ADMIN role — full access ──────────────────────────────────────────────

    @Test
    void createResource_asAdmin_shouldReturn201() throws Exception {
        Resource saved = new Resource();
        saved.setId(1L);
        saved.setName("Room A");
        saved.setType("room");
        when(resourceRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/resources")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Room A\",\"type\":\"room\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteReservation_asAdmin_shouldReturn404_whenNotFound() throws Exception {
        when(reservationRepository.existsById(anyLong())).thenReturn(false);
        mockMvc.perform(delete("/reservations/999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getResourceById_asAdmin_shouldReturn404_whenNotFound() throws Exception {
        when(resourceRepository.findById(anyLong())).thenReturn(Optional.empty());
        mockMvc.perform(get("/resources/999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    // ─── Seed user login & register test ───────────────────────────────────────

    @Test
    void login_withInvalidCredentials_shouldReturn400() throws Exception {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad@email.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withValidData_shouldReturn200() throws Exception {
        when(userRepository.findByEmail("newuser@test.com")).thenReturn(Optional.empty());

        User saved = new User();
        saved.setId(1L);
        saved.setEmail("newuser@test.com");
        saved.setRole(Role.USER);
        when(userRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New User\",\"email\":\"newuser@test.com\"," +
                                 "\"password\":\"password123\",\"role\":\"USER\"}"))
                .andExpect(status().isOk());
    }
}
