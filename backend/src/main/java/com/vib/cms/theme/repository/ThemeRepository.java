package com.vib.cms.theme.repository;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import com.vib.cms.common.exception.ResourceNotFoundException;
import com.vib.cms.theme.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class ThemeRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final RowMapper<ThemeResponse> mapper = (r, n) -> new ThemeResponse(r.getObject("id", UUID.class),
            r.getObject("product_id", UUID.class), r.getString("code"), r.getString("name"), r.getString("root_base"),
            r.getString("domains"), r.getString("head_config"), r.getString("body_config"), r.getString("status"),
            r.getObject("created_at", java.time.OffsetDateTime.class),
            r.getObject("updated_at", java.time.OffsetDateTime.class));

    public ThemeRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    private String validJson(String raw, String fallback, boolean array) {
        try {
            JsonNode n = json.readTree(raw == null || raw.isBlank() ? fallback : raw);
            if (array && !n.isArray())
                throw new IllegalArgumentException("domainsJson must be an array");
            if (!array && !n.isObject())
                throw new IllegalArgumentException("head/body config must be an object");
            return n.toString();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON input");
        }
    }

    public List<ThemeResponse> list(UUID product) {
        return product == null ? jdbc.query("select * from themes order by name", mapper)
                : jdbc.query("select * from themes where product_id=? order by name", mapper, product);
    }

    public ThemeResponse get(UUID id) {
        return jdbc.query("select * from themes where id=?", mapper, id).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found: " + id));
    }

    public ThemeResponse create(ThemeRequest r, String actor) {
        return jdbc.queryForObject(
                "insert into themes(product_id,code,name,root_base,domains,head_config,body_config,status,created_by,updated_by) values(?,?,?,?,?::jsonb,?::jsonb,?::jsonb,coalesce(?,'ACTIVE'),?,?) returning *",
                mapper, r.productId(), r.code(), r.name(), r.rootBase() == null ? "/" : r.rootBase(),
                validJson(r.domainsJson(), "[]", true), validJson(r.headConfigJson(), "{}", false),
                validJson(r.bodyConfigJson(), "{}", false), r.status(), actor, actor);
    }

    public ThemeResponse update(UUID id, ThemeRequest r, String actor) {
        return jdbc.query(
                "update themes set code=?,name=?,root_base=?,domains=?::jsonb,head_config=?::jsonb,body_config=?::jsonb,status=coalesce(?,status),updated_at=now(),updated_by=? where id=? returning *",
                mapper, r.code(), r.name(), r.rootBase() == null ? "/" : r.rootBase(),
                validJson(r.domainsJson(), "[]", true), validJson(r.headConfigJson(), "{}", false),
                validJson(r.bodyConfigJson(), "{}", false), r.status(), actor, id).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found: " + id));
    }

    public void archive(UUID id) {
        if (jdbc.update("update themes set status='ARCHIVED',updated_at=now() where id=?", id) == 0)
            throw new ResourceNotFoundException("Theme not found: " + id);
    }
}
