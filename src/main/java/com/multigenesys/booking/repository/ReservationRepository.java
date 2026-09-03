package com.multigenesys.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.multigenesys.booking.entity.Reservation;
import com.multigenesys.booking.enums.ReservationStatus;

import java.util.List;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    List<Reservation> findByUserUsername(String username);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByUserUsernameAndStatus(
            String username,
            ReservationStatus status
    );
}