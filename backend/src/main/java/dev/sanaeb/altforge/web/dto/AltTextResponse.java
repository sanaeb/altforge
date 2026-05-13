package dev.sanaeb.altforge.web.dto;

/**
 * Result of an alt-text generation request.
 *
 * @param altText   the generated alt text, intended to be used as the HTML
 *                  <code>alt</code> attribute on an <code>&lt;img&gt;</code> tag
 * @param language  ISO 639-1 code of the language the alt text is written in
 *                  (e.g. {@code "fr"}, {@code "en"})
 * @param model     identifier of the model that produced the description
 * @param fileName  original filename of the image, echoed back for client-side
 *                  pairing
 * @param sizeBytes size of the uploaded image in bytes
 */
public record AltTextResponse(
        String altText,
        String language,
        String model,
        String fileName,
        long sizeBytes) {
}
