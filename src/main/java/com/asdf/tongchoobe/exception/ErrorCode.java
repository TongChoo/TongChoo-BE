package com.asdf.tongchoobe.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 에러 코드.
 * GlobalExceptionHandler는 이 enum의 HTTP 상태와 메시지를 ApiResponse 에러 응답으로 변환한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),

    EXCUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 변명입니다."),
    EXCUSE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인이 생성한 변명만 조회/진화할 수 있습니다."),
    MAX_REPLY_ROUND_REACHED(HttpStatus.CONFLICT, "최대 10라운드까지만 답장을 준비할 수 있습니다."),

    LLM_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "AI 변명 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),
    LLM_PARSE_ERROR(HttpStatus.UNPROCESSABLE_ENTITY, "AI 응답을 해석하지 못했습니다. 다시 시도해주세요."),
    AI_INTERNAL_TOKEN_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "AI 서버 내부 토큰 설정이 올바르지 않습니다."),
    AI_QUOTA_EXCEEDED(HttpStatus.BAD_GATEWAY, "AI 서비스 결제 또는 quota를 확인해주세요."),
    AI_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AI 사용량 제한에 도달했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}
