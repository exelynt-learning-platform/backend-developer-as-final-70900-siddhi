package com.multigenesys.booking.util;

import com.multigenesys.booking.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

class PaginationUtilsTest {

    @Test
    @DisplayName("createResourcePageable should return valid Pageable with ASC sort")
    void testCreateResourcePageableAsc() {
        Pageable pageable = PaginationUtils.createResourcePageable(0, 10, "name", "ASC");
        assertNotNull(pageable);
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("name").getDirection());
    }

    @Test
    @DisplayName("createResourcePageable should return valid Pageable with DESC sort")
    void testCreateResourcePageableDesc() {
        Pageable pageable = PaginationUtils.createResourcePageable(1, 5, "pricePerHour", "DESC");
        assertNotNull(pageable);
        assertEquals(1, pageable.getPageNumber());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("pricePerHour").getDirection());
    }

    @Test
    @DisplayName("createReservationPageable should work with totalPrice sort")
    void testCreateReservationPageable() {
        Pageable pageable = PaginationUtils.createReservationPageable(0, 20, "totalPrice", "DESC");
        assertNotNull(pageable);
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("totalPrice").getDirection());
    }

    @Test
    @DisplayName("Should throw BadRequestException for invalid sortBy field in resource")
    void testInvalidSortByForResource() {
        assertThrows(BadRequestException.class,
                () -> PaginationUtils.createResourcePageable(0, 10, "invalidField", "ASC"));
    }

    @Test
    @DisplayName("Should throw BadRequestException for invalid sortBy field in reservation")
    void testInvalidSortByForReservation() {
        assertThrows(BadRequestException.class,
                () -> PaginationUtils.createReservationPageable(0, 10, "unknownField", "ASC"));
    }

    @Test
    @DisplayName("Should throw BadRequestException for invalid sortDirection")
    void testInvalidSortDirection() {
        assertThrows(BadRequestException.class,
                () -> PaginationUtils.createResourcePageable(0, 10, "id", "INVALID"));
    }

    @Test
    @DisplayName("Should throw BadRequestException for negative page number")
    void testNegativePage() {
        assertThrows(BadRequestException.class,
                () -> PaginationUtils.createResourcePageable(-1, 10, "id", "ASC"));
    }

    @Test
    @DisplayName("Should throw BadRequestException for zero page size")
    void testZeroPageSize() {
        assertThrows(BadRequestException.class,
                () -> PaginationUtils.createResourcePageable(0, 0, "id", "ASC"));
    }

    @Test
    @DisplayName("createPageable with null allowedFields should not validate sortBy")
    void testNullAllowedFields() {
        Pageable pageable = PaginationUtils.createPageable(0, 10, "anyField", "ASC", null);
        assertNotNull(pageable);
    }

    @Test
    @DisplayName("sortDirection is case-insensitive")
    void testSortDirectionCaseInsensitive() {
        Pageable pageable = PaginationUtils.createResourcePageable(0, 10, "id", "asc");
        assertNotNull(pageable);
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("id").getDirection());
    }
}
