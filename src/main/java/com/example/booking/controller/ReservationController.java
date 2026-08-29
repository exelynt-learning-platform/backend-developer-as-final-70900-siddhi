package com.example.booking.controller;

import com.example.booking.dto.ReservationRequest;
import com.example.booking.entity.Reservation;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<Reservation> create(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            reservationService.create(request, userDetails.getUsername()));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<Reservation>> myReservations(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        return ResponseEntity.ok(
            reservationService.getMyReservations(userDetails.getUsername(), pageable));
    }

    @GetMapping
    public ResponseEntity<Page<Reservation>> getAll(Pageable pageable) {
        return ResponseEntity.ok(reservationService.getAllReservations(pageable));
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<Reservation>> filter(
            @RequestParam ReservationStatus status,
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(
            reservationService.filterReservations(status, minPrice, maxPrice, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Reservation> updateStatus(
            @PathVariable Long id,
            @RequestParam ReservationStatus status) {
        return ResponseEntity.ok(reservationService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}