package com.multigenesys.booking.controller;

import com.multigenesys.booking.dto.ReservationRequest;
import com.multigenesys.booking.entity.Reservation;
import com.multigenesys.booking.enums.ReservationStatus;
import com.multigenesys.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "Reservations", description = "Manage resource reservations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // =========================================================
    // CREATE RESERVATION
    // =========================================================

    @Operation(summary = "Create a reservation (ADMIN and USER). User identity is taken from JWT.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Reservation> createReservation(

            @Valid @RequestBody ReservationRequest request,

            Authentication authentication) {

        String username = authentication.getName();

        Reservation reservation =
                reservationService.createReservation(request, username);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservation);
    }

    // =========================================================
    // GET ALL RESERVATIONS
    // =========================================================

    @Operation(summary = "Get reservations with optional filtering and pagination. "
            + "ADMIN sees all; USER sees only their own.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<Reservation>> getReservations(

            @RequestParam(required = false) ReservationStatus status,

            @RequestParam(required = false) BigDecimal minPrice,

            @RequestParam(required = false) BigDecimal maxPrice,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(required = false) String sortBy,

            @RequestParam(defaultValue = "desc") String sortDir,

            Authentication authentication) {

        String username = authentication.getName();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        // Build sort direction
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // Default sort field is createdAt; allow optional override via sortBy param
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<Reservation> reservations =
                reservationService.getReservations(
                        username, isAdmin, status, minPrice, maxPrice, pageable);

        return ResponseEntity.ok(reservations);
    }

    // =========================================================
    // GET RESERVATION BY ID
    // =========================================================

    @Operation(summary = "Get reservation by ID. USER can only access their own.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Reservation> getReservationById(

            @PathVariable Long id,

            Authentication authentication) {

        String username = authentication.getName();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(
                reservationService.getReservationById(id, username, isAdmin));
    }

    // =========================================================
    // UPDATE RESERVATION
    // =========================================================

    @Operation(summary = "Update a reservation. USER can only update their own.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Reservation> updateReservation(

            @PathVariable Long id,

            @Valid @RequestBody ReservationRequest request,

            Authentication authentication) {

        String username = authentication.getName();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(
                reservationService.updateReservation(id, request, username, isAdmin));
    }

    // =========================================================
    // DELETE RESERVATION
    // =========================================================

    @Operation(summary = "Delete a reservation. USER can only delete their own.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteReservation(

            @PathVariable Long id,

            Authentication authentication) {

        String username = authentication.getName();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        reservationService.deleteReservation(id, username, isAdmin);

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // UPDATE RESERVATION STATUS (ADMIN only)
    // =========================================================

    @Operation(summary = "Update reservation status (ADMIN only)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reservation> updateStatus(

            @PathVariable Long id,

            @RequestParam ReservationStatus status) {

        return ResponseEntity.ok(reservationService.updateStatus(id, status));
    }
}