package com.asdf.tongchoobe.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("변기(TongChoo) API")
                        .description("""
                                AI 기반 위기 상황 변명 생성 서비스 백엔드 API

                                1. /api/auth/signup, /api/auth/login, /api/meta는 인증 없이 호출 가능
                                2. 나머지 API는 로그인 후 발급받은 accessToken을 Authorize에 Bearer 토큰으로 입력해야 호출 가능
                                3. 변명 생성/진화/답장은 내부 FastAPI AI 서버와 연동
                                """)
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
