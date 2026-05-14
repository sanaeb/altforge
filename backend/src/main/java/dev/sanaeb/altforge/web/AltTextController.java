package dev.sanaeb.altforge.web;

import dev.sanaeb.altforge.gemini.GeminiException;
import dev.sanaeb.altforge.gemini.GeminiProperties;
import dev.sanaeb.altforge.gemini.GeminiVisionService;
import dev.sanaeb.altforge.lang.Language;
import dev.sanaeb.altforge.web.dto.AltTextResponse;
import dev.sanaeb.altforge.web.dto.BatchAltTextResponse;
import dev.sanaeb.altforge.web.dto.BatchItemResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

/**
 * Entry point for the alt-text generation API.
 */
@RestController
@RequestMapping("/api/alt-text")
public class AltTextController {

    private static final Logger log = LoggerFactory.getLogger(AltTextController.class);

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final int MAX_BATCH_SIZE = 10;

    private final GeminiVisionService gemini;
    private final GeminiProperties geminiProperties;

    public AltTextController(GeminiVisionService gemini, GeminiProperties geminiProperties) {
        this.gemini = gemini;
        this.geminiProperties = geminiProperties;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AltTextResponse> generate(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "lang", defaultValue = "en") String lang) throws IOException {
        validate(image);
        Language language = Language.fromString(lang);

        String altText = gemini.generateAltText(image.getBytes(), image.getContentType(), language);

        AltTextResponse body = new AltTextResponse(
                altText,
                language.iso(),
                geminiProperties.model(),
                image.getOriginalFilename(),
                image.getSize());
        return ResponseEntity.ok(body);
    }

    @PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchAltTextResponse> generateBatch(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(value = "lang", defaultValue = "en") String lang) {
        if (images == null || images.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one image is required.");
        }
        if (images.size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Batch size is limited to " + MAX_BATCH_SIZE + " images.");
        }

        Language language = Language.fromString(lang);
        List<BatchItemResult> items = new ArrayList<>(images.size());
        for (MultipartFile image : images) {
            items.add(processOne(image, language));
        }
        return ResponseEntity.ok(BatchAltTextResponse.of(geminiProperties.model(), items));
    }

    private BatchItemResult processOne(MultipartFile image, Language language) {
        String fileName = image.getOriginalFilename();
        long size = image.getSize();

        if (image.isEmpty()) {
            return BatchItemResult.failure(fileName, size, "empty_file");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return BatchItemResult.failure(fileName, size, "invalid_image");
        }
        if (size > MAX_BYTES) {
            return BatchItemResult.failure(fileName, size, "too_large");
        }

        try {
            String altText = gemini.generateAltText(image.getBytes(), contentType, language);
            return BatchItemResult.success(fileName, altText, language.iso(), size);
        } catch (GeminiException e) {
            log.warn("Gemini call failed for {}: {}", fileName, e.getMessage());
            return BatchItemResult.failure(fileName, size, "gemini_unavailable");
        } catch (IOException e) {
            log.warn("Could not read bytes for {}: {}", fileName, e.getMessage());
            return BatchItemResult.failure(fileName, size, "io_error");
        }
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
