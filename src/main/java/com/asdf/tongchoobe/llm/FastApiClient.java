package com.asdf.tongchoobe.llm;

import com.asdf.tongchoobe.config.FastApiProperties;
import com.asdf.tongchoobe.domain.EvolveDirection;
import com.asdf.tongchoobe.domain.SuspicionLevel;
import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import com.asdf.tongchoobe.exception.BusinessException;
import com.asdf.tongchoobe.exception.ErrorCode;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;
import java.util.UUID;

@Component
public class FastApiClient {
    private final RestClient restClient;

    public FastApiClient(RestClient.Builder builder, FastApiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeoutMs());
        factory.setReadTimeout(properties.readTimeoutMs());
        this.restClient = builder.baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + properties.internalToken())
                .build();
    }

    public GeneratedExcuse create(CreateRequest request) {
        return post("/internal/v1/excuses/create", request);
    }

    public GeneratedExcuse evolve(EvolveRequest request) {
        return post("/internal/v1/excuses/evolve", request);
    }

    public GeneratedExcuse reply(ReplyRequest request) {
        return post("/internal/v1/excuses/reply", request);
    }

    private GeneratedExcuse post(String path, Object request) {
        try {
            return restClient.post().uri(path)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (ignored, response) -> {
                        throw new FastApiException(response.getStatusCode().value());
                    })
                    .body(GeneratedExcuse.class);
        } catch (FastApiException exception) {
            throw map(exception.status);
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.LLM_UNAVAILABLE);
        }
    }

    private BusinessException map(int status) {
        return switch (status) {
            case 401 -> new BusinessException(ErrorCode.AI_INTERNAL_TOKEN_INVALID);
            case 402 -> new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED);
            case 422 -> new BusinessException(ErrorCode.LLM_PARSE_ERROR);
            case 429 -> new BusinessException(ErrorCode.AI_RATE_LIMITED);
            case 502, 503 -> new BusinessException(ErrorCode.LLM_UNAVAILABLE);
            default -> new BusinessException(ErrorCode.LLM_UNAVAILABLE);
        };
    }

    public record CreateRequest(String situation, Target target, Tone tone) {}
    public record EvolveRequest(String situation, Target target, Tone tone, String rootExcuse,
                                String currentExcuse, List<ConversationTurn> conversation,
                                int roundNumber, EvolveDirection direction) {}
    public record ReplyRequest(String situation, Target target, Tone tone, String rootExcuse,
                               String currentExcuse, List<ConversationTurn> conversation,
                               int roundNumber, String incomingMessage) {}
    public record ConversationTurn(String role, String message) {}

    public record GeneratedExcuse(
            @JsonAlias({"excuse", "excuse_text"}) String excuseText,
            @JsonAlias("success_rate") int successRate,
            int realism, int persuasion,
            @JsonAlias("suspicion_level") SuspicionLevel suspicionLevel,
            @JsonAlias("risk_factors") List<Item> riskFactors,
            @JsonAlias("remember_items") List<Item> rememberItems,
            List<Aftermath> aftermaths) {
        public List<Item> riskFactors() { return riskFactors == null ? List.of() : riskFactors; }
        public List<Item> rememberItems() { return rememberItems == null ? List.of() : rememberItems; }
        public List<Aftermath> aftermaths() { return aftermaths == null ? List.of() : aftermaths; }
    }
    public record Item(String content, @JsonAlias("sort_order") int sortOrder) {}
    public record Aftermath(
            @JsonAlias({"when", "when_label"}) String whenLabel,
            @JsonAlias("day_offset") int dayOffset,
            String question,
            @JsonAlias("collapse_rate") int collapseRate,
            @JsonAlias("sort_order") int sortOrder) {}

    private static class FastApiException extends RuntimeException {
        private final int status;
        private FastApiException(int status) { this.status = status; }
    }
}
