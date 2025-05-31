package com.footArena.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@Nullable CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://luxiergerie.tech", "https://www.luxiergerie.tech", "http://localhost:8081", "http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
                .allowedHeaders("Content-Type", "Date", "Authorization", "Token")
                .allowCredentials(true);
    }
}