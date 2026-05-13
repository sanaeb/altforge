package dev.sanaeb.altforge.web;

import dev.sanaeb.altforge.gemini.GeminiException;
import dev.sanaeb.altforge.gemini.GeminiProperties;
import dev.sanaeb.altforge.gemini.GeminiVisionService;
import dev.sanaeb.altforge.web.dto.AltTextResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

/**
 * Entry point for the alt-text generation API.
 */
@RestController
@RequestMapping("/api/alt-text")
public class AltTextController {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final GeminiVisionService gemini;
    private final GeminiProperties geminiProperties;

    public AltTextController(GeminiVisionService gemini, GeminiProperties geminiProperties) {
        this.gemini = gemini;
        this.geminiProperties = geminiProperties;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AltTextResponse> generate(@RequestParam("image") MultipartFile image) throws IOException {
        validate(image);

        String altText = gemini.generateAltText(image.getBytes(), image.getContentType());

        AltTextResponse body = new AltTextResponse(
                altText,
                "en",
                geminiProperties.model(),
                image.getOriginalFilename(),
                image.getSize());
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<Map<String, String>> handleGeminiFailure(GeminiException e) {
        return ResponseEntity.status(BAD_GATEWAY).body(Map.of(
                "error", "gemini_unavailable",
                "message", e.getMessage()));
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
