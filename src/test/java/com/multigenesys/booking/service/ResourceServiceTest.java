package com.multigenesys.booking.service;

import com.multigenesys.booking.dto.request.ResourceRequest;
import com.multigenesys.booking.dto.response.ResourceResponse;
import com.multigenesys.booking.entity.Resource;
import com.multigenesys.booking.entity.ResourceType;
import com.multigenesys.booking.exception.ResourceNotFoundException;
import com.multigenesys.booking.repository.ResourceRepository;
import com.multigenesys.booking.service.impl.ResourceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private Resource sampleResource;

    @BeforeEach
    void setUp() {
        sampleResource = Resource.builder()
                .id(1L)
                .name("Conference Room A")
                .description("Spacious meeting room")
                .type(ResourceType.CONFERENCE_HALL)
                .pricePerHour(new BigDecimal("100.00"))
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Create resource should save and return ResourceResponse")
    void testCreateResource() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Conference Room A")
                .description("Spacious meeting room")
                .type(ResourceType.CONFERENCE_HALL)
                .pricePerHour(new BigDecimal("100.00"))
                .isAvailable(true)
                .build();

        when(resourceRepository.save(any(Resource.class))).thenReturn(sampleResource);

        ResourceResponse response = resourceService.createResource(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Conference Room A", response.getName());
        assertEquals(new BigDecimal("100.00"), response.getPricePerHour());
    }

    @Test
    @DisplayName("Get resource by id should return resource if found")
    void testGetResourceByIdSuccess() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));

        ResourceResponse response = resourceService.getResourceById(1L);

        assertNotNull(response);
        assertEquals("Conference Room A", response.getName());
    }

    @Test
    @DisplayName("Get resource by id should throw ResourceNotFoundException if not found")
    void testGetResourceByIdNotFound() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceService.getResourceById(99L));
    }

    @Test
    @DisplayName("Get all resources should support pagination and filtering")
    void testGetAllResources() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Resource> page = new PageImpl<>(List.of(sampleResource), pageable, 1);

        when(resourceRepository.findAll(pageable)).thenReturn(page);

        Page<ResourceResponse> result = resourceService.getAllResources(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Conference Room A", result.getContent().get(0).getName());
    }
}
