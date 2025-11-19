package com.oluyinkabright.tiny_url.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public class UrlDto {

    public static class CreateRequest {
        @NotBlank(message = "URL is required")
        @URL(message = "Invalid URL format")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        private String longUrl;

        @Pattern(regexp = "\\d+", message = "Expiry days must be a positive number")
        private String expiryDays;

        public String getLongUrl() { return longUrl; }
        public void setLongUrl(String longUrl) { this.longUrl = longUrl; }

        public String getExpiryDays() { return expiryDays; }
        public void setExpiryDays(String expiryDays) { this.expiryDays = expiryDays; }
    }

    public static class CreateResponse {
        private final String code;
        private final String shortUrl;
        private final String longUrl;

        public CreateResponse(String code, String shortUrl, String longUrl) {
            this.code = code;
            this.shortUrl = shortUrl;
            this.longUrl = longUrl;
        }

        public String getCode() { return code; }
        public String getShortUrl() { return shortUrl; }
        public String getLongUrl() { return longUrl; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetadataResponse {
        private final String code;
        private final String longUrl;
        private final String createdAt;
        private final String expiresAt;
        private final Long hitCount;

        public MetadataResponse(String code, String longUrl, String createdAt,
                                String expiresAt, Long hitCount) {
            this.code = code;
            this.longUrl = longUrl;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.hitCount = hitCount;
        }

        // Getters
        public String getCode() { return code; }
        public String getLongUrl() { return longUrl; }
        public String getCreatedAt() { return createdAt; }
        public String getExpiresAt() { return expiresAt; }
        public Long getHitCount() { return hitCount; }
    }
}