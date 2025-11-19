package com.oluyinkabright.tiny_url.services;

import com.oluyinkabright.tiny_url.entities.UrlMapping;
import com.oluyinkabright.tiny_url.repositories.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class UrlService {

    private final UrlMappingRepository repository;
    private final CodeGenerator codeGenerator;
    private final MetricsService metricsService;

    @Value("${app.short-url.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.url.default-expiry-days:30}")
    private int defaultExpiryDays;

    public UrlService(UrlMappingRepository repository, CodeGenerator codeGenerator,
                      MetricsService metricsService) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.metricsService = metricsService;
    }

    public UrlMapping createShortUrl(String longUrl) {
        return createShortUrl(longUrl, null);
    }

    public UrlMapping createShortUrl(String longUrl, Integer expiryDays) {
        // Check if URL already exists and is not expired
        Optional<UrlMapping> existing = repository.findActiveByLongUrl(longUrl);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Generate unique code
        String code = codeGenerator.generateUniqueCode(repository::existsByCode);

        // Calculate expiry
        Instant expiresAt = null;
        if (expiryDays != null && expiryDays > 0) {
            expiresAt = Instant.now().plus(expiryDays, ChronoUnit.DAYS);
        } else if (defaultExpiryDays > 0) {
            expiresAt = Instant.now().plus(defaultExpiryDays, ChronoUnit.DAYS);
        }

        // Create and save mapping
        UrlMapping mapping = new UrlMapping(code, longUrl, expiresAt);
        return repository.save(mapping);
    }

    public UrlMapping resolveAndIncrement(String code) {
        Optional<UrlMapping> mapping = repository.findByCode(code);
        if (mapping.isEmpty()) {
            metricsService.incrementRedirectCounter("not_found");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short URL not found");
        }

        UrlMapping urlMapping = mapping.get();
        if (urlMapping.isExpired()) {
            metricsService.incrementRedirectCounter("expired");
            throw new ResponseStatusException(HttpStatus.GONE, "Short URL has expired");
        }

        urlMapping.incrementHitCount();
        repository.save(urlMapping);
        metricsService.incrementRedirectCounter("success");

        return urlMapping;
    }

    public Optional<UrlMapping> getUrlMetadata(String code) {
        return repository.findByCode(code);
    }

    public String buildShortUrl(String code) {
        return baseUrl + "/r/" + code;
    }
}