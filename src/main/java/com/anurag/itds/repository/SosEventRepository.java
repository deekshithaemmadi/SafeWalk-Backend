package com.anurag.itds.repository;

import com.anurag.itds.entity.SosEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SosEventRepository extends JpaRepository<SosEvent, Long> {

    Optional<SosEvent> findByIdAndUserId(Long id, Long userId);

    List<SosEvent> findByUserIdOrderByCreatedAtDesc(Long userId);
}
