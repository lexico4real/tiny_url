package com.oluyinkabright.tiny_url.controllers;

import com.oluyinkabright.tiny_url.entities.UrlMapping;
import com.oluyinkabright.tiny_url.services.UrlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlService urlService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public UrlService urlService() {
            return mock(UrlService.class);
        }
    }

    @Test
    void createShortUrl_shouldReturnCreatedResponse() throws Exception {
        String longUrl = "https://example.com";
        String code = "abc123";
        String shortUrl = "http://localhost:8080/r/abc123";

        UrlMapping mapping = new UrlMapping(code, longUrl, null);
        mapping.setId(1L);

        when(urlService.createShortUrl(anyString(), any())).thenReturn(mapping);
        when(urlService.buildShortUrl(code)).thenReturn(shortUrl);

        CreateUrlRequest request = new CreateUrlRequest(longUrl);
        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.shortUrl").value(shortUrl))
                .andExpect(jsonPath("$.longUrl").value(longUrl));
    }

    // Helper class to avoid self-reference in anonymous classes
    static class CreateUrlRequest {
        public String longUrl;

        public CreateUrlRequest(String longUrl) {
            this.longUrl = longUrl;
        }
    }

    @Test
    void createShortUrl_shouldReturnBadRequestForInvalidUrl() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("invalid-url");
        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redirectToLongUrl_shouldRedirectForValidCode() throws Exception {
        String code = "abc123";
        String longUrl = "https://example.com";
        UrlMapping mapping = new UrlMapping(code, longUrl, null);

        when(urlService.resolveAndIncrement(code)).thenReturn(mapping);

        mockMvc.perform(get("/r/{code}", code))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(longUrl));
    }

    @Test
    void redirectToLongUrl_shouldReturnNotFoundForInvalidCode() throws Exception {
        String code = "invalid";

        when(urlService.resolveAndIncrement(code))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/r/{code}", code))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUrlMetadata_shouldReturnMetadataForValidCode() throws Exception {
        String code = "abc123";
        String longUrl = "https://example.com";
        UrlMapping mapping = new UrlMapping(code, longUrl, null);
        mapping.setCreatedAt(Instant.now());
        mapping.setHitCount(5L);

        when(urlService.getUrlMetadata(code)).thenReturn(Optional.of(mapping));

        mockMvc.perform(get("/api/urls/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.longUrl").value(longUrl))
                .andExpect(jsonPath("$.hitCount").value(5L));
    }

    @Test
    void getUrlMetadata_shouldReturnNotFoundForInvalidCode() throws Exception {
        String code = "invalid";

        when(urlService.getUrlMetadata(code)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/urls/{code}", code))
                .andExpect(status().isNotFound());
    }
}