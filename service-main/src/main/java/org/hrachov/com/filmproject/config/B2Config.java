package org.hrachov.com.filmproject.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class B2Config {

    // Используем стандартные названия для ясности
    @Value("${backblaze.b2.access-key-id}")
    private String accessKeyId;

    @Value("${backblaze.b2.secret-access-key}")
    private String secretAccessKey;

    @Value("${backblaze.b2.endpoint-url}")
    private String endpointUrl;

    @Value("${backblaze.b2.region}")
    private String region;

    @Bean
    public S3Client backblazeS3Client() {
        // 1. Создаем провайдер кредов с ПРАВИЛЬНЫМИ переменными
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );

        // 2. Создаем и настраиваем S3 клиент
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                // 3. Указываем кастомный эндпоинт для Backblaze B2
                .endpointOverride(URI.create(endpointUrl))
                .build();
    }
}
