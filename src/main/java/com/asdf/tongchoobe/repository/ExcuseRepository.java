package com.asdf.tongchoobe.repository;

import com.asdf.tongchoobe.domain.Excuse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcuseRepository extends JpaRepository<Excuse, Long> {
    Page<Excuse> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
