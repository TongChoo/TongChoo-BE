package com.asdf.tongchoobe.controller;

import com.asdf.tongchoobe.dto.response.ApiResponse;
import com.asdf.tongchoobe.dto.response.MetaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @GetMapping
    public ResponseEntity<ApiResponse<MetaResponse>> getMeta() {
        return ResponseEntity.ok(ApiResponse.success(MetaResponse.create()));
    }
}
