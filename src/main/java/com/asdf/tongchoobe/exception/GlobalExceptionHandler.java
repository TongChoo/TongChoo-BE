package com.asdf.tongchoobe.exception;

import com.asdf.tongchoobe.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 컨트롤러 전역 예외 처리기.
 * 모든 에러 응답을 ApiResponse 형태로 맞춰 FE가 status/message/data를 일관되게 읽도록 한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(status.value(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        HttpStatus status = errorCode.getHttpStatus();

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(status.value(), errorCode.getMessage(), errors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException() {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        HttpStatus status = errorCode.getHttpStatus();

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(status.value(), errorCode.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        HttpStatus status = errorCode.getHttpStatus();

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(status.value(), errorCode.getMessage()));
    }
}
