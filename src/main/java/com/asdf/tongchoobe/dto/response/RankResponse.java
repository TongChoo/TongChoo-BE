package com.asdf.tongchoobe.dto.response;

import com.asdf.tongchoobe.domain.Grade;
import com.asdf.tongchoobe.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "내 등급/경험치 응답")
public class RankResponse {
    private Grade grade;
    private String gradeLabel;
    private int totalXp;
    private Grade nextGrade;
    private String nextGradeLabel;
    private Integer xpToNext;
    private int excuseCount;

    public static RankResponse from(User user) {
        Grade grade = user.getGrade();
        Grade nextGrade = grade.next();

        return RankResponse.builder()
                .grade(grade)
                .gradeLabel(grade.getLabel())
                .totalXp(user.getTotalXp())
                .nextGrade(nextGrade)
                .nextGradeLabel(nextGrade == null ? null : nextGrade.getLabel())
                .xpToNext(nextGrade == null ? null : nextGrade.getMinXp() - user.getTotalXp())
                .excuseCount(user.getExcuseCount())
                .build();
    }
}
