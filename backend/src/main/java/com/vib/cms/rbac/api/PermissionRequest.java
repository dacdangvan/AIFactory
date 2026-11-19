package com.vib.cms.rbac.api;
import jakarta.validation.constraints.*;
public record PermissionRequest(@NotBlank @Size(max=80) String resourceType,@NotBlank @Size(max=80) String actionCode,@Size(max=80) String stateCode,@NotBlank @Pattern(regexp="GLOBAL|PRODUCT|RESOURCE") String scopeType,@NotBlank @Pattern(regexp="ALLOW|DENY") String effect,String description) {}