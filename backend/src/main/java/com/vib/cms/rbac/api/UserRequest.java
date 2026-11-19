package com.vib.cms.rbac.api;
import jakarta.validation.constraints.*;
public record UserRequest(@NotBlank @Size(max=150) String username,@Email @Size(max=320) String email,@Size(max=255) String displayName,@NotBlank @Pattern(regexp="ACTIVE|INACTIVE|LOCKED") String status) {}