package com.vib.cms.rbac.api;
import jakarta.validation.constraints.NotNull;import java.util.UUID;
public record RolePermissionRequest(@NotNull UUID permissionId) {}