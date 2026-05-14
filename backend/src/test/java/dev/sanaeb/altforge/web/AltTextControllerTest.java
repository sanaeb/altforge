package dev.sanaeb.altforge.web;

import dev.sanaeb.altforge.audit.RequestAuditService;
import dev.sanaeb.altforge.gemini.GeminiException;
import dev.sanaeb.altforge.gemini.GeminiProperties;
import dev.sanaeb.altforge.gemini.GeminiVisionService;
import dev.sanaeb.altforge.lang.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AltTextController.class)
class AltTextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiVisionService geminiVisionService;

    @MockitoBean
    private GeminiProperties geminiProperties;

    @MockitoBean
    private RequestAuditService requestAuditService;

    @Test
    @DisplayName("Should return 200 with generated alt text for a valid image upload")
    void returnsGeneratedAltText() throws Exception {
        given(geminiVisionService.generateAltText(any(byte[].class), eq("image/jpeg"), eq(Language.EN)))
                .willReturn("A red apple on a wooden table.");
        given(geminiProperties.model()).willReturn("gemini-2.0-flash");

        MockMultipartFile image = new MockMultipartFile(
                "image", "apple.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/api/alt-text").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.altText").value("A red apple on a wooden table."))
                .andExpect(jsonPath("$.model").value("gemini-2.0-flash"))
                .andExpect(jsonPath("$.fileName").value("apple.jpg"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.sizeBytes").value("fake-bytes".getBytes().length));
    }

    @Test
    @DisplayName("Should generate French alt text when lang=fr is requested")
    void returnsFrenchAltText() throws Exception {
        given(geminiVisionService.generateAltText(any(byte[].class), eq("image/jpeg"), eq(Language.FR)))
                .willReturn("Une pomme rouge posée sur une table en bois.");
        given(geminiProperties.model()).willReturn("gemini-2.0-flash");

        MockMultipartFile image = new MockMultipartFile(
                "image", "pomme.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/api/alt-text").file(image).param("lang", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.altText").value("Une pomme rouge posée sur une table en bois."))
                .andExpect(jsonPath("$.language").value("fr"));
    }

    @Test
    @DisplayName("Should return 400 when the uploaded image is empty")
    void rejectsEmptyImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "empty.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/alt-text").file(image))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when the uploaded file is not an image")
    void rejectsNonImageContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "report.pdf", "application/pdf", "fake-pdf".getBytes());

        mockMvc.perform(multipart("/api/alt-text").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 502 with a clear payload when Gemini fails")
    void returnsBadGatewayWhenGeminiFails() throws Exception {
        given(geminiVisionService.generateAltText(any(byte[].class), any(), any()))
                .willThrow(new GeminiException("Gemini API error: 500"));

        MockMultipartFile image = new MockMultipartFile(
                "image", "apple.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/api/alt-text").file(image))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("gemini_unavailable"))
                .andExpect(jsonPath("$.message").value("Gemini API error: 500"));
    }

    @Test
    @DisplayName("Batch: should return 200 with per-image results when all succeed")
    void returnsBatchResultsWhenAllSucceed() throws Exception {
        given(geminiVisionService.generateAltText(any(byte[].class), eq("image/jpeg"), eq(Language.EN)))
                .willReturn("A red apple.", "A green pear.", "A yellow banana.");
        given(geminiProperties.model()).willReturn("gemini-2.0-flash");

        MockMultipartFile a = new MockMultipartFile("images", "a.jpg", "image/jpeg", "a-bytes".getBytes());
        MockMultipartFile b = new MockMultipartFile("images", "b.jpg", "image/jpeg", "b-bytes".getBytes());
        MockMultipartFile c = new MockMultipartFile("images", "c.jpg", "image/jpeg", "c-bytes".getBytes());

        mockMvc.perform(multipart("/api/alt-text/batch").file(a).file(b).file(c))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("gemini-2.0-flash"))
                .andExpect(jsonPath("$.succeeded").value(3))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.items[0].fileName").value("a.jpg"))
                .andExpect(jsonPath("$.items[0].altText").value("A red apple."))
                .andExpect(jsonPath("$.items[0].error").doesNotExist())
                .andExpect(jsonPath("$.items[2].altText").value("A yellow banana."));
    }

    @Test
    @DisplayName("Batch: should report a per-item failure without breaking other items")
    void reportsPartialFailure() throws Exception {
        given(geminiVisionService.generateAltText(any(byte[].class), eq("image/jpeg"), eq(Language.EN)))
                .willReturn("A red apple.")
                .willThrow(new GeminiException("Gemini API error: 429"));
        given(geminiProperties.model()).willReturn("gemini-2.0-flash");

        MockMultipartFile ok = new MockMultipartFile("images", "ok.jpg", "image/jpeg", "ok-bytes".getBytes());
        MockMultipartFile broken = new MockMultipartFile("images", "broken.jpg", "image/jpeg", "broken-bytes".getBytes());

        mockMvc.perform(multipart("/api/alt-text/batch").file(ok).file(broken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.items[0].altText").value("A red apple."))
                .andExpect(jsonPath("$.items[1].altText").doesNotExist())
                .andExpect(jsonPath("$.items[1].error").value("gemini_unavailable"));
    }

    @Test
    @DisplayName("Batch: should flag a non-image file as invalid_image without calling Gemini for it")
    void flagsInvalidImageInBatch() throws Exception {
        given(geminiVisionService.generateAltText(any(byte[].class), eq("image/jpeg"), eq(Language.EN)))
                .willReturn("A red apple.");
        given(geminiProperties.model()).willReturn("gemini-2.0-flash");

        MockMultipartFile image = new MockMultipartFile("images", "a.jpg", "image/jpeg", "a-bytes".getBytes());
        MockMultipartFile pdf = new MockMultipartFile("images", "doc.pdf", "application/pdf", "pdf-bytes".getBytes());

        mockMvc.perform(multipart("/api/alt-text/batch").file(image).file(pdf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.items[1].error").value("invalid_image"));
    }

    @Test
    @DisplayName("Batch: should return 400 when more than 10 images are uploaded")
    void rejectsBatchOverLimit() throws Exception {
        var request = multipart("/api/alt-text/batch");
        for (int i = 0; i < 11; i++) {
            request = request.file(new MockMultipartFile(
                    "images", "img-" + i + ".jpg", "image/jpeg", ("bytes-" + i).getBytes()));
        }

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }
}
