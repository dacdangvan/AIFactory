package com.vib.cms.content.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        UUID productId,
        String contentType,
        String code,
        String slug,
        String status,
        int version,
        Map<String, Object> localeData,
        Map<String, Object> seoData,
        Instant createdAt,
        Instant updatedAt
) {}