package com.asdf.tongchoobe.dto.response;

import com.asdf.tongchoobe.domain.Grade;
import com.asdf.tongchoobe.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(description = "내 프로필 응답")
public class UserProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private int totalXp;
    private Grade grade;
    private String gradeLabel;
    private int excuseCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .totalXp(user.getTotalXp())
                .grade(user.getGrade())
                .gradeLabel(user.getGrade().getLabel())
                .excuseCount(user.getExcuseCount())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
