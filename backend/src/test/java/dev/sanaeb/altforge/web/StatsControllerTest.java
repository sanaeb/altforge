package dev.sanaeb.altforge.web;

import dev.sanaeb.altforge.audit.RequestAuditRepository;
import dev.sanaeb.altforge.audit.RequestAuditService;
import dev.sanaeb.altforge.gemini.GeminiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestAuditRepository repository;

    @MockitoBean
    private RequestAuditService requestAuditService;

    @MockitoBean
    private GeminiProperties geminiProperties;

    @MockitoBean
    private dev.sanaeb.altforge.ratelimit.RateLimitProperties rateLimitProperties;

    @Test
    @DisplayName("Should aggregate counts, success rate, average latency and breakdowns")
    void aggregatesAllFields() throws Exception {
        given(repository.countByCreatedAtAfter(any(OffsetDateTime.class))).willReturn(10L);
        given(repository.countByCreatedAtAfterAndStatusCodeLessThan(any(OffsetDateTime.class), eq((short) 400)))
                .willReturn(9L);
        given(repository.averageLatencyMsSince(any(OffsetDateTime.class))).willReturn(742.5);
        given(repository.countByLanguageSince(any(OffsetDateTime.class)))
                .willReturn(List.of(new Object[]{"en", 7L}, new Object[]{"fr", 3L}));
        given(repository.countByEndpointSince(any(OffsetDateTime.class)))
                .willReturn(List.of(new Object[]{"single", 8L}, new Object[]{"batch", 2L}));

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(10))
                .andExpect(jsonPath("$.succeededRequests").value(9))
                .andExpect(jsonPath("$.failedRequests").value(1))
                .andExpect(jsonPath("$.successRatePct").value(90.0))
                .andExpect(jsonPath("$.avgLatencyMs").value(742.5))
                .andExpect(jsonPath("$.byLanguage.en").value(7))
                .andExpect(jsonPath("$.byLanguage.fr").value(3))
                .andExpect(jsonPath("$.byEndpoint.single").value(8))
                .andExpect(jsonPath("$.byEndpoint.batch").value(2))
                .andExpect(jsonPath("$.windowHours").value(24));
    }

    @Test
    @DisplayName("Should return zeros and empty breakdowns when the window is empty")
    void returnsEmptyWindow() throws Exception {
        given(repository.countByCreatedAtAfter(any(OffsetDateTime.class))).willReturn(0L);
        given(repository.countByCreatedAtAfterAndStatusCodeLessThan(any(OffsetDateTime.class), eq((short) 400)))
                .willReturn(0L);
        given(repository.averageLatencyMsSince(any(OffsetDateTime.class))).willReturn(null);
        given(repository.countByLanguageSince(any(OffsetDateTime.class))).willReturn(List.of());
        given(repository.countByEndpointSince(any(OffsetDateTime.class))).willReturn(List.of());

        mockMvc.perform(get("/api/stats").param("hours", "168"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(0))
                .andExpect(jsonPath("$.successRatePct").value(0.0))
                .andExpect(jsonPath("$.avgLatencyMs").doesNotExist())
                .andExpect(jsonPath("$.windowHours").value(168));
    }
}
