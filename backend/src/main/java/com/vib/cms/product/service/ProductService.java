package com.vib.cms.product.service;

import com.vib.cms.product.api.PageResponse;
import com.vib.cms.product.api.ProductRequest;
import com.vib.cms.product.api.ProductResponse;
import com.vib.cms.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "ARCHIVED");

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public PageResponse<ProductResponse> list(int page, int size, String keyword, String status) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (status != null && !status.isBlank()) {
            status = normalizeStatus(status);
        } else {
            status = null;
        }
        try {
            Math.multiplyExact((long) page, size);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("page and size are too large");
        }
        return repository.findPage(page, size, keyword, status);
    }

    public ProductResponse get(UUID id) {
        return repository.find(id);
    }

    @Transactional
    public ProductResponse create(ProductRequest request, String actor) {
        return repository.insert(request, actor);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request, String actor) {
        ProductResponse current = repository.find(id);
        ensureNotArchived(current);
        return repository.update(id, request, actor);
    }

    @Transactional
    public ProductResponse changeStatus(UUID id, String requestedStatus, String actor) {
        ProductResponse current = repository.find(id);
        ensureNotArchived(current);

        String target = normalizeStatus(requestedStatus);
        if ("ARCHIVED".equals(target)) {
            throw new IllegalArgumentException("Use DELETE to archive a product");
        }
        if (current.status().equals(target)) {
            return current;
        }
        return repository.updateStatus(id, target, actor);
    }

    @Transactional
    public void archive(UUID id, String actor) {
        repository.archive(id, actor);
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("status must be ACTIVE, INACTIVE or ARCHIVED");
        }
        return normalized;
    }

    private void ensureNotArchived(ProductResponse product) {
        if ("ARCHIVED".equals(product.status())) {
            throw new IllegalArgumentException("Archived products cannot be modified");
        }
    }
}
