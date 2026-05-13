package dev.sanaeb.altforge.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for {@code POST /v1beta/models/{model}:generateContent}.
 *
 * <p>The Gemini API expects {@code snake_case} field names for some keys
 * (notably {@code inline_data} and {@code mime_type}), hence the explicit
 * {@link JsonProperty} annotations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiRequest(
        List<Content> contents,
        GenerationConfig generationConfig) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(List<Part> parts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(
            String text,
            @JsonProperty("inline_data") InlineData inlineData) {

        public static Part text(String text) {
            return new Part(text, null);
        }

        public static Part image(String mimeType, String base64Data) {
            return new Part(null, new InlineData(mimeType, base64Data));
        }
    }

    public record InlineData(
            @JsonProperty("mime_type") String mimeType,
            String data) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerationConfig(
            Double temperature,
            Integer maxOutputTokens) {
    }
}
