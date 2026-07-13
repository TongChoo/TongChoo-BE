package com.asdf.tongchoobe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 외부 HTTP 호출에 사용하는 RestClient Builder 설정.
 * 현재는 Spring 백엔드가 FastAPI AI 서버를 호출할 때 사용한다.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
