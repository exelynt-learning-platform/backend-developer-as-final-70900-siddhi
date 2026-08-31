package com.multigenesys.booking.service.impl;

import com.multigenesys.booking.dto.request.ResourceRequest;
import com.multigenesys.booking.dto.response.ResourceResponse;
import com.multigenesys.booking.entity.Resource;
import com.multigenesys.booking.entity.ResourceType;
import com.multigenesys.booking.exception.ResourceNotFoundException;
import com.multigenesys.booking.repository.ResourceRepository;
import com.multigenesys.booking.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;

    @Override
    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        log.info("Creating new resource: name={}, type={}", request.getName(), request.getType());

        Resource resource = Resource.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .type(request.getType())
                .pricePerHour(request.getPricePerHour())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .build();

        Resource savedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(savedResource);
    }

    @Override
    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        log.info("Updating resource with id: {}", id);

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        resource.setName(request.getName().trim());
        resource.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        resource.setType(request.getType());
        resource.setPricePerHour(request.getPricePerHour());
        if (request.getIsAvailable() != null) {
            resource.setIsAvailable(request.getIsAvailable());
        }

        Resource updatedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(updatedResource);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        log.info("Fetching resource by id: {}", id);

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        return ResourceResponse.fromEntity(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceResponse> getAllResources(ResourceType type, Boolean availableOnly, Pageable pageable) {
        log.info("Fetching resources: type={}, availableOnly={}, page={}, size={}", 
                type, availableOnly, pageable.getPageNumber(), pageable.getPageSize());

        Page<Resource> resourcePage;

        if (type != null && Boolean.TRUE.equals(availableOnly)) {
            resourcePage = resourceRepository.findByTypeAndIsAvailableTrue(type, pageable);
        } else if (type != null) {
            resourcePage = resourceRepository.findByType(type, pageable);
        } else if (Boolean.TRUE.equals(availableOnly)) {
            resourcePage = resourceRepository.findAllByIsAvailableTrue(pageable);
        } else {
            resourcePage = resourceRepository.findAll(pageable);
        }

        return resourcePage.map(ResourceResponse::fromEntity);
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        log.info("Deleting resource by id: {}", id);

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        resourceRepository.delete(resource);
    }
}
