package com.gomsu.workshopservice.repository;

import com.gomsu.workshopservice.entity.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {
}
