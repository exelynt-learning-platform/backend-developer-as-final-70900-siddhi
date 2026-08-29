package com.example.booking;

import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.enums.Role;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private Resource testResource;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("user@test.com");
        testUser.setRole(Role.USER);

        testResource = new Resource();
        testResource.setId(1L);
        testResource.setName("Room 101");
        testResource.setType("room");

        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setResource(testResource);
        testReservation.setStartTime(LocalDateTime.now().plusHours(1));
        testReservation.setEndTime(LocalDateTime.now().plusHours(3));
        testReservation.setPrice(new BigDecimal("500.00"));
        testReservation.setStatus(ReservationStatus.PENDING);
    }

    // ─── Create ────────────────────────────────────────────────────────────────

    @Test
    void create_shouldReturnResponse_whenValidRequest() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.save(any())).thenReturn(testReservation);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(3));
        request.setPrice(new BigDecimal("500.00"));

        ReservationResponse response = reservationService.create(request, "user@test.com");

        assertNotNull(response);
        assertEquals("user@test.com", response.getUserEmail());
        assertEquals("Room 101", response.getResourceName());
        assertEquals(ReservationStatus.PENDING, response.getStatus());
    }

    @Test
    void create_shouldThrowException_whenEndTimeBeforeStartTime() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(3));
        request.setEndTime(LocalDateTime.now().plusHours(1)); // end < start
        request.setPrice(new BigDecimal("100.00"));

        assertThrows(RuntimeException.class,
                () -> reservationService.create(request, "user@test.com"));
    }

    @Test
    void create_shouldThrowException_whenResourceNotFound() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(99L);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(3));
        request.setPrice(new BigDecimal("100.00"));

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.create(request, "user@test.com"));
    }

    // ─── Get My Reservations ───────────────────────────────────────────────────

    @Test
    void getMyReservations_shouldReturnOnlyOwnReservations() {
        Page<Reservation> page = new PageImpl<>(List.of(testReservation));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), isNull(), any()))
                .thenReturn(page);

        Page<ReservationResponse> result = reservationService.getMyReservations(
                "user@test.com", null, null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("user@test.com", result.getContent().get(0).getUserEmail());
    }

    // ─── Update Status ─────────────────────────────────────────────────────────

    @Test
    void updateStatus_shouldChangeStatus_whenFound() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        testReservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.save(any())).thenReturn(testReservation);

        ReservationResponse response = reservationService.updateStatus(1L, ReservationStatus.CONFIRMED);

        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void updateStatus_shouldThrow404_whenNotFound() {
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.updateStatus(99L, ReservationStatus.CANCELLED));
    }

    // ─── Delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldSucceed_whenExists() {
        when(reservationRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> reservationService.delete(1L));
        verify(reservationRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrow404_whenNotFound() {
        when(reservationRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> reservationService.delete(99L));
        verify(reservationRepository, never()).deleteById(any());
    }
}
