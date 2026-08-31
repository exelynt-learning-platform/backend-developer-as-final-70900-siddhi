package com.multigenesys.booking.service;

import com.multigenesys.booking.dto.request.ReservationRequest;
import com.multigenesys.booking.dto.response.ReservationResponse;
import com.multigenesys.booking.entity.ReservationStatus;
import com.multigenesys.booking.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ReservationService {

    ReservationResponse createReservation(ReservationRequest request, UserPrincipal currentUser);

    Page<ReservationResponse> getReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable,
            UserPrincipal currentUser
    );

    ReservationResponse getReservationById(Long id, UserPrincipal currentUser);

    ReservationResponse updateReservationStatus(Long id, ReservationStatus status, UserPrincipal currentUser);

    void deleteReservation(Long id, UserPrincipal currentUser);
}
