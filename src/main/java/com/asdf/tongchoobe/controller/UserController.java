package com.asdf.tongchoobe.controller;

import com.asdf.tongchoobe.dto.response.ApiResponse;
import com.asdf.tongchoobe.dto.response.RankResponse;
import com.asdf.tongchoobe.security.CustomUserDetails;
import com.asdf.tongchoobe.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final RankService rankService;

    @GetMapping("/me/rank")
    public ResponseEntity<ApiResponse<RankResponse>> getMyRank(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(rankService.getMyRank(userDetails)));
    }
}
