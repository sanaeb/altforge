package dev.sanaeb.altforge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web-layer configuration.
 *
 * <p>For local development the Vite dev server typically runs on
 * <code>:5173</code> but falls back to <code>:5174</code>, <code>:5175</code>…
 * when the port is busy (e.g. another project is already serving on
 * <code>:5173</code>). We accept any localhost port via
 * {@link CorsRegistry#allowedOriginPatterns(String...)}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:[*]", "http://127.0.0.1:[*]")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
