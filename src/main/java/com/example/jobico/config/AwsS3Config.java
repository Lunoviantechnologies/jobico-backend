package com.example.jobico.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
public class AwsS3Config {

    @Bean
    public S3Client s3Client(@Value("${aws.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region.trim()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
