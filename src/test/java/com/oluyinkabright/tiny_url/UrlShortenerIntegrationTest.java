package com.oluyinkabright.tiny_url;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UrlShortenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.defer-datasource-initialization", () -> "true");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthCheck_shouldReturnOk() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createAndRedirect_shouldWorkEndToEnd() throws Exception {
        // Create short URL
        String longUrl = "https://example.com/" + System.currentTimeMillis();
        String requestBody = String.format("{\"longUrl\": \"%s\"}", longUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> createResponse = restTemplate.postForEntity("/api/urls", request, String.class);

        System.out.println("=== CREATE RESPONSE ===");
        System.out.println("Status: " + createResponse.getStatusCode());
        System.out.println("Body: " + createResponse.getBody());

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode(), "Should create URL successfully");

        // Extract code using JSON parsing
        JsonNode jsonResponse = objectMapper.readTree(createResponse.getBody());
        String code = jsonResponse.get("code").asText();
        String shortUrl = jsonResponse.get("shortUrl").asText();

        System.out.println("Extracted code: " + code);
        System.out.println("Extracted shortUrl: " + shortUrl);

        assertNotNull(code, "Should extract code from response");

        // Add a small delay to ensure the transaction is committed
        Thread.sleep(100);

        // First, verify the URL exists by getting metadata
        ResponseEntity<String> metadataResponse = restTemplate.getForEntity("/api/urls/" + code, String.class);
        System.out.println("=== METADATA RESPONSE ===");
        System.out.println("Status: " + metadataResponse.getStatusCode());
        System.out.println("Body: " + metadataResponse.getBody());

        if (metadataResponse.getStatusCode() != HttpStatus.OK) {
            fail("URL metadata should be accessible. Got: " + metadataResponse.getStatusCode());
        }

        // Now test the redirect
        ResponseEntity<Void> redirectResponse = restTemplate.exchange(
                "/r/" + code,
                HttpMethod.GET,
                null,
                Void.class
        );

        System.out.println("=== REDIRECT RESPONSE ===");
        System.out.println("Status: " + redirectResponse.getStatusCode());
        System.out.println("Location: " + redirectResponse.getHeaders().getLocation());
        System.out.println("All Headers: " + redirectResponse.getHeaders());

        // The key test - should be 302 with Location header
        assertEquals(HttpStatus.FOUND, redirectResponse.getStatusCode(),
                "Redirect should return 302 FOUND. If this fails, check your redirect controller implementation.");
        assertNotNull(redirectResponse.getHeaders().getLocation(),
                "Redirect should have Location header");
        assertEquals(longUrl, redirectResponse.getHeaders().getLocation().toString(),
                "Location header should point to original URL");
    }

    @Test
    void createShortUrl_shouldValidateInput() {
        // Test with invalid URL
        String invalidRequestBody = "{\"longUrl\": \"not-a-valid-url\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(invalidRequestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/urls", request, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void redirect_shouldReturnNotFoundForInvalidCode() {
        ResponseEntity<String> response = restTemplate.getForEntity("/r/invalidcode123", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void debugTransactionIssue() throws Exception {
        // This test isolates the transaction issue
        String longUrl = "https://debug-example.com";
        String requestBody = String.format("{\"longUrl\": \"%s\"}", longUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // Step 1: Create URL
        ResponseEntity<String> createResponse = restTemplate.postForEntity("/api/urls", request, String.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        assertNotNull(createResponse.getBody());
        String code = createResponse.getBody().split("\"code\":\"")[1].split("\"")[0];
        System.out.println("Debug - Created code: " + code);

        // Step 2: Immediately check if metadata is accessible
        ResponseEntity<String> metadataResponse1 = restTemplate.getForEntity("/api/urls/" + code, String.class);
        System.out.println("Debug - Metadata check 1: " + metadataResponse1.getStatusCode());

        // Step 3: Small delay
        Thread.sleep(200);

        // Step 4: Check metadata again
        ResponseEntity<String> metadataResponse2 = restTemplate.getForEntity("/api/urls/" + code, String.class);
        System.out.println("Debug - Metadata check 2: " + metadataResponse2.getStatusCode());

        // Step 5: Try redirect
        ResponseEntity<Void> redirectResponse = restTemplate.exchange("/r/" + code, HttpMethod.GET, null, Void.class);
        System.out.println("Debug - Redirect: " + redirectResponse.getStatusCode());
        System.out.println("Debug - Location: " + redirectResponse.getHeaders().getLocation());

        // If metadata works but redirect doesn't, the issue is in your redirect controller
        if (metadataResponse2.getStatusCode() == HttpStatus.OK && redirectResponse.getStatusCode() != HttpStatus.FOUND) {
            System.err.println("ISSUE: Metadata is accessible but redirect fails. Check your UrlController.redirectToLongUrl method!");
        }
    }
}