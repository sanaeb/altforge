package dev.sanaeb.altforge.web.dto;

/**
 * One entry of a batch alt-text response. Either {@code altText} is set
 * (success) or {@code error} is set (failure) — never both.
 *
 * @param fileName  original filename of the uploaded image
 * @param altText   generated alt text, or {@code null} if generation failed
 * @param language  ISO 639-1 code of the language the alt text is written in,
 *                  or {@code null} if generation failed
 * @param sizeBytes size of the uploaded image in bytes
 * @param error     short machine-readable error code (e.g. {@code invalid_image},
 *                  {@code too_large}, {@code gemini_unavailable}), or {@code null}
 *                  on success
 */
public record BatchItemResult(
        String fileName,
        String altText,
        String language,
        long sizeBytes,
        String error) {

    public static BatchItemResult success(String fileName, String altText, String language, long sizeBytes) {
        return new BatchItemResult(fileName, altText, language, sizeBytes, null);
    }

    public static BatchItemResult failure(String fileName, long sizeBytes, String error) {
        return new BatchItemResult(fileName, null, null, sizeBytes, error);
    }
}
