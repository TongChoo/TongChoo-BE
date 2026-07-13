package com.asdf.tongchoobe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tongchoo.ai")
public record FastApiProperties(
        String baseUrl,
        String internalToken,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
