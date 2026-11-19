package com.vib.cms.workflow.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class WorkflowConfigurationService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public WorkflowConfigurationService(JdbcTemplate jdbc,ObjectMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}

    public List<Map<String,Object>> list(UUID productId) {
        if(productId==null) return jdbc.queryForList("""
            SELECT id,product_id AS "productId",code,name,description,status
            FROM cms_workflow_definitions ORDER BY name
            """);
        return jdbc.queryForList("""
            SELECT id,product_id AS "productId",code,name,description,status
            FROM cms_workflow_definitions WHERE product_id=? OR product_id IS NULL ORDER BY product_id NULLS FIRST,name
            """,productId);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String,Object> create(Map<String,Object> d,String actor) {
        UUID workflowId=UUID.randomUUID(), versionId=UUID.randomUUID();
        Object product=d.get("productId");
        UUID productId=product==null||String.valueOf(product).isBlank()?null:UUID.fromString(String.valueOf(product));
        jdbc.update("""
            INSERT INTO cms_workflow_definitions(id,product_id,code,name,description,created_by,updated_by)
            VALUES (?,?,?,?,?,?,?)
            """,workflowId,productId,required(d,"code"),required(d,"name"),d.get("description"),actor,actor);
        jdbc.update("""
            INSERT INTO cms_workflow_versions(id,workflow_id,version_no,created_by)
            VALUES (?,?,1,?)
            """,versionId,workflowId,actor);

        Map<String,UUID> states=new HashMap<>(), actions=new HashMap<>(), rules=new HashMap<>();
        for(Map<String,Object> s:maps(d.get("states"))){
            UUID id=UUID.randomUUID(); String code=required(s,"code");
            jdbc.update("""
                INSERT INTO cms_workflow_states(id,workflow_version_id,state_code,state_name,is_initial,is_terminal)
                VALUES (?,?,?,?,?,?)
                """,id,versionId,code,required(s,"name"),Boolean.TRUE.equals(s.get("initial")),Boolean.TRUE.equals(s.get("terminal")));
            states.put(code,id);
        }
        for(Map<String,Object> a:maps(d.get("actions"))){
            UUID id=UUID.randomUUID();String code=required(a,"code");
            jdbc.update("""
                INSERT INTO cms_workflow_actions(id,workflow_version_id,action_code,action_name,changes_state,config)
                VALUES (?,?,?,?,?,?::jsonb)
                """,id,versionId,code,required(a,"name"),!Boolean.FALSE.equals(a.get("changesState")),json(a.getOrDefault("config",Map.of())));
            actions.put(code,id);
        }
        for(Map<String,Object> r:maps(d.get("userRules"))){
            UUID id=UUID.randomUUID();String code=required(r,"code");
            jdbc.update("""
                INSERT INTO cms_workflow_user_rules(id,workflow_version_id,rule_code,rule_name,rule_definition)
                VALUES (?,?,?,?,?::jsonb)
                """,id,versionId,code,required(r,"name"),json(r.getOrDefault("definition",Map.of())));
            rules.put(code,id);
        }
        for(Map<String,Object> t:maps(d.get("transitions"))){
            jdbc.update("""
                INSERT INTO cms_workflow_transitions(workflow_version_id,action_id,from_state_id,to_state_id,condition_config)
                VALUES (?,?,?,?,?::jsonb)
                """,versionId,requiredRef(actions,t,"action"),requiredRef(states,t,"fromState"),
                    optionalRef(states,t,"toState"),json(t.getOrDefault("condition",Map.of())));
        }
        for(Map<String,Object> p:maps(d.get("permissions"))){
            jdbc.update("""
                INSERT INTO cms_workflow_action_permissions(workflow_version_id,state_id,action_id,user_rule_id,effect,condition_config)
                VALUES (?,?,?,?,?,?::jsonb)
                """,versionId,requiredRef(states,p,"state"),requiredRef(actions,p,"action"),
                    requiredRef(rules,p,"userRule"),Objects.toString(p.get("effect"),"ALLOW"),
                    json(p.getOrDefault("condition",Map.of())));
        }
        for(Map<String,Object> v:maps(d.get("visibilityRules"))){
            jdbc.update("""
                INSERT INTO cms_workflow_visibility_rules(workflow_version_id,state_id,user_rule_id,visibility_scope,condition_config)
                VALUES (?,?,?,?,?::jsonb)
                """,versionId,requiredRef(states,v,"state"),requiredRef(rules,v,"userRule"),
                    Objects.toString(v.get("scope"),"NONE"),json(v.getOrDefault("condition",Map.of())));
        }
        return Map.of("id",workflowId,"versionId",versionId,"versionNo",1,"status","DRAFT",
                "states",states.keySet(),"actions",actions.keySet(),"userRules",rules.keySet());
    }

    private List<Map<String,Object>> maps(Object raw){
        if(!(raw instanceof List<?> list)) return List.of();
        List<Map<String,Object>> out=new ArrayList<>();
        for(Object x:list) if(x instanceof Map<?,?> m){Map<String,Object> n=new HashMap<>();m.forEach((k,v)->n.put(String.valueOf(k),v));out.add(n);}
        return out;
    }
    private UUID requiredRef(Map<String,UUID> refs,Map<String,Object> m,String key){
        UUID id=refs.get(required(m,key));if(id==null)throw new IllegalArgumentException("Unknown "+key+": "+m.get(key));return id;
    }
    private UUID optionalRef(Map<String,UUID> refs,Map<String,Object> m,String key){
        Object v=m.get(key);if(v==null||String.valueOf(v).isBlank())return null;
        UUID id=refs.get(String.valueOf(v));if(id==null)throw new IllegalArgumentException("Unknown "+key+": "+v);return id;
    }
    private String required(Map<String,Object> m,String k){
        Object v=m.get(k);if(v==null||String.valueOf(v).isBlank())throw new IllegalArgumentException(k+" is required");return String.valueOf(v).trim();
    }
    private String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("Invalid workflow JSON",e);}}
}
