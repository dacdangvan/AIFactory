package com.vib.cms.content.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ArticleRequest(
        @NotBlank @Pattern(regexp = "NEWS|INVESTOR_RELATIONS|PROMOTION|GUIDE") String contentType,
        @NotBlank @Size(max = 150) String code,
        @NotBlank @Size(max = 300) String slug,
        Map<String, Object> localeData,
        Map<String, Object> seoData
) {}