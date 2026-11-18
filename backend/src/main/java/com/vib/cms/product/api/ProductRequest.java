package com.vib.cms.product.api;
import jakarta.validation.constraints.*;
public record ProductRequest(@NotBlank @Size(max=100) @Pattern(regexp="[A-Za-z0-9._-]+") String code,@NotBlank @Size(max=255) String name,String description,@Pattern(regexp="ACTIVE|INACTIVE|ARCHIVED") String status){}
