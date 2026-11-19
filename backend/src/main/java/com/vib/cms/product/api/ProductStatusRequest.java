package com.vib.cms.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProductStatusRequest(
        @NotBlank
        @Pattern(regexp = "ACTIVE|INACTIVE", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "status must be ACTIVE or INACTIVE")
        String status
) {
}
