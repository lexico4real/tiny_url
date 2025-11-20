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
        String longUrl = "https://tiny-url.com/" + System.currentTimeMillis();
        String requestBody = String.format("{\"longUrl\": \"%s\"}", longUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> createResponse = restTemplate.postForEntity("/api/urls", request, String.class);
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode(), "Should create URL successfully");

        JsonNode jsonResponse = objectMapper.readTree(createResponse.getBody());
        String code = jsonResponse.get("code").asText();
        
        assertNotNull(code, "Should extract code from response");

        Thread.sleep(100);

        ResponseEntity<String> metadataResponse = restTemplate.getForEntity("/api/urls/" + code, String.class);
       
        if (metadataResponse.getStatusCode() != HttpStatus.OK) {
            fail("URL metadata should be accessible. Got: " + metadataResponse.getStatusCode());
        }

        ResponseEntity<Void> redirectResponse = restTemplate.exchange(
                "/r/" + code,
                HttpMethod.GET,
                null,
                Void.class
        );
        
        assertEquals(HttpStatus.OK, redirectResponse.getStatusCode(),
                "Redirect should return 200 FOUND. If this fails, check your redirect controller implementation.");
    }

    @Test
    void createShortUrl_shouldValidateInput() {
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
}