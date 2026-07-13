package com.asdf.tongchoobe.repository;

import com.asdf.tongchoobe.domain.Excuse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ExcuseRepository extends JpaRepository<Excuse, Long> {
    long countByUserIdAndReplyToExcuseIsNull(Long userId);

    List<Excuse> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByReplyToExcuseId(Long excuseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Excuse e where e.id = :id")
    Optional<Excuse> findByIdForUpdate(@Param("id") Long id);
}
