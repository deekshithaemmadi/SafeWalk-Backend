package com.anurag.itds.repository;

import com.anurag.itds.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByIdAndUserId(Long id, Long userId);

    Page<Trip> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
