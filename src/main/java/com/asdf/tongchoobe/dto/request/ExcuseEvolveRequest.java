package com.asdf.tongchoobe.dto.request;

import com.asdf.tongchoobe.domain.EvolveDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "변명 진화 요청")
public class ExcuseEvolveRequest {
    @NotNull
    @Schema(description = "변명 진화 방향", example = "MORE_PLAUSIBLE")
    private EvolveDirection direction;
}
