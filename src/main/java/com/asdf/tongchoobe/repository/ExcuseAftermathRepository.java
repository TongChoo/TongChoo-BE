package com.asdf.tongchoobe.repository;

import com.asdf.tongchoobe.domain.ExcuseAftermath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExcuseAftermathRepository extends JpaRepository<ExcuseAftermath, Long> {
    List<ExcuseAftermath> findByExcuseIdOrderBySortOrderAsc(Long excuseId);
}
