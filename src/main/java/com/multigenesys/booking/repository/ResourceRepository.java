package com.multigenesys.booking.repository;

import com.multigenesys.booking.entity.Resource;
import com.multigenesys.booking.entity.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    Page<Resource> findAllByIsAvailableTrue(Pageable pageable);

    Page<Resource> findByType(ResourceType type, Pageable pageable);

    Page<Resource> findByTypeAndIsAvailableTrue(ResourceType type, Pageable pageable);
}
