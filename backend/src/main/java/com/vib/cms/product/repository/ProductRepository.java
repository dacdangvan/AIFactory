package com.vib.cms.product.repository;

import com.vib.cms.common.exception.ResourceNotFoundException;
import com.vib.cms.product.api.PageResponse;
import com.vib.cms.product.api.ProductRequest;
import com.vib.cms.product.api.ProductResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class ProductRepository {

    private static final String SELECT_COLUMNS = """
            id, code, name, description, status,
            created_at, created_by, updated_at, updated_by
            """;

    private final JdbcTemplate jdbc;
    private final RowMapper<ProductResponse> mapper = (rs, rowNum) -> new ProductResponse(
            rs.getObject("id", UUID.class),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("status"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getString("created_by"),
            rs.getObject("updated_at", java.time.OffsetDateTime.class),
            rs.getString("updated_by")
    );

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResponse<ProductResponse> findPage(int page, int size, String keyword, String status) {
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (code ilike ? or name ilike ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null) {
            where.append(" and status = ?");
            params.add(status);
        }

        Long total = jdbc.queryForObject(
                "select count(*) from products" + where,
                Long.class,
                params.toArray()
        );

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add(Math.multiplyExact((long) page, size));

        List<ProductResponse> items = jdbc.query(
                "select " + SELECT_COLUMNS + " from products" + where
                        + " order by name asc, id asc limit ? offset ?",
                mapper,
                pageParams.toArray()
        );

        long totalElements = total == null ? 0 : total;
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(items, page, size, totalElements, totalPages);
    }

    public ProductResponse find(UUID id) {
        return jdbc.query(
                "select " + SELECT_COLUMNS + " from products where id = ?",
                mapper,
                id
        ).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public ProductResponse insert(ProductRequest request, String actor) {
        return jdbc.queryForObject("""
                insert into products(code, name, description, status, created_by, updated_by)
                values (?, ?, ?, 'ACTIVE', ?, ?)
                returning id, code, name, description, status,
                          created_at, created_by, updated_at, updated_by
                """, mapper, request.code(), request.name(), request.description(), actor, actor);
    }

    public ProductResponse update(UUID id, ProductRequest request, String actor) {
        return jdbc.query("""
                update products
                   set code = ?, name = ?, description = ?, updated_at = now(), updated_by = ?
                 where id = ? and status <> 'ARCHIVED'
                returning id, code, name, description, status,
                          created_at, created_by, updated_at, updated_by
                """, mapper, request.code(), request.name(), request.description(), actor, id)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found or archived: " + id));
    }

    public ProductResponse updateStatus(UUID id, String status, String actor) {
        return jdbc.query("""
                update products
                   set status = ?, updated_at = now(), updated_by = ?
                 where id = ? and status <> 'ARCHIVED'
                returning id, code, name, description, status,
                          created_at, created_by, updated_at, updated_by
                """, mapper, status, actor, id)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found or archived: " + id));
    }

    public void archive(UUID id, String actor) {
        int changed = jdbc.update("""
                update products
                   set status = 'ARCHIVED', updated_at = now(), updated_by = ?
                 where id = ? and status <> 'ARCHIVED'
                """, actor, id);

        if (changed == 0) {
            boolean exists = Boolean.TRUE.equals(jdbc.queryForObject(
                    "select exists(select 1 from products where id = ?)",
                    Boolean.class,
                    id
            ));
            if (!exists) {
                throw new ResourceNotFoundException("Product not found: " + id);
            }
            // Archiving an already archived product is intentionally idempotent.
        }
    }
}
