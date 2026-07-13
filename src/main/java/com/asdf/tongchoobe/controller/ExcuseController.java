package com.asdf.tongchoobe.controller;

import com.asdf.tongchoobe.dto.request.ExcuseCreateRequest;
import com.asdf.tongchoobe.dto.request.ExcuseEvolveRequest;
import com.asdf.tongchoobe.dto.response.ApiResponse;
import com.asdf.tongchoobe.dto.response.ExcuseResponse;
import com.asdf.tongchoobe.dto.response.ExcuseSummaryResponse;
import com.asdf.tongchoobe.dto.response.PageResponse;
import com.asdf.tongchoobe.security.CustomUserDetails;
import com.asdf.tongchoobe.service.ExcuseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/excuses")
public class ExcuseController {
    private final ExcuseService excuseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExcuseResponse>> createExcuse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExcuseCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(excuseService.createExcuse(request, userDetails)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExcuseSummaryResponse>>> getMyExcuses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(excuseService.getMyExcuses(userDetails, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExcuseResponse>> getExcuse(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(excuseService.getExcuse(id, userDetails)));
    }

    @PostMapping("/{id}/evolve")
    public ResponseEntity<ApiResponse<ExcuseResponse>> evolveExcuse(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExcuseEvolveRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(excuseService.evolveExcuse(id, request, userDetails)));
    }
}
