package com.asdf.tongchoobe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 모든 컨트롤러가 공통으로 사용하는 응답 래퍼. Backend.md §4.1.
 * 컨트롤러는 항상 ResponseEntity<ApiResponse<T>>를 반환해서 FE가 {status, message, data} 형태를 일관되게 파싱할 수 있게 한다.
 * 생성자를 private으로 막아두고, 상황별 정적 팩토리 메서드로만 만들도록 강제한다.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // data가 null이면 JSON 응답에서 필드 자체를 생략한다
public class ApiResponse<T> {
    private final int status;
    private final String message;
    private final T data;

    private ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // 200 OK, 기본 메시지("성공")
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "성공", data);
    }

    // 200 OK, 메시지를 직접 지정하고 싶을 때 (예: 닉네임 수정 성공 안내 등)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    // 201 Created — 회원가입/변명 생성처럼 새 리소스를 만든 경우
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "생성되었습니다.", data);
    }

    // 204 No Content — 이 프로젝트에는 삭제 API가 없지만 Kiosk-BE 컨벤션을 그대로 유지
    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(204, "삭제되었습니다.", null);
    }

    // 4xx/5xx 에러, data 없이 message만 내려줄 때 (GlobalExceptionHandler에서 주로 사용)
    public static ApiResponse<Void> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }

    // 4xx 에러인데 필드별 에러 상세(Map<String, String> 등)를 data에 함께 담아야 할 때
    // (예: MethodArgumentNotValidException → {필드명: 에러메시지} 맵)
    public static <T> ApiResponse<T> error(int status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }
}
