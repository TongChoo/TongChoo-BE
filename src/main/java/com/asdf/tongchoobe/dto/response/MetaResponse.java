package com.asdf.tongchoobe.dto.response;

import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MetaResponse {
    private List<OptionResponse> targets;
    private List<OptionResponse> tones;

    public static MetaResponse create() {
        return MetaResponse.builder()
                .targets(List.of(
                        OptionResponse.of(Target.TEACHER.name(), "선생님"),
                        OptionResponse.of(Target.PARENT.name(), "부모님"),
                        OptionResponse.of(Target.FRIEND.name(), "친구"),
                        OptionResponse.of(Target.LOVER.name(), "연인"),
                        OptionResponse.of(Target.TEAM_LEAD.name(), "팀장"),
                        OptionResponse.of(Target.TEAM_MEMBER.name(), "팀원"),
                        OptionResponse.of(Target.CUSTOM.name(), "직접 입력")
                ))
                .tones(List.of(
                        OptionResponse.of(Tone.MILD.name(), "순한맛"),
                        OptionResponse.of(Tone.SLICK.name(), "능글맞은맛"),
                        OptionResponse.of(Tone.DESPERATE.name(), "목숨 걸기"),
                        OptionResponse.of(Tone.BULLSHIT.name(), "개소리 모드")
                ))
                .build();
    }

    @Getter
    @Builder
    public static class OptionResponse {
        private String code;
        private String label;

        public static OptionResponse of(String code, String label) {
            return OptionResponse.builder()
                    .code(code)
                    .label(label)
                    .build();
        }
    }
}
