package com.multigenesys.booking.service;

import com.multigenesys.booking.dto.request.ResourceRequest;
import com.multigenesys.booking.dto.response.ResourceResponse;
import com.multigenesys.booking.entity.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResourceService {

    ResourceResponse createResource(ResourceRequest request);

    ResourceResponse updateResource(Long id, ResourceRequest request);

    ResourceResponse getResourceById(Long id);

    Page<ResourceResponse> getAllResources(ResourceType type, Boolean availableOnly, Pageable pageable);

    void deleteResource(Long id);
}
