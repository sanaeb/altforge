package dev.sanaeb.altforge.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Gemini Vision integration. Values are bound from the
 * {@code altforge.gemini.*} keys of {@code application.properties}.
 *
 * @param apiKey         Google AI Studio API key (read from the
 *                       {@code GEMINI_API_KEY} environment variable)
 * @param model          Gemini model identifier, e.g. {@code gemini-2.0-flash}
 * @param baseUrl        base URL of the Generative Language API
 * @param timeoutSeconds request timeout for outbound calls
 */
@ConfigurationProperties(prefix = "altforge.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        int timeoutSeconds) {
}
