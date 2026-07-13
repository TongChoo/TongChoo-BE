package com.asdf.tongchoobe.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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
}
