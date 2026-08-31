package com.multigenesys.booking.service;

import com.multigenesys.booking.dto.request.ReservationRequest;
import com.multigenesys.booking.dto.response.ReservationResponse;
import com.multigenesys.booking.entity.Reservation;
import com.multigenesys.booking.entity.ReservationStatus;
import com.multigenesys.booking.entity.Resource;
import com.multigenesys.booking.entity.ResourceType;
import com.multigenesys.booking.entity.Role;
import com.multigenesys.booking.entity.User;
import com.multigenesys.booking.exception.BadRequestException;
import com.multigenesys.booking.exception.ConflictException;
import com.multigenesys.booking.repository.ReservationRepository;
import com.multigenesys.booking.repository.ResourceRepository;
import com.multigenesys.booking.repository.UserRepository;
import com.multigenesys.booking.security.UserPrincipal;
import com.multigenesys.booking.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private User user1;
    private User user2;
    private User admin;
    private UserPrincipal user1Principal;
    private UserPrincipal user2Principal;
    private UserPrincipal adminPrincipal;
    private Resource sampleResource;
    private Reservation sampleReservation;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L)
                .username("user1")
                .email("user1@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .build();
        user1Principal = UserPrincipal.create(user1);

        user2 = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .build();
        user2Principal = UserPrincipal.create(user2);

        admin = User.builder()
                .id(3L)
                .username("admin")
                .email("admin@example.com")
                .password("password")
                .role(Role.ROLE_ADMIN)
                .build();
        adminPrincipal = UserPrincipal.create(admin);

        sampleResource = Resource.builder()
                .id(10L)
                .name("Conference Hall B")
                .type(ResourceType.CONFERENCE_HALL)
                .pricePerHour(new BigDecimal("50.00"))
                .isAvailable(true)
                .build();

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        sampleReservation = Reservation.builder()
                .id(100L)
                .user(user1)
                .resource(sampleResource)
                .startTime(start)
                .endTime(end)
                .totalPrice(new BigDecimal("100.00"))
                .status(ReservationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Create reservation should compute decimal price and associate authenticated user")
    void testCreateReservationSuccess() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(sampleResource));
        when(reservationRepository.existsOverlappingReservation(10L, start, end)).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(sampleReservation);

        ReservationResponse response = reservationService.createReservation(request, user1Principal);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("user1", response.getUsername());
        assertEquals(new BigDecimal("100.00"), response.getTotalPrice());
        assertEquals(ReservationStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("Create reservation should throw ConflictException if time slot overlaps")
    void testCreateReservationOverlapConflict() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(sampleResource));
        when(reservationRepository.existsOverlappingReservation(10L, start, end)).thenReturn(true);

        assertThrows(ConflictException.class, () -> reservationService.createReservation(request, user1Principal));
    }

    @Test
    @DisplayName("Create reservation should throw BadRequestException if end time is before start time")
    void testCreateReservationInvalidTime() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);
        LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .build();

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(request, user1Principal));
    }

    @Test
    @DisplayName("User accessing another user's reservation should throw AccessDeniedException")
    void testUserAccessOtherReservationForbidden() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));

        assertThrows(AccessDeniedException.class, () -> reservationService.getReservationById(100L, user2Principal));
    }

    @Test
    @DisplayName("Admin accessing any user's reservation should succeed")
    void testAdminAccessAnyReservationSuccess() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));

        ReservationResponse response = reservationService.getReservationById(100L, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("user1", response.getUsername());
    }

    @Test
    @DisplayName("User cancelling own reservation should set status to CANCELLED")
    void testUserCancelOwnReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.updateReservationStatus(100L, ReservationStatus.CANCELLED, user1Principal);

        assertNotNull(response);
        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
    }

    @Test
    @DisplayName("User trying to set status to CONFIRMED should throw BadRequestException")
    void testUserConfirmReservationBadRequest() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));

        assertThrows(BadRequestException.class, () -> reservationService.updateReservationStatus(100L, ReservationStatus.CONFIRMED, user1Principal));
    }

    @Test
    @DisplayName("Get reservations with pagination and filtering should call repository specification")
    void testGetReservations() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> page = new PageImpl<>(List.of(sampleReservation), pageable, 1);

        when(reservationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ReservationResponse> responses = reservationService.getReservations(
                ReservationStatus.PENDING,
                new BigDecimal("50.00"),
                new BigDecimal("200.00"),
                pageable,
                user1Principal
        );

        assertNotNull(responses);
        assertEquals(1, responses.getTotalElements());
        assertEquals(100L, responses.getContent().get(0).getId());
    }
}
