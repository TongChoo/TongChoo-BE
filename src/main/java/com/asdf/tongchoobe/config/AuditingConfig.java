package com.asdf.tongchoobe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 설정. Backend.md §5 "공통 규칙".
 * 이 애노테이션이 있어야 엔티티의 @CreatedDate/@LastModifiedDate(예: User/Excuse의 createdAt/updatedAt)가
 * 저장/수정 시점에 자동으로 채워진다. 실제 감시(auditing) 로직은 각 엔티티의 @EntityListeners(AuditingEntityListener.class)에서 동작한다.
 */
@Configuration
@EnableJpaAuditing
public class AuditingConfig {
}
