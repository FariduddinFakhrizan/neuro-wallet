package com.neurogine.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Configures CORS and static resource handling for file uploads.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * Serve uploaded files as static resources
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourcePath = java.nio.file.Paths.get(uploadDir).toUri().toString();
        if (!resourcePath.endsWith("/")) {
            resourcePath += "/";
        }
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(resourcePath);
    }
}
