package com.vib.cms.theme.api;
import java.time.OffsetDateTime;import java.util.UUID;
public record ThemeResponse(UUID id,UUID productId,String code,String name,String rootBase,String domainsJson,String headConfigJson,String bodyConfigJson,String status,OffsetDateTime createdAt,OffsetDateTime updatedAt){}
