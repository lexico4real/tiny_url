package com.oluyinkabright.tiny_url.repositories;

import com.oluyinkabright.tiny_url.entities.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByCode(String code);

    Optional<UrlMapping> findByLongUrl(String longUrl);

    boolean existsByCode(String code);

    @Query("SELECT u FROM UrlMapping u WHERE u.longUrl = :longUrl AND (u.expiresAt IS NULL OR u.expiresAt > FUNCTION('NOW'))")
    Optional<UrlMapping> findActiveByLongUrl(@Param("longUrl") String longUrl);
}