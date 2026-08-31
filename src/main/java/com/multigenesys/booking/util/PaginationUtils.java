package com.multigenesys.booking.util;

import com.multigenesys.booking.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PaginationUtils {

    public static final Set<String> RESOURCE_SORT_FIELDS = Set.of(
            "id", "name", "type", "pricePerHour", "isAvailable", "createdAt", "updatedAt"
    );

    public static final Set<String> RESERVATION_SORT_FIELDS = Set.of(
            "id", "startTime", "endTime", "totalPrice", "status", "createdAt", "updatedAt"
    );

    private PaginationUtils() {
        // Utility class
    }

    public static Pageable createResourcePageable(int page, int size, String sortBy, String sortDirection) {
        return createPageable(page, size, sortBy, sortDirection, RESOURCE_SORT_FIELDS);
    }

    public static Pageable createReservationPageable(int page, int size, String sortBy, String sortDirection) {
        return createPageable(page, size, sortBy, sortDirection, RESERVATION_SORT_FIELDS);
    }

    public static Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Set<String> allowedSortFields) {

        if (page < 0) {
            throw new BadRequestException("Page index must not be less than zero");
        }

        if (size < 1) {
            throw new BadRequestException("Page size must not be less than one");
        }

        if (allowedSortFields != null && !allowedSortFields.contains(sortBy)) {
            throw new BadRequestException("Invalid sortBy field: '" + sortBy + "'. Allowed fields are: " + allowedSortFields);
        }

        if (!"ASC".equalsIgnoreCase(sortDirection) && !"DESC".equalsIgnoreCase(sortDirection)) {
            throw new BadRequestException("Invalid sortDirection: '" + sortDirection + "'. Allowed values are: 'ASC', 'DESC'");
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
