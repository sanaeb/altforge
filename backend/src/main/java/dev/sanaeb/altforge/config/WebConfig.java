package dev.sanaeb.altforge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web-layer configuration.
 *
 * <p>Allowed origin patterns are configurable via the
 * {@code altforge.cors.allowed-origin-patterns} property (or the
 * {@code CORS_ALLOWED_ORIGIN_PATTERNS} environment variable in production).
 * Defaults cover the local Vite dev servers; production deployments should
 * extend the list with the public front-end URL.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    public WebConfig(
            @Value("${altforge.cors.allowed-origin-patterns:http://localhost:[*],http://127.0.0.1:[*]}")
            String[] allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
