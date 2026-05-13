package dev.sanaeb.altforge.gemini;

import dev.sanaeb.altforge.gemini.dto.GeminiRequest;
import dev.sanaeb.altforge.gemini.dto.GeminiResponse;
import dev.sanaeb.altforge.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;

/**
 * Calls the Gemini Vision model to produce a WCAG-friendly alt text for an
 * image. Stateless and thread-safe.
 */
@Service
public class GeminiVisionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisionService.class);

    private static final double TEMPERATURE = 0.2;
    private static final int MAX_OUTPUT_TOKENS = 200;

    private final GeminiProperties properties;
    private final RestClient http;

    public GeminiVisionService(GeminiProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.http = builder.baseUrl(properties.baseUrl()).build();
    }

    /**
     * Send an image to Gemini and return the generated alt text in the
     * requested language. Throws {@link GeminiException} on any failure — the
     * caller is expected to map it to a 5xx response.
     */
    public String generateAltText(byte[] imageBytes, String mimeType, Language language) {
        requireConfigured();

        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        GeminiRequest body = new GeminiRequest(
                List.of(new GeminiRequest.Content(List.of(
                        GeminiRequest.Part.text(language.prompt()),
                        GeminiRequest.Part.image(mimeType, base64)))),
                new GeminiRequest.GenerationConfig(TEMPERATURE, MAX_OUTPUT_TOKENS));

        try {
            GeminiResponse response = http.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Gemini returned {} on {}", res.getStatusCode(), req.getURI());
                        throw new GeminiException("Gemini API error: " + res.getStatusCode());
                    })
                    .body(GeminiResponse.class);

            return extractText(response);
        } catch (RestClientException e) {
            log.error("Gemini Vision call failed", e);
            throw new GeminiException("Could not reach Gemini Vision API.", e);
        }
    }

    private void requireConfigured() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new GeminiException(
                    "Gemini API key is not configured. Set GEMINI_API_KEY in your environment.");
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new GeminiException("Gemini returned no candidates.");
        }
        GeminiResponse.Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            throw new GeminiException("Gemini candidate had no content parts.");
        }
        String text = candidate.content().parts().get(0).text();
        if (text == null || text.isBlank()) {
            throw new GeminiException("Gemini returned an empty alt text.");
        }
        return text.trim();
    }
}
