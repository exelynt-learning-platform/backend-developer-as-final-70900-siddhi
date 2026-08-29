package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import com.example.booking.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	Page<Reservation> findByUserId(Long userId, Pageable pageable);

	Page<Reservation> findByStatusAndPriceBetween(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
			Pageable pageable);
}
