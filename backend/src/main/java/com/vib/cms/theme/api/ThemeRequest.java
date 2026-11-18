package com.vib.cms.theme.api;
import jakarta.validation.constraints.*;import java.util.UUID;
public record ThemeRequest(@NotNull UUID productId,@NotBlank @Size(max=100) String code,@NotBlank @Size(max=255) String name,@Size(max=500) String rootBase,String domainsJson,String headConfigJson,String bodyConfigJson,@Pattern(regexp="ACTIVE|INACTIVE|ARCHIVED") String status){}
