package com.byakushield.backend.repository;

import com.byakushield.backend.model.Audit;
import com.byakushield.backend.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Long> {

    Page<Audit> findByUserOrderByTimestampDesc(
            User user,
            Pageable pageable
    );

    Page<Audit> findAllByOrderByTimestampDesc(
            Pageable pageable
    );
}