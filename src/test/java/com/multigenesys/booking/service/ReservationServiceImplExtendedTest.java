package com.multigenesys.booking.service;

import com.multigenesys.booking.dto.request.ReservationRequest;
import com.multigenesys.booking.dto.response.ReservationResponse;
import com.multigenesys.booking.entity.*;
import com.multigenesys.booking.exception.BadRequestException;
import com.multigenesys.booking.exception.ConflictException;
import com.multigenesys.booking.exception.ResourceNotFoundException;
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
class ReservationServiceImplExtendedTest {

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
        user1 = User.builder().id(1L).username("user1").email("user1@example.com")
                .password("password").role(Role.ROLE_USER).build();
        user2 = User.builder().id(2L).username("user2").email("user2@example.com")
                .password("password").role(Role.ROLE_USER).build();
        admin = User.builder().id(3L).username("admin").email("admin@example.com")
                .password("password").role(Role.ROLE_ADMIN).build();

        user1Principal = UserPrincipal.create(user1);
        user2Principal = UserPrincipal.create(user2);
        adminPrincipal = UserPrincipal.create(admin);

        sampleResource = Resource.builder()
                .id(10L).name("Conference Hall B").type(ResourceType.CONFERENCE_HALL)
                .pricePerHour(new BigDecimal("50.00")).isAvailable(true).build();

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        sampleReservation = Reservation.builder()
                .id(100L).user(user1).resource(sampleResource).startTime(start).endTime(end)
                .totalPrice(new BigDecimal("100.00")).status(ReservationStatus.PENDING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("Create reservation should fail if resource is unavailable")
    void testCreateReservationResourceUnavailable() {
        sampleResource.setIsAvailable(false);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L).startTime(start).endTime(end).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(sampleResource));

        assertThrows(BadRequestException.class,
                () -> reservationService.createReservation(request, user1Principal));
    }

    @Test
    @DisplayName("Create reservation should fail if duration < 15 minutes")
    void testCreateReservationTooShort() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(10);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L).startTime(start).endTime(end).build();

        assertThrows(BadRequestException.class,
                () -> reservationService.createReservation(request, user1Principal));
    }

    @Test
    @DisplayName("Create reservation should fail if user not found")
    void testCreateReservationUserNotFound() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L).startTime(start).endTime(end).build();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.createReservation(request, user1Principal));
    }

    @Test
    @DisplayName("Create reservation should fail if resource not found")
    void testCreateReservationResourceNotFound() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(99L).startTime(start).endTime(end).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.createReservation(request, user1Principal));
    }

    @Test
    @DisplayName("GetReservationById not found should throw ResourceNotFoundException")
    void testGetReservationByIdNotFound() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.getReservationById(999L, adminPrincipal));
    }

    @Test
    @DisplayName("User can access their own reservation by ID")
    void testUserAccessOwnReservationSuccess() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));

        ReservationResponse response = reservationService.getReservationById(100L, user1Principal);

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Admin can update reservation to any status including CONFIRMED")
    void testAdminConfirmReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.updateReservationStatus(
                100L, ReservationStatus.CONFIRMED, adminPrincipal);

        assertNotNull(response);
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
    }

    @Test
    @DisplayName("Admin reactivating a cancelled reservation with overlap should throw ConflictException")
    void testAdminReactivateCancelledReservationWithOverlap() {
        sampleReservation.setStatus(ReservationStatus.CANCELLED);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));
        when(reservationRepository.existsOverlappingReservationExcludingSelf(
                eq(10L), eq(100L), any(), any())).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> reservationService.updateReservationStatus(
                        100L, ReservationStatus.CONFIRMED, adminPrincipal));
    }

    @Test
    @DisplayName("Admin reactivating cancelled reservation without overlap should succeed")
    void testAdminReactivateCancelledReservationSuccess() {
        sampleReservation.setStatus(ReservationStatus.CANCELLED);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));
        when(reservationRepository.existsOverlappingReservationExcludingSelf(
                eq(10L), eq(100L), any(), any())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.updateReservationStatus(
                100L, ReservationStatus.CONFIRMED, adminPrincipal);

        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
    }

    @Test
    @DisplayName("User cannot update status for another user's reservation")
    void testUserCannotUpdateOtherUserReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));

        assertThrows(AccessDeniedException.class,
                () -> reservationService.updateReservationStatus(
                        100L, ReservationStatus.CANCELLED, user2Principal));
    }

    @Test
    @DisplayName("Cancelling an already cancelled reservation should throw BadRequestException")
    void testCancelAlreadyCancelledReservation() {
        sampleReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));

        assertThrows(BadRequestException.class,
                () -> reservationService.updateReservationStatus(
                        100L, ReservationStatus.CANCELLED, user1Principal));
    }

    @Test
    @DisplayName("Admin delete reservation should succeed")
    void testAdminDeleteReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> reservationService.deleteReservation(100L, adminPrincipal));
        assertEquals(ReservationStatus.CANCELLED, sampleReservation.getStatus());
    }

    @Test
    @DisplayName("User delete another user's reservation should throw AccessDeniedException")
    void testUserDeleteOtherUserReservationForbidden() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));

        assertThrows(AccessDeniedException.class,
                () -> reservationService.deleteReservation(100L, user2Principal));
    }

    @Test
    @DisplayName("Delete already cancelled reservation should throw BadRequestException")
    void testDeleteAlreadyCancelledReservation() {
        sampleReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));

        assertThrows(BadRequestException.class,
                () -> reservationService.deleteReservation(100L, adminPrincipal));
    }

    @Test
    @DisplayName("Delete reservation not found should throw ResourceNotFoundException")
    void testDeleteReservationNotFound() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.deleteReservation(999L, adminPrincipal));
    }

    @Test
    @DisplayName("Admin can see all reservations (userId filter is null)")
    void testAdminGetAllReservations() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> page = new PageImpl<>(List.of(sampleReservation), pageable, 1);

        when(reservationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ReservationResponse> responses = reservationService.getReservations(
                null, null, null, pageable, adminPrincipal);

        assertNotNull(responses);
        assertEquals(1, responses.getTotalElements());
    }

    @Test
    @DisplayName("UpdateReservation not found should throw ResourceNotFoundException")
    void testUpdateReservationNotFound() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.updateReservationStatus(
                        999L, ReservationStatus.CANCELLED, adminPrincipal));
    }

    @Test
    @DisplayName("Admin cancel PENDING reservation without overlap check should succeed")
    void testAdminCancelPendingReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(sampleReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.updateReservationStatus(
                100L, ReservationStatus.CANCELLED, adminPrincipal);

        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
    }
}
