package com.vib.cms.product.api;
import java.time.OffsetDateTime;import java.util.UUID;
public record ProductResponse(UUID id,String code,String name,String description,String status,OffsetDateTime createdAt,OffsetDateTime updatedAt){}
