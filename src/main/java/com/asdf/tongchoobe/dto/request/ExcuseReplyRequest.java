package com.asdf.tongchoobe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "답장 준비 요청")
public class ExcuseReplyRequest {
    @NotBlank(message = "상대방의 답장 내용을 입력해주세요.")
    @Size(max = 500, message = "답장 내용은 500자 이하여야 합니다.")
    @Schema(description = "상대방이 실제로 보낸 답장", example = "근데 그럼 왜 미리 연락 안 했어?")
    private String incomingMessage;

    @Size(min = 1, max = 1000, message = "선택한 답장은 1000자 이하여야 합니다.")
    @Schema(description = "사용자가 후보 중 선택한 현재 답장", example = "맞아요. 지금 바로 수정해서 올릴게요.")
    private String currentExcuse;
}
