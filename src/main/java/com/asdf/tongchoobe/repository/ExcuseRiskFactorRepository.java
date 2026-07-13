package com.asdf.tongchoobe.repository;

import com.asdf.tongchoobe.domain.ExcuseRiskFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExcuseRiskFactorRepository extends JpaRepository<ExcuseRiskFactor, Long> {
    List<ExcuseRiskFactor> findByExcuseIdOrderBySortOrderAsc(Long excuseId);
}
