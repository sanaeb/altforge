package dev.sanaeb.altforge.jobs;

/** In-memory description of one image to be processed by the async worker. */
public record ImagePayload(
        short position,
        String fileName,
        String contentType,
        long sizeBytes,
        byte[] bytes,
        String validationError) {

    public boolean isValid() {
        return validationError == null;
    }
}
