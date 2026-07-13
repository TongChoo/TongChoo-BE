package com.asdf.tongchoobe.dto.response;

import com.asdf.tongchoobe.domain.Excuse;
import com.asdf.tongchoobe.domain.ExcuseAftermath;
import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Getter
@Builder
@Schema(description = "변명 기록 목록 요약 응답")
public class ExcuseSummaryResponse {
    private Long id;
    private String situation;
    private Target target;
    private String targetDescription;
    private Tone tone;
    private int roundNumber;
    private int successRate;
    private Integer nextAftermathInDays;
    private Instant createdAt;

    public static ExcuseSummaryResponse from(Excuse excuse, List<ExcuseAftermath> aftermaths) {
        return ExcuseSummaryResponse.builder()
                .id(excuse.getId())
                .situation(excuse.getSituation())
                .target(excuse.getTarget())
                .targetDescription(excuse.getTargetDescription())
                .tone(excuse.getTone())
                .roundNumber(excuse.getRoundNumber())
                .successRate(excuse.getSuccessRate())
                .nextAftermathInDays(calculateNextAftermathInDays(excuse, aftermaths))
                .createdAt(excuse.getCreatedAt())
                .build();
    }

    private static Integer calculateNextAftermathInDays(Excuse excuse, List<ExcuseAftermath> aftermaths) {
        if (excuse.getCreatedAt() == null || aftermaths.isEmpty()) {
            return null;
        }

        LocalDate createdDate = excuse.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate today = LocalDate.now();

        return aftermaths.stream()
                .map(aftermath -> (int) ChronoUnit.DAYS.between(today, createdDate.plusDays(aftermath.getDayOffset())))
                .filter(daysLeft -> daysLeft >= 0)
                .min(Integer::compareTo)
                .orElse(null);
    }
}
