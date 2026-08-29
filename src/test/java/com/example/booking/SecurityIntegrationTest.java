package com.example.booking;

import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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

    // Mock repos so no real DB is needed
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    @MockitoBean private ReservationRepository reservationRepository;

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
    @WithMockUser(roles = "USER")
    void createResource_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Room\",\"type\":\"room\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateResource_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(put("/resources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"type\":\"room\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteResource_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/resources/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllReservations_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateReservationStatus_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/reservations/1/status")
                        .param("status", "CONFIRMED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteReservation_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/reservations/1"))
                .andExpect(status().isForbidden());
    }

    // ─── USER role — allowed endpoints ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void getAllResources_asUser_shouldReturn200() throws Exception {
        when(resourceRepository.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/resources"))
                .andExpect(status().isOk());
    }

    // ─── ADMIN role — full access ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createResource_asAdmin_shouldReturn201() throws Exception {
        Resource saved = new Resource();
        saved.setId(1L);
        saved.setName("Room A");
        saved.setType("room");
        when(resourceRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Room A\",\"type\":\"room\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReservation_asAdmin_shouldReturn404_whenNotFound() throws Exception {
        when(reservationRepository.existsById(anyLong())).thenReturn(false);
        mockMvc.perform(delete("/reservations/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getResourceById_asAdmin_shouldReturn404_whenNotFound() throws Exception {
        when(resourceRepository.findById(anyLong())).thenReturn(Optional.empty());
        mockMvc.perform(get("/resources/999"))
                .andExpect(status().isNotFound());
    }

    // ─── Seed user login test ──────────────────────────────────────────────────

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
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

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
