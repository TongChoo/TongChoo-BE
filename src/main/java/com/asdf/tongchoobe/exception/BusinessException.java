package com.asdf.tongchoobe.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 표현하는 런타임 예외.
 * 서비스 계층은 HTTP 상태를 직접 다루지 않고 ErrorCode만 던진다.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String detailCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String message, String detailCode) {
        super(message == null || message.isBlank() ? errorCode.getMessage() : message);
        this.errorCode = errorCode;
        this.detailCode = detailCode;
    }
}
