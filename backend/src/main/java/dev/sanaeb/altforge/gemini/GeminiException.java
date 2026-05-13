package dev.sanaeb.altforge.gemini;

/**
 * Thrown when the Gemini Vision integration fails (network, quota, malformed
 * response, missing configuration…). Carries a user-safe message that can be
 * surfaced to the client.
 */
public class GeminiException extends RuntimeException {

    public GeminiException(String message) {
        super(message);
    }

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}
