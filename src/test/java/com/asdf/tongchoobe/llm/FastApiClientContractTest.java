package com.asdf.tongchoobe.llm;

import com.asdf.tongchoobe.domain.SituationSeverity;
import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import com.asdf.tongchoobe.config.FastApiProperties;
import com.asdf.tongchoobe.exception.BusinessException;
import com.asdf.tongchoobe.exception.ErrorCode;
import com.asdf.tongchoobe.exception.GlobalExceptionHandler;
import com.asdf.tongchoobe.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FastAPI의 camelCase 응답이 Spring의 내부 DTO에 손실 없이 매핑되는지 검증한다.
 *
 * Spring ApplicationContext나 DB를 띄우지 않으므로, AI 응답 계약만 빠르게 회귀
 * 테스트할 수 있다. 특히 REPLY의 replyOptions는 프론트에 그대로 전달돼야 한다.
 */
class FastApiClientContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsFastApiReplyOptionsAndCamelCaseAnalysisFields() throws Exception {
        String responseBody = """
                {
                  "excuse": "사전 공유를 놓친 제 책임입니다. 오늘 일정 영향부터 정리해 바로 공유드리겠습니다.",
                  "successRate": 71,
                  "realism": 4,
                  "persuasion": 5,
                  "suspicionLevel": "MEDIUM",
                  "situationSeverity": "SERIOUS",
                  "replyOptions": [
                    "지금 일정부터 확인하겠습니다.",
                    "미리 공유하지 못해 죄송합니다. 오늘 일정 영향부터 정리해 바로 공유드리겠습니다.",
                    "이번엔 제 일정 관리가 졌네요. 바로 정리해서 만회하겠습니다."
                  ],
                  "riskFactors": [{"content": "일정 지연", "sortOrder": 0}],
                  "rememberItems": [{"content": "사전 공유", "sortOrder": 0}]
                }
                """;

        FastApiClient.GeneratedExcuse result = objectMapper.readValue(
                responseBody,
                FastApiClient.GeneratedExcuse.class
        );

        assertEquals(71, result.successRate());
        assertEquals(4, result.realism());
        assertEquals(5, result.persuasion());
        assertEquals(SituationSeverity.SERIOUS, result.situationSeverity());
        assertEquals(
                List.of(
                        "지금 일정부터 확인하겠습니다.",
                        "미리 공유하지 못해 죄송합니다. 오늘 일정 영향부터 정리해 바로 공유드리겠습니다.",
                        "이번엔 제 일정 관리가 졌네요. 바로 정리해서 만회하겠습니다."
                ),
                result.replyOptions()
        );
        assertEquals("일정 지연", result.riskFactors().getFirst().content());
        assertEquals("사전 공유", result.rememberItems().getFirst().content());
    }

    @Test
    void preservesFastApiQualityErrorCodeAndMessage() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/v1/excuses/reply", exchange -> {
            byte[] body = """
                    {"detail":{"code":"REPLY_QUALITY_REJECTED","message":"상황에 맞는 답장 후보를 만들지 못했습니다. 다시 시도해주세요."}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(422, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            FastApiProperties properties = new FastApiProperties(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "",
                    1000,
                    1000
            );
            FastApiClient client = new FastApiClient(RestClient.builder(), properties);
            FastApiClient.ReplyRequest request = new FastApiClient.ReplyRequest(
                    "영어 수행평가 제출 마감을 놓쳤다",
                    Target.TEACHER,
                    null,
                    Tone.SLICK,
                    SituationSeverity.NORMAL,
                    "제출 마감을 놓쳤습니다.",
                    List.of(),
                    2,
                    "왜 시간을 지키지 않았나요?"
            );

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> client.reply(request)
            );

            assertEquals(ErrorCode.LLM_PARSE_ERROR, exception.getErrorCode());
            assertEquals("REPLY_QUALITY_REJECTED", exception.getDetailCode());
            assertEquals(
                    "상황에 맞는 답장 후보를 만들지 못했습니다. 다시 시도해주세요.",
                    exception.getMessage()
            );

            ResponseEntity<ApiResponse<?>> response =
                    new GlobalExceptionHandler().handleBusinessException(exception);
            assertEquals(422, response.getStatusCode().value());
            assertEquals(exception.getMessage(), response.getBody().getMessage());
            assertEquals(
                    "REPLY_QUALITY_REJECTED",
                    ((Map<?, ?>) response.getBody().getData()).get("code")
            );
        } finally {
            server.stop(0);
        }
    }
}
