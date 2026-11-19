package com.vib.cms.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "code may contain only letters, numbers, dot, underscore and hyphen")
        String code,

        @NotBlank @Size(max = 255)
        String name,

        String description
) {
}
