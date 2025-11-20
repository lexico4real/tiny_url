package com.oluyinkabright.tiny_url.repositories;

import com.oluyinkabright.tiny_url.entities.UrlMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UrlMappingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UrlMappingRepository repository;

    @Test
    void findByCode_shouldReturnUrlMappingWhenExists() {
        UrlMapping mapping = new UrlMapping("abc123", "https://example.com", null);
        entityManager.persistAndFlush(mapping);

        Optional<UrlMapping> found = repository.findByCode("abc123");

        assertTrue(found.isPresent());
        assertEquals("https://example.com", found.get().getLongUrl());
    }

    @Test
    void findByCode_shouldReturnEmptyWhenNotExists() {
        Optional<UrlMapping> found = repository.findByCode("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void findByLongUrl_shouldReturnUrlMappingWhenExists() {
        UrlMapping mapping = new UrlMapping("abc123", "https://example.com", null);
        entityManager.persistAndFlush(mapping);

        Optional<UrlMapping> found = repository.findByLongUrl("https://example.com");

        assertTrue(found.isPresent());
        assertEquals("abc123", found.get().getCode());
    }

    @Test
    void existsByCode_shouldReturnTrueWhenCodeExists() {
        UrlMapping mapping = new UrlMapping("abc123", "https://example.com", null);
        entityManager.persistAndFlush(mapping);

        boolean exists = repository.existsByCode("abc123");
        assertTrue(exists);
    }

    @Test
    void existsByCode_shouldReturnFalseWhenCodeNotExists() {
        boolean exists = repository.existsByCode("nonexistent");
        assertFalse(exists);
    }

    @Test
    void shouldPreventDuplicateCodes() {
        UrlMapping mapping1 = new UrlMapping("abc123", "https://example1.com", null);
        entityManager.persistAndFlush(mapping1);

        UrlMapping mapping2 = new UrlMapping("abc123", "https://example2.com", null);

        Exception exception = assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(mapping2);
        });

        assertTrue(exception instanceof DataIntegrityViolationException ||
                        exception instanceof ConstraintViolationException ||
                        (exception.getCause() instanceof ConstraintViolationException),
                "Expected constraint violation exception but got: " + exception.getClass().getName());
    }

    @Test
    void findActiveByLongUrl_shouldReturnNonExpiredMapping() {
        Instant future = Instant.now().plusSeconds(3600 * 24);
        UrlMapping mapping = new UrlMapping("test123", "https://future-example.com", future);

        entityManager.persistAndFlush(mapping);
        entityManager.clear();

        Optional<UrlMapping> found = repository.findActiveByLongUrl("https://future-example.com");

        assertTrue(found.isPresent(), "Should find active mapping with future expiry");
        assertEquals("test123", found.get().getCode());
    }

    @Test
    void findActiveByLongUrl_shouldNotReturnExpiredMapping() {
        Instant past = Instant.now().minusSeconds(3600 * 24); // 1 day in past
        UrlMapping mapping = new UrlMapping("exp123", "https://past-example.com", past);

        entityManager.persistAndFlush(mapping);
        entityManager.clear();

        Optional<UrlMapping> found = repository.findActiveByLongUrl("https://past-example.com");

        assertFalse(found.isPresent(), "Should not find expired mapping");
    }

    @Test
    void findActiveByLongUrl_shouldReturnMappingWithNoExpiry() {
        UrlMapping mapping = new UrlMapping("noexp123", "https://noexpiry-example.com", null);
        entityManager.persistAndFlush(mapping);

        entityManager.clear();

        Optional<UrlMapping> found = repository.findActiveByLongUrl("https://noexpiry-example.com");

        assertTrue(found.isPresent(), "Should find mapping with no expiry");
        assertEquals("noexp123", found.get().getCode());
    }
}