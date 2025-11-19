package com.oluyinkabright.tiny_url.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter redirectCounter;

    public MetricsService(MeterRegistry meterRegistry) {
        this.redirectCounter = Counter.builder("shortener.redirect.total")
                .description("Total number of URL redirects")
                .tag("type", "total")
                .register(meterRegistry);
    }

    public void incrementRedirectCounter(String status) {
        redirectCounter.increment();
    }
}