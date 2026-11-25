package com.vib.cms.content.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class PageManagementService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public PageManagementService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public List<Map<String,Object>> tree(UUID productId) {
        return jdbc.queryForList("""
            SELECT p.id,p.product_id AS "productId",p.parent_id AS "parentId",p.theme_id AS "themeId",
                   p.page_type AS "pageType",p.code,p.title,p.locale,p.slug,p.status,p.sort_order AS "sortOrder",
                   p.seo::text AS seo,p.settings::text AS settings,p.created_at AS "createdAt",p.updated_at AS "updatedAt"
            FROM cms_pages p WHERE p.product_id=? AND p.status<>'ARCHIVED'
            ORDER BY COALESCE(p.parent_id,p.id),p.sort_order,p.title
            """,productId).stream().map(this::decode).toList();
    }

    public Map<String,Object> get(UUID id) {
        return decode(jdbc.queryForMap("""
            SELECT p.id,p.product_id AS "productId",p.parent_id AS "parentId",p.theme_id AS "themeId",
                   p.page_type AS "pageType",p.code,p.title,p.locale,p.slug,p.status,p.sort_order AS "sortOrder",
                   p.seo::text AS seo,p.settings::text AS settings,p.created_at AS "createdAt",p.updated_at AS "updatedAt"
            FROM cms_pages p WHERE p.id=?
            """,id));
    }

    @Transactional
    public Map<String,Object> create(UUID productId,Map<String,Object> body,String actor) {
        String code=req(body,"code"),title=req(body,"title"),slug=req(body,"slug");
        UUID id=UUID.randomUUID();
        String parent=nullable(body.get("parentId")),theme=nullable(body.get("themeId"));
        jdbc.update("""
            INSERT INTO cms_pages(id,product_id,parent_id,theme_id,page_type,code,title,locale,slug,status,sort_order,seo,settings,created_by,updated_by)
            VALUES (?,?,?::uuid,?::uuid,?,?,?,?,?,?,?::jsonb,?::jsonb,?,?)
            """,id,productId,parent,theme,Objects.toString(body.get("pageType"),"NORMAL"),code,title,
                Objects.toString(body.get("locale"),"vi-VN"),slug,Objects.toString(body.get("status"),"DRAFT"),
                num(body.get("sortOrder"),0),json(body.get("seo")),json(body.get("settings")),actor,actor);
        return get(id);
    }

    @Transactional
    public Map<String,Object> update(UUID id,Map<String,Object> body,String actor) {
        Map<String,Object> before=get(id);
        String oldPath=routePath(before);
        String slug=Objects.toString(body.getOrDefault("slug",before.get("slug")));
        String locale=Objects.toString(body.getOrDefault("locale",before.get("locale")));
        String title=Objects.toString(body.getOrDefault("title",before.get("title")));
        String parent=body.containsKey("parentId")?nullable(body.get("parentId")):nullable(before.get("parentId"));
        if(parent!=null && UUID.fromString(parent).equals(id)) throw new IllegalArgumentException("A page cannot be its own parent");
        jdbc.update("""
            UPDATE cms_pages SET parent_id=?::uuid,theme_id=?::uuid,page_type=?,title=?,locale=?,slug=?,status=?,sort_order=?,
              seo=?::jsonb,settings=?::jsonb,updated_by=?,updated_at=now() WHERE id=?
            """,parent,body.containsKey("themeId")?nullable(body.get("themeId")):nullable(before.get("themeId")),
                Objects.toString(body.getOrDefault("pageType",before.get("pageType")),"NORMAL"),title,locale,slug,
                Objects.toString(body.getOrDefault("status",before.get("status")),"DRAFT"),
                num(body.get("sortOrder"),num(before.get("sortOrder"),0)),
                json(body.getOrDefault("seo",before.get("seo"))),json(body.getOrDefault("settings",before.get("settings"))),actor,id);
        Map<String,Object> after=get(id);
        String newPath=routePath(after);
        if(!oldPath.equals(newPath)) {
            jdbc.update("""
                INSERT INTO cms_page_route_history(page_id,old_route_path,new_route_path,redirect_status,created_by)
                VALUES (?,?,?,301,?) ON CONFLICT(old_route_path) DO NOTHING
                """,id,oldPath,newPath,actor);
        }
        snapshot(id,actor);
        return after;
    }

    @Transactional
    public void archive(UUID id) {
        if(jdbc.queryForObject("SELECT count(*) FROM cms_pages WHERE parent_id=? AND status<>'ARCHIVED'",Integer.class,id)>0)
            throw new IllegalArgumentException("Move or archive child pages before archiving this page");
        jdbc.update("UPDATE cms_pages SET status='ARCHIVED',updated_at=now() WHERE id=?",id);
    }

    public List<Map<String,Object>> sections(UUID pageId) {
        return jdbc.queryForList("""
            SELECT id,page_id AS "pageId",section_key AS "sectionKey",section_name AS "sectionName",
                   component_key AS "componentKey",content_item_id AS "contentItemId",sort_order AS "sortOrder",config::text AS config
            FROM cms_page_sections WHERE page_id=? ORDER BY sort_order,id
            """,pageId).stream().map(this::decode).toList();
    }

    @Transactional
    public List<Map<String,Object>> saveSections(UUID pageId,List<Map<String,Object>> sections,String actor) {
        jdbc.queryForObject("SELECT id FROM cms_pages WHERE id=?",UUID.class,pageId);
        jdbc.update("DELETE FROM cms_page_sections WHERE page_id=?",pageId);
        int order=0;
        for(Map<String,Object> s:sections) {
            order++;
            jdbc.update("""
                INSERT INTO cms_page_sections(id,page_id,section_key,section_name,component_key,content_item_id,sort_order,config)
                VALUES (?,?,?,?,?,?::uuid,?,?::jsonb)
                """,UUID.randomUUID(),pageId,req(s,"sectionKey"),req(s,"sectionName"),req(s,"componentKey"),
                    nullable(s.get("contentItemId")),num(s.get("sortOrder"),order),json(s.get("config")));
        }
        snapshot(pageId,actor);
        return sections(pageId);
    }

    public List<Map<String,Object>> themes(UUID productId) {
        return jdbc.queryForList("""
            SELECT id,product_id AS "productId",code,name,description,status,head_html AS "headHtml",
                   body_start_html AS "bodyStartHtml",body_end_html AS "bodyEndHtml",custom_css AS "customCss",custom_js AS "customJs"
            FROM cms_themes WHERE product_id=? AND status<>'ARCHIVED' ORDER BY name
            """,productId);
    }

    @Transactional
    public Map<String,Object> saveTheme(UUID productId,Map<String,Object> body,String actor) {
        UUID id=body.get("id")==null?UUID.randomUUID():UUID.fromString(String.valueOf(body.get("id")));
        jdbc.update("""
            INSERT INTO cms_themes(id,product_id,code,name,description,head_html,body_start_html,body_end_html,custom_css,custom_js,status,created_by,updated_by)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,head_html=EXCLUDED.head_html,
             body_start_html=EXCLUDED.body_start_html,body_end_html=EXCLUDED.body_end_html,custom_css=EXCLUDED.custom_css,
             custom_js=EXCLUDED.custom_js,status=EXCLUDED.status,updated_by=EXCLUDED.updated_by,updated_at=now()
            """,id,productId,req(body,"code"),req(body,"name"),body.get("description"),body.get("headHtml"),
                body.get("bodyStartHtml"),body.get("bodyEndHtml"),body.get("customCss"),body.get("customJs"),
                Objects.toString(body.get("status"),"DRAFT"),actor,actor);
        return jdbc.queryForMap("SELECT id,product_id AS \"productId\",code,name,description,status,head_html AS \"headHtml\",body_start_html AS \"bodyStartHtml\",body_end_html AS \"bodyEndHtml\",custom_css AS \"customCss\",custom_js AS \"customJs\" FROM cms_themes WHERE id=?",id);
    }

    private void snapshot(UUID pageId,String actor) {
        Map<String,Object> page=get(pageId);
        page.put("sections",sections(pageId));
        Integer version=jdbc.queryForObject("SELECT COALESCE(max(version_no),0)+1 FROM cms_page_versions WHERE page_id=?",Integer.class,pageId);
        jdbc.update("INSERT INTO cms_page_versions(page_id,version_no,snapshot,created_by) VALUES (?,?,?::jsonb,?)",pageId,version,json(page),actor);
    }
    private String routePath(Map<String,Object> page) {
        List<String> slugs=new ArrayList<>();
        Map<String,Object> current=page;
        Set<UUID> seen=new HashSet<>();
        while(current!=null) {
            UUID id=UUID.fromString(String.valueOf(current.get("id")));
            if(!seen.add(id)) throw new IllegalArgumentException("Page tree contains a parent cycle");
            String slug=Objects.toString(current.get("slug"),"").trim();
            if(!slug.isBlank()) slugs.add(slug);
            Object parent=current.get("parentId");
            current=parent==null?null:get(UUID.fromString(String.valueOf(parent)));
        }
        Collections.reverse(slugs);
        String locale=Objects.toString(page.get("locale"),"vi-VN");
        List<String> parts=new ArrayList<>();
        parts.add(locale);
        parts.addAll(slugs);
        return "/"+String.join("/",parts);
    }
    private Map<String,Object> decode(Map<String,Object> row) {
        for(String key:List.of("seo","settings","config")) if(row.containsKey(key)&&row.get(key) instanceof String s) {
            try { row.put(key,mapper.readValue(s,Object.class)); } catch(Exception ignored) { row.put(key,Map.of()); }
        }
        return row;
    }
    private String req(Map<?,?> m,String k) { Object v=m.get(k); if(v==null||String.valueOf(v).isBlank()) throw new IllegalArgumentException(k+" is required"); return String.valueOf(v).trim(); }
    private String nullable(Object v) { return v==null||String.valueOf(v).isBlank()?null:String.valueOf(v); }
    private int num(Object v,int d) { return v instanceof Number n?n.intValue():v==null?d:Integer.parseInt(String.valueOf(v)); }
    private String json(Object v) { try { return mapper.writeValueAsString(v==null?Map.of():v); } catch(Exception e) { throw new IllegalArgumentException("Invalid JSON",e); } }
}
