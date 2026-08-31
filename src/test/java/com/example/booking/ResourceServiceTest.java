package com.example.booking;

import com.example.booking.dto.ResourceRequest;
import com.example.booking.entity.Resource;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void getAll_shouldReturnAllResources() {
        Resource r = new Resource();
        r.setName("Room 101");
        when(resourceRepository.findAll()).thenReturn(List.of(r));

        List<Resource> result = resourceService.getAll();

        assertEquals(1, result.size());
        assertEquals("Room 101", result.get(0).getName());
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> resourceService.getById(99L));
    }

    @Test
    void create_shouldSaveResource() {
        ResourceRequest request = new ResourceRequest();
        request.setName("Room 202");
        request.setType("room");

        Resource saved = new Resource();
        saved.setName("Room 202");
        when(resourceRepository.save(any())).thenReturn(saved);

        Resource result = resourceService.create(request);

        assertEquals("Room 202", result.getName());
    }

    @Test
    void update_shouldUpdateAndSaveResource() {
        Resource existing = new Resource();
        existing.setId(1L);
        existing.setName("Old Name");
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(resourceRepository.save(any())).thenReturn(existing);

        ResourceRequest request = new ResourceRequest();
        request.setName("New Name");
        request.setType("room");
        request.setDescription("Updated room");

        Resource result = resourceService.update(1L, request);

        assertEquals("New Name", result.getName());
        verify(resourceRepository).save(existing);
    }

    @Test
    void delete_shouldDeleteResource_whenExists() {
        when(resourceRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> resourceService.delete(1L));
        verify(resourceRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        when(resourceRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> resourceService.delete(99L));
    }
}