package com.asdf.tongchoobe.dto.response;

import com.asdf.tongchoobe.domain.Excuse;
import com.asdf.tongchoobe.domain.ExcuseAftermath;
import com.asdf.tongchoobe.domain.ExcuseRememberItem;
import com.asdf.tongchoobe.domain.ExcuseRiskFactor;
import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@Schema(description = "변명 생성 결과")
public class ExcuseResponse {
    private Long id;
    private Long parentId;
    private Long replyToExcuseId;
    private String incomingMessage;
    private int roundNumber;
    private String excuse;
    private Target target;
    private Tone tone;
    private AnalysisResponse analysis;
    private List<AftermathResponse> aftermath;
    private List<String> remember;
    private int earnedXp;
    private ComplexityWarningResponse complexityWarning;
    private Instant createdAt;

    public static ExcuseResponse from(
            Excuse excuse,
            List<ExcuseRiskFactor> riskFactors,
            List<ExcuseRememberItem> rememberItems,
            List<ExcuseAftermath> aftermaths,
            ComplexityWarningResponse complexityWarning
    ) {
        return ExcuseResponse.builder()
                .id(excuse.getId())
                .parentId(excuse.getParent() == null ? null : excuse.getParent().getId())
                .replyToExcuseId(excuse.getReplyToExcuse() == null ? null : excuse.getReplyToExcuse().getId())
                .incomingMessage(excuse.getIncomingMessage())
                .roundNumber(excuse.getRoundNumber())
                .excuse(excuse.getExcuseText())
                .target(excuse.getTarget())
                .tone(excuse.getTone())
                .analysis(AnalysisResponse.builder()
                        .successRate(excuse.getSuccessRate())
                        .realism(excuse.getRealism())
                        .persuasion(excuse.getPersuasion())
                        .suspicionLevel(excuse.getSuspicionLevel().name())
                        .riskFactors(riskFactors.stream()
                                .map(ExcuseRiskFactor::getContent)
                                .toList())
                        .build())
                .aftermath(aftermaths.stream()
                        .map(AftermathResponse::from)
                        .toList())
                .remember(rememberItems.stream()
                        .map(ExcuseRememberItem::getContent)
                        .toList())
                .earnedXp(excuse.getEarnedXp())
                .complexityWarning(complexityWarning)
                .createdAt(excuse.getCreatedAt())
                .build();
    }

    @Getter
    @Builder
    public static class AnalysisResponse {
        private int successRate;
        private int realism;
        private int persuasion;
        private String suspicionLevel;
        private List<String> riskFactors;
    }

    @Getter
    @Builder
    public static class AftermathResponse {
        private String when;
        private int dayOffset;
        private String question;
        private int collapseRate;

        private static AftermathResponse from(ExcuseAftermath aftermath) {
            return AftermathResponse.builder()
                    .when(aftermath.getWhenLabel())
                    .dayOffset(aftermath.getDayOffset())
                    .question(aftermath.getQuestion())
                    .collapseRate(aftermath.getCollapseRate())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ComplexityWarningResponse {
        private boolean enabled;
        private String message;
    }
}
