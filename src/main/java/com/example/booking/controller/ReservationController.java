package com.example.booking.controller;

import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Manage reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * USER / ADMIN — create a reservation.
     * User identity is extracted from JWT, NOT from request body.
     */
    @PostMapping
    @Operation(summary = "Create a reservation (USER / ADMIN)")
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.create(request, userDetails.getUsername()));
    }

    /**
     * USER / ADMIN — view own reservations.
     * Supports optional filtering by status, minPrice, maxPrice.
     * Supports pagination: ?page=0&size=10&sort=price,asc
     */
    @GetMapping("/my")
    @Operation(summary = "Get my reservations with optional filters (USER / ADMIN)")
    public ResponseEntity<Page<ReservationResponse>> myReservations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(
                reservationService.getMyReservations(
                        userDetails.getUsername(), status, minPrice, maxPrice, pageable));
    }

    /**
     * ADMIN only — view all reservations.
     * Supports optional filtering by status, minPrice, maxPrice.
     * Supports pagination: ?page=0&size=10&sort=price,desc
     */
    @GetMapping
    @Operation(summary = "Get all reservations with optional filters (ADMIN only)")
    public ResponseEntity<Page<ReservationResponse>> getAll(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(
                reservationService.getAllReservations(status, minPrice, maxPrice, pageable));
    }

    /**
     * ADMIN only — update reservation status.
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update reservation status (ADMIN only)")
    public ResponseEntity<ReservationResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ReservationStatus status) {
        return ResponseEntity.ok(reservationService.updateStatus(id, status));
    }

    /**
     * ADMIN only — delete a reservation.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a reservation (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}