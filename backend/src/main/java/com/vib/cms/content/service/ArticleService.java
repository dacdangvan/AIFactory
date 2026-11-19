package com.vib.cms.content.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.vib.cms.common.exception.ResourceNotFoundException;
import com.vib.cms.content.api.ArticleRequest;
import com.vib.cms.content.api.ArticleResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ArticleService {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ArticleService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public List<ArticleResponse> list(UUID productId, String type, String status, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }

        StringBuilder sql = new StringBuilder("""
                SELECT a.*, v.locale_data::text AS locale_data, v.seo_data::text AS seo_data
                FROM cms_articles a
                JOIN cms_article_versions v
                  ON v.article_id = a.id AND v.version_no = a.current_version
                WHERE a.product_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(productId);

        // Add optional filters only when supplied. Avoid expressions such as
        // (? IS NULL OR column = ?) because PostgreSQL cannot infer the type
        // of a NULL bind parameter in the IS NULL branch.
        if (type != null && !type.isBlank()) {
            sql.append(" AND a.content_type = ?");
            params.add(type.trim());
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = ?");
            params.add(status.trim());
        }

        sql.append(" ORDER BY a.updated_at DESC LIMIT ?");
        params.add(limit);

        return jdbc.query(sql.toString(), this::map, params.toArray());
    }

    public ArticleResponse get(UUID id) {
        return jdbc.query("""
                SELECT a.*, v.locale_data::text AS locale_data, v.seo_data::text AS seo_data
                FROM cms_articles a JOIN cms_article_versions v
                  ON v.article_id=a.id AND v.version_no=a.current_version
                WHERE a.id=?
                """, this::map, id).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + id));
    }

    @Transactional
    public ArticleResponse create(UUID productId, ArticleRequest request, String actor) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO cms_articles(id,product_id,content_type,code,slug,created_by,updated_by)
                VALUES (?,?,?,?,?,?,?)
                """, id, productId, request.contentType(), request.code(), request.slug(), actor, actor);
        insertVersion(id, 1, request, actor);
        return get(id);
    }

    @Transactional
    public ArticleResponse update(UUID id, ArticleRequest request, String actor) {
        ArticleResponse current = get(id);
        if ("PUBLISHED".equals(current.status()) || "ARCHIVED".equals(current.status()))
            throw new IllegalArgumentException("Published or archived article cannot be edited; create a new draft/version");
        int next = current.version() + 1;
        jdbc.update("""
                UPDATE cms_articles SET content_type=?,code=?,slug=?,current_version=?,updated_at=now(),updated_by=?
                WHERE id=?
                """, request.contentType(), request.code(), request.slug(), next, actor, id);
        insertVersion(id, next, request, actor);
        return get(id);
    }

    @Transactional
    public ArticleResponse transition(UUID id, String target, String actor) {
        ArticleResponse current = get(id);
        boolean valid = switch (target) {
            case "IN_REVIEW" -> "DRAFT".equals(current.status());
            case "APPROVED" -> "IN_REVIEW".equals(current.status());
            case "PUBLISHED" -> "APPROVED".equals(current.status());
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("Invalid article transition: " + current.status() + " -> " + target);
        jdbc.update("UPDATE cms_articles SET status=?,updated_at=now(),updated_by=? WHERE id=?", target, actor, id);
        return get(id);
    }

    private void insertVersion(UUID id, int version, ArticleRequest request, String actor) {
        jdbc.update("""
                INSERT INTO cms_article_versions(article_id,version_no,locale_data,seo_data,created_by)
                VALUES (?,?,?::jsonb,?::jsonb,?)
                """, id, version, json(request.localeData()), json(request.seoData()), actor);
    }

    private String json(Map<String, Object> value) {
        try { return mapper.writeValueAsString(value == null ? Map.of() : value); }
        catch (Exception ex) { throw new IllegalArgumentException("Invalid JSON data", ex); }
    }

    private ArticleResponse map(ResultSet rs, int row) throws SQLException {
        try {
            return new ArticleResponse(
                    rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class),
                    rs.getString("content_type"), rs.getString("code"), rs.getString("slug"),
                    rs.getString("status"), rs.getInt("current_version"),
                    mapper.readValue(rs.getString("locale_data"), MAP),
                    mapper.readValue(rs.getString("seo_data"), MAP),
                    rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
        } catch (Exception ex) { throw new SQLException("Cannot map article row", ex); }
    }
}
