package com.asdf.tongchoobe.llm;

import com.asdf.tongchoobe.config.FastApiProperties;
import com.asdf.tongchoobe.domain.SuspicionLevel;
import com.asdf.tongchoobe.domain.SituationSeverity;
import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import com.asdf.tongchoobe.exception.BusinessException;
import com.asdf.tongchoobe.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FastApiClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FastApiClient(RestClient.Builder builder, FastApiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeoutMs());
        factory.setReadTimeout(properties.readTimeoutMs());
        this.restClient = builder.baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
        this.internalToken = properties.internalToken();
    }

    private final String internalToken;

    public GeneratedExcuse create(CreateRequest request) {
        return post("/internal/v1/excuses/create", request);
    }

    public GeneratedExcuse reply(ReplyRequest request) {
        return post("/internal/v1/excuses/reply", request);
    }

    private GeneratedExcuse post(String path, Object request) {
        try {
            RestClient.RequestBodySpec spec = restClient.post().uri(path)
                    .header("X-Request-ID", UUID.randomUUID().toString());

            if (internalToken != null && !internalToken.isBlank()) {
                spec.header("Authorization", "Bearer " + internalToken);
            }

            return spec.body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (ignored, response) -> {
                        throw readFastApiException(
                                response.getStatusCode().value(),
                                response.getBody()
                        );
                    })
                    .body(GeneratedExcuse.class);
        } catch (FastApiException exception) {
            throw map(exception);
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.LLM_UNAVAILABLE);
        }
    }

    private FastApiException readFastApiException(int status, java.io.InputStream body) {
        try {
            FastApiErrorResponse response = objectMapper.readValue(body, FastApiErrorResponse.class);
            if (response.detail() != null) {
                return new FastApiException(
                        status,
                        response.detail().code(),
                        response.detail().message()
                );
            }
        } catch (Exception ignored) {
            // FastAPI 오류 본문까지 깨진 경우에는 기존 상태 코드 기반 메시지로 처리한다.
        }
        return new FastApiException(status, null, null);
    }

    private BusinessException map(FastApiException exception) {
        ErrorCode errorCode = switch (exception.status) {
            case 401 -> ErrorCode.AI_INTERNAL_TOKEN_INVALID;
            case 402 -> ErrorCode.AI_QUOTA_EXCEEDED;
            case 422 -> ErrorCode.LLM_PARSE_ERROR;
            case 429 -> ErrorCode.AI_RATE_LIMITED;
            case 502, 503 -> ErrorCode.LLM_UNAVAILABLE;
            case 504 -> ErrorCode.LLM_TIMEOUT;
            default -> ErrorCode.LLM_UNAVAILABLE;
        };
        return new BusinessException(errorCode, exception.aiMessage, exception.aiCode);
    }

    public record CreateRequest(String situation, Target target, String targetDescription, Tone tone) {}
    public record ReplyRequest(String situation, Target target, String targetDescription, Tone tone,
                               SituationSeverity situationSeverity, String currentExcuse,
                               List<ConversationTurn> conversation,
                               int roundNumber, String incomingMessage) {}
    public record ConversationTurn(String role, String content) {}

    public record GeneratedExcuse(
            @JsonAlias({"excuse", "excuse_text"}) String excuseText,
            @JsonAlias({"successRate", "success_rate"}) Integer rawSuccessRate,
            @JsonAlias({"realism", "raw_realism"}) Integer rawRealism,
            @JsonAlias({"persuasion", "raw_persuasion"}) Integer rawPersuasion,
            @JsonAlias({"suspicionLevel", "suspicion_level"}) SuspicionLevel rawSuspicionLevel,
            @JsonAlias({"situationSeverity", "situation_severity"}) SituationSeverity rawSituationSeverity,
            Analysis analysis,
            @JsonAlias({"riskFactors", "risk_factors"}) List<Item> legacyRiskFactors,
            @JsonAlias({"rememberItems", "remember_items"}) List<Item> legacyRememberItems,
            @JsonAlias("remember") List<String> rememberTexts,
            @JsonAlias({"aftermath", "aftermaths"}) List<Aftermath> rawAftermaths,
            @JsonAlias({"replyOptions", "reply_options"}) List<String> rawReplyOptions) {
        public int successRate() {
            return analysis != null ? analysis.successRate() : valueOrDefault(rawSuccessRate, 50);
        }

        public int realism() {
            return analysis != null ? analysis.realism() : valueOrDefault(rawRealism, 3);
        }

        public int persuasion() {
            return analysis != null ? analysis.persuasion() : valueOrDefault(rawPersuasion, 3);
        }

        public SuspicionLevel suspicionLevel() {
            if (analysis != null && analysis.suspicionLevel() != null) {
                return analysis.suspicionLevel();
            }
            return rawSuspicionLevel == null ? SuspicionLevel.MEDIUM : rawSuspicionLevel;
        }

        public SituationSeverity situationSeverity() {
            return rawSituationSeverity == null ? SituationSeverity.NORMAL : rawSituationSeverity;
        }

        public List<Item> riskFactors() {
            if (legacyRiskFactors != null && !legacyRiskFactors.isEmpty()) {
                return legacyRiskFactors;
            }
            if (analysis == null || analysis.riskFactors() == null) {
                return List.of();
            }
            return toItems(analysis.riskFactors());
        }

        public List<Item> rememberItems() {
            if (legacyRememberItems != null && !legacyRememberItems.isEmpty()) {
                return legacyRememberItems;
            }
            return toItems(rememberTexts);
        }

        public List<Aftermath> aftermaths() {
            if (rawAftermaths == null) {
                return List.of();
            }
            List<Aftermath> indexed = new ArrayList<>();
            for (int index = 0; index < rawAftermaths.size(); index++) {
                Aftermath item = rawAftermaths.get(index);
                indexed.add(new Aftermath(
                        item.whenLabel(),
                        item.dayOffset(),
                        item.question(),
                        item.collapseRate(),
                        item.sortOrder() == null ? index : item.sortOrder()
                ));
            }
            return indexed;
        }

        public List<String> replyOptions() {
            if (rawReplyOptions == null) {
                return List.of();
            }
            return rawReplyOptions.stream()
                    .filter(option -> option != null && !option.isBlank())
                    .limit(3)
                    .toList();
        }

        private static int valueOrDefault(Integer value, int defaultValue) {
            return value == null ? defaultValue : value;
        }

        private static List<Item> toItems(List<String> texts) {
            if (texts == null) {
                return List.of();
            }
            List<Item> items = new ArrayList<>();
            for (int index = 0; index < texts.size(); index++) {
                items.add(new Item(texts.get(index), index));
            }
            return items;
        }
    }

    public record Analysis(
            int successRate,
            int realism,
            int persuasion,
            SuspicionLevel suspicionLevel,
            List<String> riskFactors
    ) {}

    public record Item(String content, @JsonAlias("sort_order") int sortOrder) {}
    public record Aftermath(
            @JsonAlias({"when", "when_label"}) String whenLabel,
            @JsonAlias("day_offset") int dayOffset,
            String question,
            @JsonAlias("collapse_rate") int collapseRate,
            @JsonAlias("sort_order") Integer sortOrder) {}

    private static class FastApiException extends RuntimeException {
        private final int status;
        private final String aiCode;
        private final String aiMessage;

        private FastApiException(int status, String aiCode, String aiMessage) {
            this.status = status;
            this.aiCode = aiCode;
            this.aiMessage = aiMessage;
        }
    }

    private record FastApiErrorResponse(FastApiErrorDetail detail) {}
    private record FastApiErrorDetail(String code, String message) {}
}
