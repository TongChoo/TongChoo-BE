package com.asdf.tongchoobe.llm;

import com.asdf.tongchoobe.config.FastApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import com.asdf.tongchoobe.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void serializesCustomTargetDescriptionForCreateReplyAndEvolve() throws Exception {
        FastApiClient.CreateRequest create = new FastApiClient.CreateRequest(
                "회식 참석이 어렵습니다.", Target.CUSTOM, "회사 부장님", Tone.MILD
        );
        FastApiClient.ReplyRequest reply = new FastApiClient.ReplyRequest(
                "회식 참석이 어렵습니다.", Target.CUSTOM, "회사 부장님", Tone.MILD,
                "개인 사정이 있어 참석이 어렵습니다.",
                "개인적인 부분이라 자세히 말씀드리기 어렵습니다.",
                List.of(), 2, "개인 사정이 뭔가요?"
        );
        FastApiClient.EvolveRequest evolve = new FastApiClient.EvolveRequest(
                "회식 참석이 어렵습니다.", Target.CUSTOM, "회사 부장님", Tone.MILD,
                "개인 사정이 있어 참석이 어렵습니다.",
                "개인적인 부분이라 자세히 말씀드리기 어렵습니다.",
                List.of(), 1, "더 짧게"
        );

        assertEquals("회사 부장님", objectMapper.readTree(objectMapper.writeValueAsString(create))
                .path("targetDescription").asText());
        assertEquals("회사 부장님", objectMapper.readTree(objectMapper.writeValueAsString(reply))
                .path("targetDescription").asText());
        assertEquals("회사 부장님", objectMapper.readTree(objectMapper.writeValueAsString(evolve))
                .path("targetDescription").asText());
    }

    @Test
    void mapsReplyQualityRejectionWithoutExposingLlmParseError() {
        FastApiClient client = new FastApiClient(
                RestClient.builder(),
                new FastApiProperties("http://localhost", "", 100, 100)
        );

        assertEquals(
                ErrorCode.REPLY_QUALITY_REJECTED,
                client.map(422, "REPLY_QUALITY_REJECTED").getErrorCode()
        );
    }

    @Test
    void preservesKoreanParticleBoundToTimeExpression() throws Exception {
        FastApiClient.GeneratedExcuse result = objectMapper.readValue(
                """
                        {
                          "excuse": "오늘은 회식 참석이 어렵습니다.",
                          "replyOptions": [
                            "오늘은 회식 참석이 어렵습니다.",
                            "오늘은 참석이 어려운 점 양해 부탁드립니다.",
                            "오늘은 개인적인 부분이라 자세히 말씀드리기 어렵습니다."
                          ]
                        }
                        """,
                FastApiClient.GeneratedExcuse.class
        );

        assertEquals("오늘은 회식 참석이 어렵습니다.", result.excuseText());
        assertEquals("오늘은 회식 참석이 어렵습니다.", result.replyOptions().getFirst());
    }
}
