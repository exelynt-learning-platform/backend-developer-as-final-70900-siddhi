package com.multigenesys.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.multigenesys.booking.entity.Resource;


public interface ResourceRepository extends JpaRepository<Resource, Long> {
}