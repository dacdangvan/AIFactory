package com.vib.cms.content.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class ContentConfigurationService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public ContentConfigurationService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc; this.mapper = mapper;
    }

    public List<Map<String,Object>> listContentTypes(UUID productId) {
        return jdbc.queryForList("""
            SELECT id, product_id AS "productId", parent_id AS "parentId", code, name,
                   description, sort_order AS "sortOrder", status
            FROM cms_content_types WHERE product_id=? AND status <> 'ARCHIVED'
            ORDER BY parent_id NULLS FIRST, sort_order, name
            """, productId);
    }

    @Transactional
    public Map<String,Object> createContentType(UUID productId, Map<String,Object> body, String actor) {
        String code = required(body, "code"), name = required(body, "name");
        UUID id = UUID.randomUUID();
        Object parent = body.get("parentId");
        jdbc.update("""
            INSERT INTO cms_content_types(id,product_id,parent_id,code,name,description,sort_order,created_by,updated_by)
            VALUES (?,?,?::uuid,?,?,?,?,?,?)
            """, id, productId, parent == null || String.valueOf(parent).isBlank() ? null : String.valueOf(parent),
                code, name, body.get("description"), number(body.get("sortOrder"),0), actor, actor);
        return jdbc.queryForMap("""
            SELECT id, product_id AS "productId", parent_id AS "parentId", code, name,
                   description, sort_order AS "sortOrder", status
            FROM cms_content_types WHERE id=?
            """, id);
    }

    public List<Map<String,Object>> listTemplates(UUID productId) {
        return jdbc.queryForList("""
            SELECT t.id, t.product_id AS "productId", t.code, t.name, t.description, t.status,
                   COALESCE((SELECT max(v.version_no) FROM cms_authoring_template_versions v WHERE v.template_id=t.id),0) AS "latestVersion"
            FROM cms_authoring_templates t WHERE t.product_id=? AND t.status <> 'ARCHIVED'
            ORDER BY t.name
            """, productId);
    }

    @Transactional
    public Map<String,Object> createTemplate(UUID productId, Map<String,Object> body, String actor) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO cms_authoring_templates(id,product_id,code,name,description,created_by,updated_by)
            VALUES (?,?,?,?,?,?,?)
            """, id, productId, required(body,"code"), required(body,"name"), body.get("description"), actor, actor);
        return jdbc.queryForMap("""
            SELECT id, product_id AS "productId", code, name, description, status
            FROM cms_authoring_templates WHERE id=?
            """, id);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String,Object> createTemplateVersion(UUID templateId, Map<String,Object> body, String actor) {
        Integer next = jdbc.queryForObject(
            "SELECT COALESCE(max(version_no),0)+1 FROM cms_authoring_template_versions WHERE template_id=?",
            Integer.class, templateId);
        Map<String,Object> schema = body.get("fieldSchema") instanceof Map<?,?> m
                ? (Map<String,Object>) m : Map.of("groups", List.of(), "fields", List.of());
        UUID versionId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO cms_authoring_template_versions(id,template_id,version_no,field_schema,created_by)
            VALUES (?,?,?,?::jsonb,?)
            """, versionId, templateId, next, json(schema), actor);

        Object rawSegments = body.get("urlSegments");
        if (rawSegments instanceof List<?> segments) {
            for (Object raw : segments) {
                if (!(raw instanceof Map<?,?> segment)) continue;
                UUID segmentId = UUID.randomUUID();
                jdbc.update("""
                    INSERT INTO cms_template_url_segments(id,template_version_id,segment_order,segment_code,separator,is_required)
                    VALUES (?,?,?,?,?,?)
                    """, segmentId, versionId, number(segment.get("order"),1),
                        required(segment,"code"), Objects.toString(segment.get("separator"),"-"),
                        !Boolean.FALSE.equals(segment.get("required")));
                Object rawFields = segment.get("fields");
                if (rawFields instanceof List<?> fields) {
                    int order=0;
                    for (Object field : fields) {
                        order++;
                        String fieldCode = field instanceof Map<?,?> fm
                                ? required(fm,"fieldCode") : String.valueOf(field);
                        jdbc.update("""
                            INSERT INTO cms_template_url_segment_fields(segment_id,field_code,field_order,locale_aware)
                            VALUES (?,?,?,?)
                            """, segmentId, fieldCode, order,
                                !(field instanceof Map<?,?> fm) || !Boolean.FALSE.equals(fm.get("localeAware")));
                    }
                }
            }
        }
        return Map.of("id",versionId,"templateId",templateId,"versionNo",next,"status","DRAFT",
                "fieldSchema",schema,"urlSegments",rawSegments == null ? List.of() : rawSegments);
    }

    @Transactional
    public Map<String,Object> bindTemplate(UUID contentTypeId, UUID templateVersionId, String actor) {
        jdbc.queryForObject("SELECT id FROM cms_content_types WHERE id=?", UUID.class, contentTypeId);
        jdbc.queryForObject("SELECT id FROM cms_authoring_template_versions WHERE id=?", UUID.class, templateVersionId);
        jdbc.update("UPDATE cms_content_type_template_bindings SET is_default=false WHERE content_type_id=?", contentTypeId);
        UUID id=UUID.randomUUID();
        jdbc.update("""
            INSERT INTO cms_content_type_template_bindings(id,content_type_id,template_version_id,is_default,created_by)
            VALUES (?,?,?,true,?)
            ON CONFLICT (content_type_id,template_version_id)
            DO UPDATE SET is_default=true
            """, id, contentTypeId, templateVersionId, actor);
        return Map.of("contentTypeId",contentTypeId,"templateVersionId",templateVersionId,"isDefault",true);
    }

    private String required(Map<?,?> body,String key) {
        Object value=body.get(key);
        if(value==null || String.valueOf(value).isBlank()) throw new IllegalArgumentException(key+" is required");
        return String.valueOf(value).trim();
    }
    private int number(Object value,int fallback) {
        if(value instanceof Number n) return n.intValue();
        if(value==null) return fallback;
        return Integer.parseInt(String.valueOf(value));
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch(Exception e) { throw new IllegalArgumentException("Invalid JSON configuration",e); }
    }
}
