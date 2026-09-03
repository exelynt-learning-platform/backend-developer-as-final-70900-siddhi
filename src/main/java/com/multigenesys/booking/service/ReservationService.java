package com.multigenesys.booking.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.multigenesys.booking.dto.ReservationRequest;
import com.multigenesys.booking.entity.Reservation;
import com.multigenesys.booking.entity.Resource;
import com.multigenesys.booking.entity.User;
import com.multigenesys.booking.enums.ReservationStatus;
import com.multigenesys.booking.exception.ResourceNotFoundException;
import com.multigenesys.booking.repository.ReservationRepository;
import com.multigenesys.booking.repository.ResourceRepository;
import com.multigenesys.booking.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository) {

        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public Reservation createReservation(
            ReservationRequest request,
            String username) {

        validateDates(
                request.getStartDateTime(),
                request.getEndDateTime()
        );

        Resource resource = resourceRepository
                .findById(request.getResourceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id: "
                                        + request.getResourceId()
                        )
                );

        if (!Boolean.TRUE.equals(resource.getAvailable())) {
            throw new IllegalStateException(
                    "Resource is not available"
            );
        }

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + username
                        )
                );

        Reservation reservation = new Reservation();

        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartDateTime(request.getStartDateTime());
        reservation.setEndDateTime(request.getEndDateTime());
        reservation.setPrice(request.getPrice());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }

    public Page<Reservation> getReservations(
            String username,
            boolean isAdmin,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new IllegalArgumentException(
                    "Minimum price cannot be greater than maximum price"
            );
        }

        Specification<Reservation> specification = Specification.where(null);

        if (!isAdmin) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("user")
                                            .get("username"),
                                    username
                            )
            );
        }

        if (status != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("status"),
                                    status
                            )
            );
        }

        if (minPrice != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.greaterThanOrEqualTo(
                                    root.get("price"),
                                    minPrice
                            )
            );
        }

        if (maxPrice != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.lessThanOrEqualTo(
                                    root.get("price"),
                                    maxPrice
                            )
            );
        }

        return reservationRepository.findAll(
                specification,
                pageable
        );
    }

    public Reservation getReservationById(
            Long id,
            String username,
            boolean isAdmin) {

        Reservation reservation = reservationRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found with id: " + id
                        )
                );

        if (!isAdmin &&
                !reservation.getUser()
                        .getUsername()
                        .equals(username)) {

            throw new AccessDeniedException(
                    "You are not allowed to access this reservation"
            );
        }

        return reservation;
    }

    public Reservation updateReservation(
            Long id,
            ReservationRequest request,
            String username,
            boolean isAdmin) {

        Reservation reservation =
                getReservationById(id, username, isAdmin);

        validateDates(
                request.getStartDateTime(),
                request.getEndDateTime()
        );

        Resource resource = resourceRepository
                .findById(request.getResourceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id: "
                                        + request.getResourceId()
                        )
                );

        if (!Boolean.TRUE.equals(resource.getAvailable())) {
            throw new IllegalStateException(
                    "Resource is not available"
            );
        }

        reservation.setResource(resource);
        reservation.setStartDateTime(
                request.getStartDateTime()
        );
        reservation.setEndDateTime(
                request.getEndDateTime()
        );
        reservation.setPrice(request.getPrice());

        return reservationRepository.save(reservation);
    }

    public void deleteReservation(
            Long id,
            String username,
            boolean isAdmin) {

        Reservation reservation =
                getReservationById(id, username, isAdmin);

        reservationRepository.delete(reservation);
    }

    public Reservation updateStatus(
            Long id,
            ReservationStatus status) {

        Reservation reservation =
                reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reservation not found with id: "
                                                + id
                                )
                        );

        reservation.setStatus(status);

        return reservationRepository.save(reservation);
    }

    private void validateDates(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {

        if (!endDateTime.isAfter(startDateTime)) {
            throw new IllegalArgumentException(
                    "End date and time must be after start date and time"
            );
        }
    }
}
