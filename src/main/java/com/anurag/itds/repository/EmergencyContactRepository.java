package com.anurag.itds.repository;

import com.anurag.itds.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    List<EmergencyContact> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<EmergencyContact> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndPhone(Long userId, String phone);

    long countByUserId(Long userId);
}
