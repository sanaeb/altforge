package dev.sanaeb.altforge.audit;

import dev.sanaeb.altforge.gemini.GeminiProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Captures one {@link RequestAudit} per call to the audited endpoints. The
 * controller is expected to populate a few request attributes during processing
 * ({@code audit.imagesCount}, {@code audit.totalBytes}, {@code audit.errorCode});
 * everything else (latency, status, IP, language, model) is derived here so
 * the controller stays focused on its happy path.
 */
@Component
public class RequestAuditInterceptor implements HandlerInterceptor {

    public static final String ATTR_START_NANOS = "audit.startNanos";
    public static final String ATTR_IMAGES_COUNT = "audit.imagesCount";
    public static final String ATTR_TOTAL_BYTES = "audit.totalBytes";
    public static final String ATTR_ERROR_CODE = "audit.errorCode";

    private static final String SINGLE_PATH = "/api/alt-text";
    private static final String BATCH_PATH = "/api/alt-text/batch";

    private final RequestAuditService service;
    private final GeminiProperties geminiProperties;

    public RequestAuditInterceptor(RequestAuditService service, GeminiProperties geminiProperties) {
        this.service = service;
        this.geminiProperties = geminiProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_NANOS, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startedAt = request.getAttribute(ATTR_START_NANOS);
        if (!(startedAt instanceof Long start)) {
            return;
        }
        int latencyMs = (int) ((System.nanoTime() - start) / 1_000_000L);

        String endpoint = resolveEndpoint(request);
        if (endpoint == null) {
            return;
        }

        String language = request.getParameter("lang");
        int imagesCount = readInt(request, ATTR_IMAGES_COUNT);
        long totalBytes = readLong(request, ATTR_TOTAL_BYTES);
        String errorCode = (String) request.getAttribute(ATTR_ERROR_CODE);
        if (errorCode == null && response.getStatus() >= 400) {
            errorCode = "http_" + response.getStatus();
        }

        service.record(
                endpoint,
                RequestAuditService.resolveClientIp(request),
                language == null ? "" : language,
                imagesCount,
                totalBytes,
                response.getStatus(),
                latencyMs,
                geminiProperties.model(),
                errorCode);
    }

    private static String resolveEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (BATCH_PATH.equals(path)) {
            return "batch";
        }
        if (SINGLE_PATH.equals(path)) {
            return "single";
        }
        return null;
    }

    private static int readInt(HttpServletRequest request, String attribute) {
        Object v = request.getAttribute(attribute);
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static long readLong(HttpServletRequest request, String attribute) {
        Object v = request.getAttribute(attribute);
        return v instanceof Number n ? n.longValue() : 0L;
    }
}
