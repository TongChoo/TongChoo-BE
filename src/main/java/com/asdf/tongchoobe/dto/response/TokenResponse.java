package com.asdf.tongchoobe.dto.response;

import com.asdf.tongchoobe.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String email;
    private String nickname;

    public static TokenResponse of(String accessToken, long expiresIn, User user) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }
}
