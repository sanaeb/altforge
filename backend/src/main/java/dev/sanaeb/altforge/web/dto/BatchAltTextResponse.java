package dev.sanaeb.altforge.web.dto;

import java.util.List;

/**
 * Result of a batch alt-text generation request. Always returned with HTTP 200
 * when the request itself is valid (well-formed multipart, within size limits);
 * per-image failures are reported through individual {@link BatchItemResult}s.
 *
 * @param model     identifier of the model used for the whole batch
 * @param succeeded number of items that produced an alt text
 * @param failed    number of items that errored out
 * @param items     per-image results, in the order the images were uploaded
 */
public record BatchAltTextResponse(
        String model,
        int succeeded,
        int failed,
        List<BatchItemResult> items) {

    public static BatchAltTextResponse of(String model, List<BatchItemResult> items) {
        int succeeded = (int) items.stream().filter(i -> i.error() == null).count();
        return new BatchAltTextResponse(model, succeeded, items.size() - succeeded, items);
    }
}
