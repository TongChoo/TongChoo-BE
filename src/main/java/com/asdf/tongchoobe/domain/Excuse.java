package com.asdf.tongchoobe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "excuses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_excuses_reply_to_excuse_id",
                columnNames = "reply_to_excuse_id"
        )
)
@EntityListeners(AuditingEntityListener.class)
public class Excuse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_excuse_id")
    private Excuse replyToExcuse;

    @Column(nullable = false, length = 500)
    private String situation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Target target;

    @Column(name = "target_description", length = 100)
    private String targetDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tone tone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "situation_severity", nullable = false, length = 10)
    private SituationSeverity situationSeverity = SituationSeverity.NORMAL;

    @Column(name = "excuse_text", nullable = false, length = 1000)
    private String excuseText;

    @Column(length = 500)
    private String incomingMessage;

    @Builder.Default
    @Column(nullable = false)
    private int roundNumber = 1;

    @Column(nullable = false)
    private int successRate;

    @Column(nullable = false)
    private int realism;

    @Column(nullable = false)
    private int persuasion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SuspicionLevel suspicionLevel;

    @Builder.Default
    @Column(nullable = false)
    private int earnedXp = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
