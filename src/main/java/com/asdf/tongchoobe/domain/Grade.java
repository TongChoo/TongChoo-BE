package com.asdf.tongchoobe.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Grade {
    NOVICE(0, "초보 변명러"),
    SURVIVOR(200, "위기 생존자"),
    EXCUSE_EXPERT(500, "핑계 전문가"),
    SOCIAL_MASTER(900, "사회생활 마스터"),
    EXCUSE_GOD(1400, "변명의 신");

    private final int minXp;
    private final String label;

    public static Grade of(int totalXp) {
        Grade result = NOVICE;
        for (Grade grade : values()) {
            if (totalXp >= grade.minXp) {
                result = grade;
            }
        }
        return result;
    }

    public Grade next() {
        Grade[] grades = values();
        int nextIndex = ordinal() + 1;
        return nextIndex >= grades.length ? null : grades[nextIndex];
    }
}
