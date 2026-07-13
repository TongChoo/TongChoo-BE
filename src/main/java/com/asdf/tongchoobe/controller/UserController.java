package com.asdf.tongchoobe.controller;

import com.asdf.tongchoobe.dto.request.NicknameUpdateRequest;
import com.asdf.tongchoobe.dto.request.PasswordUpdateRequest;
import com.asdf.tongchoobe.dto.response.ApiResponse;
import com.asdf.tongchoobe.dto.response.RankResponse;
import com.asdf.tongchoobe.dto.response.UserProfileResponse;
import com.asdf.tongchoobe.security.CustomUserDetails;
import com.asdf.tongchoobe.service.RankService;
import com.asdf.tongchoobe.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final RankService rankService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile(userDetails)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("닉네임이 변경되었습니다.", userService.updateNickname(userDetails, request)));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordUpdateRequest request
    ) {
        userService.updatePassword(userDetails, request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }

    @GetMapping("/me/rank")
    public ResponseEntity<ApiResponse<RankResponse>> getMyRank(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(rankService.getMyRank(userDetails)));
    }
}
