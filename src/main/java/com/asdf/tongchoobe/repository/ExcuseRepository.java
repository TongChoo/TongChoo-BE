package com.asdf.tongchoobe.repository;

import com.asdf.tongchoobe.domain.Excuse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ExcuseRepository extends JpaRepository<Excuse, Long> {
    long countByUserIdAndReplyToExcuseIsNull(Long userId);

    @Query(
            value = """
                    select e from Excuse e
                    where e.user.id = :userId
                      and not exists (
                          select 1 from Excuse child where child.replyToExcuse = e
                      )
                    order by e.createdAt desc
                    """,
            countQuery = """
                    select count(e) from Excuse e
                    where e.user.id = :userId
                      and not exists (
                          select 1 from Excuse child where child.replyToExcuse = e
                      )
                    """
    )
    Page<Excuse> findLatestConversationRounds(
            @Param("userId") Long userId,
            Pageable pageable
    );

    boolean existsByReplyToExcuseId(Long excuseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Excuse e where e.id = :id")
    Optional<Excuse> findByIdForUpdate(@Param("id") Long id);
}
