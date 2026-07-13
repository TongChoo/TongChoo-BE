package com.asdf.tongchoobe.repository;

import com.asdf.tongchoobe.domain.ExcuseRememberItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExcuseRememberItemRepository extends JpaRepository<ExcuseRememberItem, Long> {
    List<ExcuseRememberItem> findByExcuseIdOrderBySortOrderAsc(Long excuseId);
}
