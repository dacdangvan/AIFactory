package com.vib.cms.rbac.api;
import jakarta.validation.constraints.*;import java.time.Instant;import java.util.UUID;
public record AssignmentRequest(@NotNull UUID userId,@NotNull UUID productId,@NotNull UUID roleId,Instant expiresAt) {}