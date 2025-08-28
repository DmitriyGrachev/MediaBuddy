package com.hrachovblackblaze.blackblazestreamingmicroservice.config;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.exceptions.B2Exception;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.backblaze.b2.client.B2StorageClientFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
public class B2Config {
    @Value("${b2.key-id}")
    private String keyId;
    @Value("${b2.key}")
    private String key;
    @Value("${b2.user-agent:stream-media}") // Пример: значение по умолчанию "your-app-name"
    private String userAgent;

    @Bean
    public B2StorageClient b2StorageClient() throws B2Exception { // Add throws B2Exception here
        // Используем метод create из B2StorageClientFactory
        // Он требует applicationKeyId, applicationKey и userAgent.
        return B2StorageClientFactory.createDefaultFactory().create(keyId, key, userAgent);
    }
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10 seconds
        factory.setReadTimeout(120000);    // 2 minutes
        factory.setBufferRequestBody(false);

        RestTemplate restTemplate = new RestTemplate(factory);

        // Add error handler to avoid exceptions for 4xx and 5xx responses
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                // Only treat 5xx errors as actual errors, not 4xx
                return response.getStatusCode().is5xxServerError();
            }
        });

        return restTemplate;
    }
}
