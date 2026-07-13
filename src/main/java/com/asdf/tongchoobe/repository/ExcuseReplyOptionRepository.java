package com.asdf.tongchoobe.repository;

import com.asdf.tongchoobe.domain.ExcuseReplyOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExcuseReplyOptionRepository extends JpaRepository<ExcuseReplyOption, Long> {
    List<ExcuseReplyOption> findByExcuseIdOrderBySortOrderAsc(Long excuseId);
}
