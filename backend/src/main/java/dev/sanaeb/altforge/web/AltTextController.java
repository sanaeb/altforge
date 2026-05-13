package dev.sanaeb.altforge.web;

import dev.sanaeb.altforge.web.dto.AltTextResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

/**
 * Entry point for the alt-text generation API.
 *
 * <p>The current implementation is a stub: it validates the upload but returns
 * a deterministic placeholder instead of calling a vision model. Wiring to the
 * actual provider will come in a later milestone.
 */
@RestController
@RequestMapping("/api/alt-text")
public class AltTextController {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AltTextResponse> generate(@RequestParam("image") MultipartFile image) {
        validate(image);

        AltTextResponse body = new AltTextResponse(
                "[stub] Alt text will appear here once the vision provider is wired.",
                "en",
                "stub-v0",
                image.getOriginalFilename(),
                image.getSize());
        return ResponseEntity.ok(body);
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "An image file is required.");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(BAD_REQUEST, "Uploaded file must be an image.");
        }
        if (image.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Image must be 10 MB or less.");
        }
    }
}
