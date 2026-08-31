package com.multigenesys.booking.service.impl;

import com.multigenesys.booking.dto.request.ReservationRequest;
import com.multigenesys.booking.dto.response.ReservationResponse;
import com.multigenesys.booking.entity.Reservation;
import com.multigenesys.booking.entity.ReservationStatus;
import com.multigenesys.booking.entity.Resource;
import com.multigenesys.booking.entity.Role;
import com.multigenesys.booking.entity.User;
import com.multigenesys.booking.exception.BadRequestException;
import com.multigenesys.booking.exception.ConflictException;
import com.multigenesys.booking.exception.ResourceNotFoundException;
import com.multigenesys.booking.repository.ReservationRepository;
import com.multigenesys.booking.repository.ReservationSpecification;
import com.multigenesys.booking.repository.ResourceRepository;
import com.multigenesys.booking.repository.UserRepository;
import com.multigenesys.booking.security.UserPrincipal;
import com.multigenesys.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, UserPrincipal currentUser) {
        log.info("Creating reservation for userId={}, resourceId={}, start={}, end={}",
                currentUser.getId(), request.getResourceId(), request.getStartTime(), request.getEndTime());

        // Validate time range
        validateReservationTime(request.getStartTime(), request.getEndTime());

        // Fetch User entity from database
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        // Fetch Resource entity
        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", request.getResourceId()));

        if (!Boolean.TRUE.equals(resource.getIsAvailable())) {
            throw new BadRequestException("The requested resource is currently unavailable for booking");
        }

        // Check for conflicting overlapping reservations
        boolean hasOverlap = reservationRepository.existsOverlappingReservation(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (hasOverlap) {
            throw new ConflictException("Resource is already booked for the selected time interval");
        }

        // Calculate total price based on duration and hourly rate
        BigDecimal totalPrice = calculateTotalPrice(request.getStartTime(), request.getEndTime(), resource.getPricePerHour());

        Reservation reservation = Reservation.builder()
                .user(user)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(savedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable,
            UserPrincipal currentUser) {

        Long targetUserId = getUserIdToFilterBy(currentUser);

        log.info("Fetching reservations: role={}, targetUserId={}, status={}, minPrice={}, maxPrice={}",
                currentUser.getRole(), targetUserId, status, minPrice, maxPrice);

        Page<Reservation> reservationPage = reservationRepository.findAll(
                ReservationSpecification.withFilter(targetUserId, status, minPrice, maxPrice),
                pageable
        );

        return reservationPage.map(ReservationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, UserPrincipal currentUser) {
        log.info("Fetching reservation by id: {}, requested by userId={}", id, currentUser.getId());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        // Enforce RBAC: Non-admin users can only view their own reservations
        if (currentUser.getRole() != Role.ROLE_ADMIN && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to view this reservation");
        }

        return ReservationResponse.fromEntity(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatus newStatus, UserPrincipal currentUser) {
        log.info("Updating reservation id: {} to status: {} by userId={}", id, newStatus, currentUser.getId());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            applyAdminStatusTransition(reservation, newStatus);
        } else {
            applyUserStatusTransition(reservation, newStatus, currentUser);
        }

        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(updated);
    }

    private void applyAdminStatusTransition(Reservation reservation, ReservationStatus newStatus) {
        if (newStatus != ReservationStatus.CANCELLED && reservation.getStatus() == ReservationStatus.CANCELLED) {
            boolean hasOverlap = reservationRepository.existsOverlappingReservationExcludingSelf(
                    reservation.getResource().getId(),
                    reservation.getId(),
                    reservation.getStartTime(),
                    reservation.getEndTime()
            );
            if (hasOverlap) {
                throw new ConflictException("Cannot reactivate reservation: time slot is now occupied by another booking");
            }
        }
        reservation.setStatus(newStatus);
    }

    private void applyUserStatusTransition(Reservation reservation, ReservationStatus newStatus, UserPrincipal currentUser) {
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to modify this reservation");
        }

        if (newStatus != ReservationStatus.CANCELLED) {
            throw new BadRequestException("Regular users can only cancel their own reservations");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    /**
     * Cancels a reservation by performing a logical soft delete (setting status to CANCELLED).
     * This preserves historical booking records and maintains audit integrity.
     */
    @Override
    @Transactional
    public void deleteReservation(Long id, UserPrincipal currentUser) {
        log.info("Cancelling reservation id: {} by userId={}, role={}", id, currentUser.getId(), currentUser.getRole());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        if (currentUser.getRole() != Role.ROLE_ADMIN && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to cancel this reservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    private Long getUserIdToFilterBy(UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return null; // Admin can view all reservations
        }
        return currentUser.getId(); // Regular user can only view their own
    }

    private void validateReservationTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time must be specified");
        }

        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be strictly after start time");
        }

        Duration duration = Duration.between(startTime, endTime);
        if (duration.toMinutes() < 15) {
            throw new BadRequestException("Minimum reservation duration is 15 minutes");
        }
    }

    private BigDecimal calculateTotalPrice(LocalDateTime startTime, LocalDateTime endTime, BigDecimal pricePerHour) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        return hours.multiply(pricePerHour).setScale(2, RoundingMode.HALF_UP);
    }
}
