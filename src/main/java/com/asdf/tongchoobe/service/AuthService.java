package com.asdf.tongchoobe.service;

import com.asdf.tongchoobe.domain.Grade;
import com.asdf.tongchoobe.domain.User;
import com.asdf.tongchoobe.dto.request.LoginRequest;
import com.asdf.tongchoobe.dto.request.SignupRequest;
import com.asdf.tongchoobe.dto.response.TokenResponse;
import com.asdf.tongchoobe.exception.BusinessException;
import com.asdf.tongchoobe.exception.ErrorCode;
import com.asdf.tongchoobe.repository.UserRepository;
import com.asdf.tongchoobe.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .totalXp(0)
                .grade(Grade.NOVICE)
                .excuseCount(0)
                .build();

        User savedUser = userRepository.save(user);
        return createTokenResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        return createTokenResponse(user);
    }

    private TokenResponse createTokenResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        return TokenResponse.of(accessToken, accessTokenExpiration, user);
    }
}
