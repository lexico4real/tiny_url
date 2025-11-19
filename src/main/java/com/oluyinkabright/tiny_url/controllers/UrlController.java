package com.oluyinkabright.tiny_url.controllers;

import com.oluyinkabright.tiny_url.dto.UrlDto;
import com.oluyinkabright.tiny_url.entities.UrlMapping;
import com.oluyinkabright.tiny_url.services.UrlService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.util.Optional;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final Bucket bucket;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;

        // Rate limiting: 10 requests per minute per IP
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder().addLimit(limit).build();
    }

    @PostMapping("/api/urls")
    public ResponseEntity<?> createShortUrl(
            @Valid @RequestBody UrlDto.CreateRequest request,
            HttpServletRequest servletRequest) {

        // Rate limiting check
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Try again later.");
        }

        Integer expiryDays = null;
        if (request.getExpiryDays() != null) {
            expiryDays = Integer.parseInt(request.getExpiryDays());
        }

        UrlMapping mapping = urlService.createShortUrl(request.getLongUrl(), expiryDays);
        String shortUrl = urlService.buildShortUrl(mapping.getCode());

        UrlDto.CreateResponse response = new UrlDto.CreateResponse(
                mapping.getCode(), shortUrl, mapping.getLongUrl());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/r/{code}")
    public RedirectView redirectToLongUrl(@PathVariable String code) {
        UrlMapping mapping = urlService.resolveAndIncrement(code);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(mapping.getLongUrl());
        redirectView.setStatusCode(HttpStatus.FOUND);
        return redirectView;
    }

    @GetMapping("/api/urls/{code}")
    public ResponseEntity<?> getUrlMetadata(@PathVariable String code) {
        Optional<UrlMapping> mapping = urlService.getUrlMetadata(code);
        if (mapping.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UrlMapping urlMapping = mapping.get();
        UrlDto.MetadataResponse response = new UrlDto.MetadataResponse(
                urlMapping.getCode(),
                urlMapping.getLongUrl(),
                urlMapping.getCreatedAt().toString(),
                urlMapping.getExpiresAt() != null ? urlMapping.getExpiresAt().toString() : null,
                urlMapping.getHitCount()
        );

        return ResponseEntity.ok(response);
    }
}