package com.asdf.tongchoobe.dto.request;

import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import com.asdf.tongchoobe.dto.request.validation.ValidTargetDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@ValidTargetDescription
@Schema(description = "변명 생성 요청")
public class ExcuseCreateRequest {
    @NotBlank
    @Size(max = 500)
    @Schema(description = "변명이 필요한 상황", example = "팀 프로젝트 회의에 늦었어")
    private String situation;

    @NotNull
    @Schema(description = "변명을 전달할 대상", example = "TEAM_MEMBER")
    private Target target;

    @Size(max = 100, message = "직접 입력 관계는 100자 이하여야 합니다.")
    @Schema(description = "CUSTOM 대상일 때 사용하는 자연어 관계 설명", example = "회사 부장님")
    private String targetDescription;

    @NotNull
    @Schema(description = "변명 톤", example = "SLICK")
    private Tone tone;

}
