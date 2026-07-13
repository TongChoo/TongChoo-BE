package com.asdf.tongchoobe.repository;

import com.asdf.tongchoobe.domain.Excuse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExcuseRepository extends JpaRepository<Excuse, Long> {
    Page<Excuse> findByUserIdAndParentIsNullAndReplyToExcuseIsNullOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    long countByUserIdAndParentIsNullAndReplyToExcuseIsNull(Long userId);

    List<Excuse> findByUserIdOrderByCreatedAtDesc(Long userId);
}
