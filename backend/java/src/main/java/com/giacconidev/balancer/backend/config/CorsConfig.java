package com.giacconidev.balancer.backend.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
@Configuration
@Profile("dev") // This config only has to be working in development mode
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        // This can't be a real project correct CORS, be careful
        return new WebMvcConfigurer() {
            // We override the CORS properties here to avoid CORS errors
            @Override
            public void addCorsMappings(@SuppressWarnings("null") CorsRegistry registry) {
                registry
                        .addMapping("/**")
                        .allowedMethods(CorsConfiguration.ALL)
                        .allowedHeaders(CorsConfiguration.ALL)
                        .allowedOriginPatterns(CorsConfiguration.ALL);
            }
        };
    }
}