package com.asdf.tongchoobe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "답장 후보 선택 저장 요청")
public class ExcuseSelectionRequest {
    @NotBlank(message = "선택한 답장을 입력해주세요.")
    @Size(max = 1000, message = "선택한 답장은 1000자 이하여야 합니다.")
    @Schema(description = "AI가 생성한 후보 중 사용자가 선택한 문장")
    private String selectedExcuse;
}
