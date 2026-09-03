package com.multigenesys.booking.service;


import org.springframework.stereotype.Service;

import com.multigenesys.booking.dto.ResourceRequest;
import com.multigenesys.booking.entity.Resource;
import com.multigenesys.booking.exception.ResourceNotFoundException;
import com.multigenesys.booking.repository.ResourceRepository;

import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public List<Resource> getAvailableResources() {
        return resourceRepository.findAll()
                .stream()
                .filter(Resource::getAvailable)
                .toList();
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id: " + id
                        )
                );
    }

    public Resource createResource(ResourceRequest request) {

        Resource resource = new Resource();

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setAvailable(
                request.getAvailable() == null
                        ? true
                        : request.getAvailable()
        );

        return resourceRepository.save(resource);
    }

    public Resource updateResource(Long id, ResourceRequest request) {

        Resource resource = getResourceById(id);

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());

        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }

        return resourceRepository.save(resource);
    }

    public void deleteResource(Long id) {

        Resource resource = getResourceById(id);

        resourceRepository.delete(resource);
    }
}
