package com.accrevent.radius.security;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig
{
    @Value("${cors.allowed.origins}")
    private String[] allowedOrigins;
    @Bean
    public WebMvcConfigurer webMvcConfigurer()
    {
        return new WebMvcConfigurer()
        {
            @Override
            public void addCorsMappings(CorsRegistry registry)
            {
                registry.addMapping("/**") // Apply to all endpoints
                       .allowedOriginPatterns(allowedOrigins) // Allow the specified origin
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        //.allowedHeaders("Authorization", "Cache-Control", "Content-Type")
                        .allowCredentials(true);
            }
        };
    }
}
