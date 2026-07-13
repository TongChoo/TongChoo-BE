package com.asdf.tongchoobe.dto.request;

import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "변명 생성 요청")
public class ExcuseCreateRequest {
    @NotBlank
    @Size(max = 500)
    @Schema(description = "변명이 필요한 상황", example = "팀 프로젝트 회의에 늦었어")
    private String situation;

    @NotNull
    @Schema(description = "변명을 전달할 대상", example = "TEAM_MEMBER")
    private Target target;

    @Size(max = 100)
    @Schema(description = "target이 CUSTOM일 때 직접 입력한 상대 설명", example = "같은 프로젝트를 진행하는 친한 선배")
    private String targetDescription;

    @NotNull
    @Schema(description = "변명 톤", example = "SLICK")
    private Tone tone;

    @AssertTrue(message = "기타 상대를 선택하면 상대방 설명을 입력해주세요.")
    public boolean isCustomTargetValid() {
        return target != Target.CUSTOM || (targetDescription != null && !targetDescription.isBlank());
    }
}
