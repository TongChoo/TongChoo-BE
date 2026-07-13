package com.asdf.tongchoobe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "excuse_reply_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_excuse_reply_option_order",
                columnNames = {"excuse_id", "sort_order"}
        )
)
public class ExcuseReplyOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "excuse_id", nullable = false)
    private Excuse excuse;

    @Column(name = "option_text", nullable = false, length = 1000)
    private String optionText;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean selected = false;

    public void select() {
        selected = true;
    }

    public void unselect() {
        selected = false;
    }
}
