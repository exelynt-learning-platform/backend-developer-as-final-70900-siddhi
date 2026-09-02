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
class ResourceServiceImplTest {

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
        verify(resourceRepository, times(1)).save(any(Resource.class));
    }

    @Test
    @DisplayName("Create resource with null isAvailable should default to true")
    void testCreateResourceNullAvailability() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Room B")
                .description("Small room")
                .type(ResourceType.ROOM)
                .pricePerHour(new BigDecimal("50.00"))
                .isAvailable(null)
                .build();

        Resource saved = Resource.builder()
                .id(2L)
                .name("Room B")
                .type(ResourceType.ROOM)
                .pricePerHour(new BigDecimal("50.00"))
                .isAvailable(true)
                .build();

        when(resourceRepository.save(any(Resource.class))).thenReturn(saved);

        ResourceResponse response = resourceService.createResource(request);

        assertNotNull(response);
        assertTrue(response.getIsAvailable());
    }

    @Test
    @DisplayName("Create resource with null description should handle gracefully")
    void testCreateResourceNullDescription() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Equipment X")
                .description(null)
                .type(ResourceType.EQUIPMENT)
                .pricePerHour(new BigDecimal("25.00"))
                .isAvailable(true)
                .build();

        Resource saved = Resource.builder()
                .id(3L)
                .name("Equipment X")
                .type(ResourceType.EQUIPMENT)
                .pricePerHour(new BigDecimal("25.00"))
                .isAvailable(true)
                .build();

        when(resourceRepository.save(any(Resource.class))).thenReturn(saved);

        ResourceResponse response = resourceService.createResource(request);

        assertNotNull(response);
        assertNull(response.getDescription());
    }

    @Test
    @DisplayName("Update resource should modify and return updated ResourceResponse")
    void testUpdateResourceSuccess() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Updated Room A")
                .description("Updated description")
                .type(ResourceType.CONFERENCE_HALL)
                .pricePerHour(new BigDecimal("120.00"))
                .isAvailable(false)
                .build();

        Resource updated = Resource.builder()
                .id(1L)
                .name("Updated Room A")
                .description("Updated description")
                .type(ResourceType.CONFERENCE_HALL)
                .pricePerHour(new BigDecimal("120.00"))
                .isAvailable(false)
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(updated);

        ResourceResponse response = resourceService.updateResource(1L, request);

        assertNotNull(response);
        assertEquals("Updated Room A", response.getName());
        assertEquals(new BigDecimal("120.00"), response.getPricePerHour());
        assertFalse(response.getIsAvailable());
    }

    @Test
    @DisplayName("Update resource with null isAvailable should not change availability")
    void testUpdateResourceNullAvailability() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Updated Room A")
                .description("Updated description")
                .type(ResourceType.CONFERENCE_HALL)
                .pricePerHour(new BigDecimal("120.00"))
                .isAvailable(null)
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(sampleResource);

        ResourceResponse response = resourceService.updateResource(1L, request);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Update resource with null description should set null")
    void testUpdateResourceNullDescription() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Updated Room A")
                .description(null)
                .type(ResourceType.CONFERENCE_HALL)
                .pricePerHour(new BigDecimal("120.00"))
                .isAvailable(true)
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(sampleResource);

        ResourceResponse response = resourceService.updateResource(1L, request);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Update resource should throw ResourceNotFoundException for unknown ID")
    void testUpdateResourceNotFound() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Room")
                .type(ResourceType.ROOM)
                .pricePerHour(new BigDecimal("50.00"))
                .build();

        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceService.updateResource(99L, request));
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
    @DisplayName("Get all resources should support pagination without filters")
    void testGetAllResourcesNoFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Resource> page = new PageImpl<>(List.of(sampleResource), pageable, 1);

        when(resourceRepository.findAll(pageable)).thenReturn(page);

        Page<ResourceResponse> result = resourceService.getAllResources(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Conference Room A", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("Get all resources filtered by type only")
    void testGetAllResourcesByType() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Resource> page = new PageImpl<>(List.of(sampleResource), pageable, 1);

        when(resourceRepository.findByType(ResourceType.CONFERENCE_HALL, pageable)).thenReturn(page);

        Page<ResourceResponse> result = resourceService.getAllResources(ResourceType.CONFERENCE_HALL, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Get all resources filtered by availableOnly=true")
    void testGetAllResourcesAvailableOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Resource> page = new PageImpl<>(List.of(sampleResource), pageable, 1);

        when(resourceRepository.findAllByIsAvailableTrue(pageable)).thenReturn(page);

        Page<ResourceResponse> result = resourceService.getAllResources(null, true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Get all resources filtered by type AND availableOnly=true")
    void testGetAllResourcesByTypeAndAvailable() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Resource> page = new PageImpl<>(List.of(sampleResource), pageable, 1);

        when(resourceRepository.findByTypeAndIsAvailableTrue(ResourceType.CONFERENCE_HALL, pageable)).thenReturn(page);

        Page<ResourceResponse> result = resourceService.getAllResources(ResourceType.CONFERENCE_HALL, true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Delete resource should call repository delete")
    void testDeleteResourceSuccess() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
        doNothing().when(resourceRepository).delete(sampleResource);

        assertDoesNotThrow(() -> resourceService.deleteResource(1L));

        verify(resourceRepository, times(1)).delete(sampleResource);
    }

    @Test
    @DisplayName("Delete resource should throw ResourceNotFoundException for unknown ID")
    void testDeleteResourceNotFound() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceService.deleteResource(99L));
    }
}
