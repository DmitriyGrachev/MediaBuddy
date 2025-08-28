package org.hrachov.com.filmproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Эта конфигурация говорит Spring Boot:
        // запросы, начинающиеся с "/static/" (URL-путь)
        // должны мапиться на ресурсы из "classpath:/static/" (расположение в проекте)
        registry
                .addResourceHandler("/static/**") // URL-шаблон
                .addResourceLocations("classpath:/static/"); // Расположение ресурсов в classpath
        // (т.е. src/main/resources/static/)
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
