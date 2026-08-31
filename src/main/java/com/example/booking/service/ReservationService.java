package com.example.booking.service;

import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationResponse create(ReservationRequest request, String email) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new RuntimeException("Start time and end time are required");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + request.getResourceId()));

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        reservation.setStatus(ReservationStatus.PENDING);

        return toResponse(reservationRepository.save(reservation));
    }

    public Page<ReservationResponse> getMyReservations(
            String email,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return reservationRepository
                .findByUserIdWithFilters(user.getId(), status, minPrice, maxPrice, pageable)
                .map(this::toResponse);
    }

    public Page<ReservationResponse> getAllReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return reservationRepository
                .findWithFilters(status, minPrice, maxPrice, pageable)
                .map(this::toResponse);
    }

    public ReservationResponse getById(Long id, String email, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found with id: " + id));

        if (!isAdmin && !reservation.getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to access this reservation");
        }

        return toResponse(reservation);
    }

    public ReservationResponse update(Long id, ReservationRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new RuntimeException("Start time and end time are required");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found with id: " + id));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + request.getResourceId()));

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());

        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse updateStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found with id: " + id));
        reservation.setStatus(status);
        return toResponse(reservationRepository.save(reservation));
    }

    public void delete(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation not found with id: " + id);
        }
        reservationRepository.deleteById(id);
    }

    private ReservationResponse toResponse(Reservation r) {
        ReservationResponse dto = new ReservationResponse();
        dto.setId(r.getId());
        dto.setUserId(r.getUser().getId());
        dto.setUserName(r.getUser().getName());
        dto.setUserEmail(r.getUser().getEmail());
        dto.setResourceId(r.getResource().getId());
        dto.setResourceName(r.getResource().getName());
        dto.setResourceType(r.getResource().getType());
        dto.setStartTime(r.getStartTime());
        dto.setEndTime(r.getEndTime());
        dto.setPrice(r.getPrice());
        dto.setStatus(r.getStatus());
        return dto;
    }
}
