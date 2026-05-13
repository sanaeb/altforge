package dev.sanaeb.altforge.web;

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
}
