package com.oluyinkabright.tiny_url.services;

import com.oluyinkabright.tiny_url.entities.UrlMapping;
import com.oluyinkabright.tiny_url.repositories.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private CodeGenerator codeGenerator;

    @Mock
    private MetricsService metricsService;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(repository, codeGenerator, metricsService);
    }

    @Test
    void createShortUrl_shouldCreateNewMapping() {
        String longUrl = "https://example.com";
        String code = "abc123";

        when(repository.findActiveByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generateUniqueCode(any())).thenReturn(code);
        when(repository.save(any(UrlMapping.class))).thenAnswer(inv -> {
            UrlMapping mapping = inv.getArgument(0);
            mapping.setId(1L);
            return mapping;
        });

        UrlMapping result = urlService.createShortUrl(longUrl);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals(longUrl, result.getLongUrl());
        verify(repository).save(any(UrlMapping.class));
    }

    @Test
    void createShortUrl_shouldReturnExistingActiveMapping() {
        String longUrl = "https://example.com";
        UrlMapping existing = new UrlMapping("abc123", longUrl, null);

        when(repository.findActiveByLongUrl(longUrl)).thenReturn(Optional.of(existing));

        UrlMapping result = urlService.createShortUrl(longUrl);

        assertSame(existing, result);
        verify(repository, never()).save(any(UrlMapping.class));
    }

    @Test
    void resolveAndIncrement_shouldReturnMappingAndIncrementCount() {
        String code = "abc123";
        String longUrl = "https://example.com";
        UrlMapping mapping = new UrlMapping(code, longUrl, null);

        when(repository.findByCode(code)).thenReturn(Optional.of(mapping));
        when(repository.save(mapping)).thenReturn(mapping);

        UrlMapping result = urlService.resolveAndIncrement(code);

        assertSame(mapping, result);
        assertEquals(1L, mapping.getHitCount());
        verify(repository).save(mapping);
        verify(metricsService).incrementRedirectCounter("success");
    }

    @Test
    void resolveAndIncrement_shouldThrowWhenCodeNotFound() {
        String code = "invalid";

        when(repository.findByCode(code)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> urlService.resolveAndIncrement(code));
        verify(metricsService).incrementRedirectCounter("not_found");
    }

    @Test
    void resolveAndIncrement_shouldThrowWhenExpired() {
        String code = "abc123";
        Instant past = Instant.now().minusSeconds(3600);
        UrlMapping mapping = new UrlMapping(code, "https://example.com", past);

        when(repository.findByCode(code)).thenReturn(Optional.of(mapping));

        assertThrows(ResponseStatusException.class, () -> urlService.resolveAndIncrement(code));
        verify(metricsService).incrementRedirectCounter("expired");
    }
}