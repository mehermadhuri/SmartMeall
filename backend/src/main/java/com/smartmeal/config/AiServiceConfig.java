package com.smartmeal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiServiceConfig {
    @Bean
    public RestClient aiRestClient(
            @Value("${ai.service.url}") String aiServiceUrl,
            @Value("${ai.service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${ai.service.read-timeout-ms:4000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .baseUrl(normalizeBaseUrl(aiServiceUrl))
                .requestFactory(requestFactory)
                .build();
    }

    static String normalizeBaseUrl(String aiServiceUrl) {
        String baseUrl = aiServiceUrl.trim();
        return baseUrl.contains("://") ? baseUrl : "https://" + baseUrl;
    }
}