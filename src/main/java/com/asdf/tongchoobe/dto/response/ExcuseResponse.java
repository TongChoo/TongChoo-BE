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
import java.util.ArrayList;
import java.util.Collections;
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
    private List<String> replyOptions;
    private List<ThreadItemResponse> thread;
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
        return from(excuse, riskFactors, rememberItems, aftermaths, complexityWarning, List.of());
    }

    /**
     * 생성 직후 FastAPI가 반환한 답장 후보를 현재 API 응답에 함께 담는다.
     *
     * 후보는 별도 도메인 테이블에 저장하지 않고 생성 응답에서만 전달한다. 따라서 과거
     * Excuse를 재조회하는 API는 빈 목록을 반환하며, 생성 화면은 최신 응답을 바로 표시한다.
     */
    public static ExcuseResponse from(
            Excuse excuse,
            List<ExcuseRiskFactor> riskFactors,
            List<ExcuseRememberItem> rememberItems,
            List<ExcuseAftermath> aftermaths,
            ComplexityWarningResponse complexityWarning,
            List<String> replyOptions
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
                .replyOptions(replyOptions == null ? List.of() : List.copyOf(replyOptions))
                .thread(buildThread(excuse))
                .earnedXp(excuse.getEarnedXp())
                .complexityWarning(complexityWarning)
                .createdAt(excuse.getCreatedAt())
                .build();
    }

    private static List<ThreadItemResponse> buildThread(Excuse current) {
        List<Excuse> lineage = new ArrayList<>();
        Excuse cursor = current;

        while (cursor != null) {
            lineage.add(cursor);
            cursor = cursor.getParent() != null ? cursor.getParent() : cursor.getReplyToExcuse();
        }

        Collections.reverse(lineage);
        return lineage.stream().map(ThreadItemResponse::from).toList();
    }

    @Getter
    @Builder
    public static class ThreadItemResponse {
        private Long id;
        private int roundNumber;
        private String type;
        private String incomingMessage;
        private String excuse;

        private static ThreadItemResponse from(Excuse excuse) {
            String type = excuse.getIncomingMessage() != null
                    ? "REPLY"
                    : excuse.getParent() != null ? "EVOLVE" : "ORIGINAL";

            return ThreadItemResponse.builder()
                    .id(excuse.getId())
                    .roundNumber(excuse.getRoundNumber())
                    .type(type)
                    .incomingMessage(excuse.getIncomingMessage())
                    .excuse(excuse.getExcuseText())
                    .build();
        }
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
