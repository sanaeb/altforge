package dev.sanaeb.altforge.config;

import dev.sanaeb.altforge.audit.RequestAuditInterceptor;
import dev.sanaeb.altforge.ratelimit.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web-layer configuration: CORS, audit and rate-limit interceptor wiring.
 *
 * <p>Allowed origin patterns are configurable via the
 * {@code altforge.cors.allowed-origin-patterns} property (or the
 * {@code CORS_ALLOWED_ORIGIN_PATTERNS} environment variable in production).
 * Defaults cover the local Vite dev servers; production deployments should
 * extend the list with the public front-end URL.
 *
 * <p>The audit interceptor is registered first so its {@code afterCompletion}
 * still fires when the rate-limit interceptor short-circuits a request — 429
 * responses end up in {@code request_audits} with {@code error_code =
 * rate_limited}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] AUDITED_PATHS = {"/api/alt-text", "/api/alt-text/batch", "/api/alt-text/batch/async"};

    private final String[] allowedOriginPatterns;
    private final RequestAuditInterceptor auditInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(
            @Value("${altforge.cors.allowed-origin-patterns:http://localhost:[*],http://127.0.0.1:[*]}")
            String[] allowedOriginPatterns,
            RequestAuditInterceptor auditInterceptor,
            RateLimitInterceptor rateLimitInterceptor) {
        this.allowedOriginPatterns = allowedOriginPatterns;
        this.auditInterceptor = auditInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor).addPathPatterns(AUDITED_PATHS);
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns(AUDITED_PATHS);
    }
}
