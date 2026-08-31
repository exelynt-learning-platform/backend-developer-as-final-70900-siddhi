package com.multigenesys.booking.repository;

import com.multigenesys.booking.entity.Reservation;
import com.multigenesys.booking.entity.ReservationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReservationSpecification {

    public static Specification<Reservation> withFilter(
            Long userId,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by user ownership (if specified)
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            // Filter by status (PENDING, CONFIRMED, CANCELLED)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Filter by minPrice
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalPrice"), minPrice));
            }

            // Filter by maxPrice
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalPrice"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
