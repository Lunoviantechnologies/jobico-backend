package com.example.jobico.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Prevent curl/system calls from hanging forever when SMS provider is slow/unreachable.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(8).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());
        return new RestTemplate(factory);
    }
}
