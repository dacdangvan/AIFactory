package com.vib.cms.content.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;\nimport org.springframework.security.access.AccessDeniedException;

@Service
public class ContentItemService {
    private static final TypeReference<Map<String,Object>> MAP_TYPE = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ContentItemService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public List<Map<String,Object>> list(UUID productId, UUID contentTypeId, String keyword, int limit) {
        if (limit < 1 || limit > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
        StringBuilder sql = new StringBuilder("""
            SELECT i.id, i.product_id AS "productId", i.content_type_id AS "contentTypeId",
                   ct.code AS "contentTypeCode", ct.name AS "contentTypeName",
                   i.code, i.owner_username AS "ownerUsername", i.current_version AS "currentVersion",
                   v.data_snapshot::text AS "dataSnapshot", wi.current_state_id AS "currentStateId",
                   ws.state_code AS "workflowState", ws.state_name AS "workflowStateName",
                   i.created_at AS "createdAt", i.updated_at AS "updatedAt"
            FROM cms_content_items i
            JOIN cms_content_types ct ON ct.id=i.content_type_id
            JOIN cms_content_versions v ON v.content_item_id=i.id AND v.version_no=i.current_version
            LEFT JOIN cms_workflow_instances wi ON wi.content_item_id=i.id
            LEFT JOIN cms_workflow_states ws ON ws.id=wi.current_state_id
            WHERE i.product_id=? AND i.deleted_at IS NULL
            """);
        List<Object> args = new ArrayList<>();
        args.add(productId);
        if (contentTypeId != null) { sql.append(" AND i.content_type_id=?"); args.add(contentTypeId); }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (lower(i.code) LIKE ? OR lower(ct.name) LIKE ?)");
            args.add("%"+keyword.toLowerCase(Locale.ROOT)+"%");
            args.add("%"+keyword.toLowerCase(Locale.ROOT)+"%");
        }
        sql.append(" ORDER BY i.updated_at DESC LIMIT ?");
        args.add(limit);
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public Map<String,Object> get(UUID id) {
        Map<String,Object> item = jdbc.queryForMap("""
            SELECT i.id, i.product_id AS "productId", i.content_type_id AS "contentTypeId",
                   ct.code AS "contentTypeCode", ct.name AS "contentTypeName",
                   i.template_version_id AS "templateVersionId", i.workflow_version_id AS "workflowVersionId",
                   i.code, i.owner_username AS "ownerUsername", i.current_version AS "currentVersion",
                   v.data_snapshot::text AS "dataSnapshot", v.locale_metadata::text AS "localeMetadata",
                   ws.state_code AS "workflowState", ws.state_name AS "workflowStateName",
                   i.created_at AS "createdAt", i.updated_at AS "updatedAt"
            FROM cms_content_items i
            JOIN cms_content_types ct ON ct.id=i.content_type_id
            JOIN cms_content_versions v ON v.content_item_id=i.id AND v.version_no=i.current_version
            LEFT JOIN cms_workflow_instances wi ON wi.content_item_id=i.id
            LEFT JOIN cms_workflow_states ws ON ws.id=wi.current_state_id
            WHERE i.id=? AND i.deleted_at IS NULL
            """, id);
        item.put("dataSnapshot", parseObject(Objects.toString(item.get("dataSnapshot"), "{}")));
        item.put("localeMetadata", parseObject(Objects.toString(item.get("localeMetadata"), "{}")));
        return item;
    }

    public Map<String,Object> getAuthoringDefinition(UUID contentTypeId) {
        Map<String,Object> type = jdbc.queryForMap("""
            SELECT ct.id, ct.product_id AS "productId", ct.code, ct.name,
                   b.template_version_id AS "templateVersionId",
                   tv.field_schema::text AS "fieldSchema",
                   t.name AS "templateName", tv.version_no AS "templateVersion"
            FROM cms_content_types ct
            JOIN cms_content_type_template_bindings b ON b.content_type_id=ct.id AND b.is_default=true
            JOIN cms_authoring_template_versions tv ON tv.id=b.template_version_id
            JOIN cms_authoring_templates t ON t.id=tv.template_id
            WHERE ct.id=? AND ct.status='ACTIVE' AND t.status='ACTIVE'
            """, contentTypeId);
        type.put("fieldSchema", parseObject(Objects.toString(type.get("fieldSchema"), "{}")));
        return type;
    }

    public Map<String,Object> getAuthoringDefinitionForItem(UUID itemId) {
        Map<String,Object> definition = jdbc.queryForMap("""
            SELECT ct.id, ct.product_id AS "productId", ct.code, ct.name,
                   i.template_version_id AS "templateVersionId",
                   tv.field_schema::text AS "fieldSchema",
                   t.name AS "templateName", tv.version_no AS "templateVersion"
            FROM cms_content_items i
            JOIN cms_content_types ct ON ct.id=i.content_type_id
            JOIN cms_authoring_template_versions tv ON tv.id=i.template_version_id
            JOIN cms_authoring_templates t ON t.id=tv.template_id
            WHERE i.id=? AND i.deleted_at IS NULL
            """, itemId);
        definition.put("fieldSchema", parseObject(Objects.toString(definition.get("fieldSchema"), "{}")));
        return definition;
    }

    @Transactional
    public Map<String,Object> create(UUID productId, UUID contentTypeId, Map<String,Object> body, String actor) {
        String code = required(body, "code");
        Map<String,Object> definition = getAuthoringDefinition(contentTypeId);
        if (!productId.equals(definition.get("productId"))) throw new IllegalArgumentException("Content Type does not belong to selected Product");
        UUID templateVersionId = (UUID) definition.get("templateVersionId");
        UUID workflowVersionId = resolveWorkflow(productId, contentTypeId);
        Map<String,Object> fields = objectMap(body.get("fields"));
        validateFields(objectMap(definition.get("fieldSchema")), fields);

        UUID initialStateId = jdbc.queryForObject(
            "SELECT id FROM cms_workflow_states WHERE workflow_version_id=? AND is_initial=true",
            UUID.class, workflowVersionId);
        String initialStateCode = jdbc.queryForObject(
            "SELECT state_code FROM cms_workflow_states WHERE id=?", String.class, initialStateId);

        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO cms_content_items(id,product_id,content_type_id,template_version_id,workflow_version_id,
                code,owner_username,created_by,updated_by,current_version)
            VALUES (?,?,?,?,?,?,?,?,?,1)
            """, id, productId, contentTypeId, templateVersionId, workflowVersionId, code, actor, actor, actor);
        Map<String,Object> snapshot = Map.of("fields", fields);
        jdbc.update("""
            INSERT INTO cms_content_versions(content_item_id,version_no,data_snapshot,locale_metadata,created_by)
            VALUES (?,1,?::jsonb,?::jsonb,?)
            """, id, json(snapshot), json(Map.of("locales", List.of("vi-VN","en-US"))), actor);
        UUID workflowInstanceId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO cms_workflow_instances(id,workflow_version_id,content_item_id,current_state_id)
            VALUES (?,?,?,?)
            """, workflowInstanceId, workflowVersionId, id, initialStateId);
        jdbc.update("""
            INSERT INTO cms_workflow_history(workflow_instance_id,action_code,from_state_code,to_state_code,actor_username,comment)
            VALUES (?,'CREATE',NULL,?,?,?)
            """, workflowInstanceId, initialStateCode, actor, "Created in initial workflow state");
        Map<String,Object> allowed=workflowActions(id,actor);
        List<?> available=(List<?>)allowed.get("availableActions");
        if(available==null||available.stream().noneMatch(a->a instanceof Map<?,?> m && Set.of("SAVE","SUBMIT").contains(Objects.toString(m.get("code"),""))))
            throw new AccessDeniedException("You do not have permission to create content in this workflow");
        return get(id);
    }

    @Transactional
    public Map<String,Object> update(UUID id, Map<String,Object> body, String actor) {
        Map<String,Object> current = jdbc.queryForMap("""
            SELECT i.product_id AS "productId", i.content_type_id AS "contentTypeId",
                   i.template_version_id AS "templateVersionId", i.current_version AS "currentVersion",
                   i.workflow_version_id AS "workflowVersionId",
                   i.owner_username AS "ownerUsername", wi.current_state_id AS "stateId", ws.state_code AS "stateCode", ws.is_initial AS "isInitial"
            FROM cms_content_items i
            JOIN cms_workflow_instances wi ON wi.content_item_id=i.id
            JOIN cms_workflow_states ws ON ws.id=wi.current_state_id
            WHERE i.id=? AND i.deleted_at IS NULL
            """, id);
        Map<String,Object> actions=workflowActions(id,actor);
        if(!Boolean.TRUE.equals(actions.get("canEdit")))throw new AccessDeniedException("You do not have permission to save this Content Item in state "+state);
        if (!Boolean.TRUE.equals(current.get("isInitial")) && !"REJECTED".equals(state)) {
            throw new IllegalStateException("Content can only be edited in its initial workflow state or REJECTED state");
        }
        // Keep using the exact immutable Template Version captured when this Content Item was created.
        // A later default-template change on the Content Type must not change an existing item's schema.
        String schemaJson = jdbc.queryForObject(
            "SELECT field_schema::text FROM cms_authoring_template_versions WHERE id=?",
            String.class, current.get("templateVersionId"));
        Map<String,Object> fields = objectMap(body.get("fields"));
        validateFields(parseObject(schemaJson), fields);
        int expected = number(body.get("expectedVersion"), number(current.get("currentVersion"),1));
        int next = expected + 1;
        int changed = jdbc.update("""
            UPDATE cms_content_items SET current_version=?,updated_at=now(),updated_by=?
            WHERE id=? AND current_version=? AND deleted_at IS NULL
            """, next, actor, id, expected);
        if (changed != 1) throw new IllegalStateException("Content was modified by another user. Reload before saving.");
        jdbc.update("""
            INSERT INTO cms_content_versions(content_item_id,version_no,data_snapshot,locale_metadata,created_by)
            VALUES (?, ?, ?::jsonb, ?::jsonb, ?)
            """, id, next, json(Map.of("fields",fields)),
                json(Map.of("locales",List.of("vi-VN","en-US"))), actor);
        return get(id);
    }

    public Map<String,Object> workflowActions(UUID id, String actor) {
        Map<String,Object> context = workflowContext(id);
        UUID stateId=(UUID)context.get("stateId");
        UUID workflowVersionId=(UUID)context.get("workflowVersionId");
        UUID productId=(UUID)context.get("productId");
        String stateCode=Objects.toString(context.get("stateCode"),"");
        String owner=Objects.toString(context.get("ownerUsername"),"");
        Set<String> userRoles=userRoles(actor,productId);
        List<Map<String,Object>> rows=jdbc.queryForList("""
            SELECT a.action_code AS "actionCode",a.action_name AS "actionName",
                   p.effect,r.rule_definition::text AS "ruleDefinition",
                   t.to_state_id AS "toStateId",target.state_code AS "toStateCode",
                   target.state_name AS "toStateName"
            FROM cms_workflow_action_permissions p
            JOIN cms_workflow_actions a ON a.id=p.action_id
            JOIN cms_workflow_user_rules r ON r.id=p.user_rule_id
            LEFT JOIN cms_workflow_transitions t
              ON t.workflow_version_id=p.workflow_version_id
             AND t.action_id=a.id AND t.from_state_id=p.state_id
            LEFT JOIN cms_workflow_states target ON target.id=t.to_state_id
            WHERE p.workflow_version_id=? AND p.state_id=?
            ORDER BY a.action_name
            """,workflowVersionId,stateId);
        Map<String,Map<String,Object>> decisions=new LinkedHashMap<>();
        for(Map<String,Object> row:rows) {
            String code=Objects.toString(row.get("actionCode"),"");
            Map<String,Object> decision=decisions.computeIfAbsent(code,k->{
                Map<String,Object> x=new LinkedHashMap<>();
                x.put("code",code);x.put("name",row.get("actionName"));
                x.put("allowed",false);x.put("denied",false);
                x.put("toStateCode",row.get("toStateCode"));x.put("toStateName",row.get("toStateName"));
                x.put("changesState",row.get("toStateId")!=null);
                return x;
            });
            Map<String,Object> rule=parseObject(Objects.toString(row.get("ruleDefinition"),"{}"));
            if(matchesWorkflowRule(rule,userRoles,actor,owner)) {
                if("DENY".equalsIgnoreCase(Objects.toString(row.get("effect"),"ALLOW")))decision.put("denied",true);
                else decision.put("allowed",true);
            }
        }
        List<Map<String,Object>> available=new ArrayList<>();
        for(Map<String,Object> decision:decisions.values()) {
            boolean allowed=Boolean.TRUE.equals(decision.get("allowed"))&&!Boolean.TRUE.equals(decision.get("denied"));
            String code=Objects.toString(decision.get("code"),"");
            boolean hasTransition=Boolean.TRUE.equals(decision.get("changesState"));
            if(allowed && (hasTransition || "SAVE".equals(code))) available.add(decision);
        }
        boolean ownerCanDelete=actor!=null && actor.equals(owner)
            && (Boolean.TRUE.equals(context.get("isInitial")) || "REJECTED".equals(stateCode));
        if(ownerCanDelete && available.stream().noneMatch(a->"DELETE".equals(a.get("code")))) {
            Map<String,Object> delete=new LinkedHashMap<>();
            delete.put("code","DELETE");delete.put("name","Delete");delete.put("allowed",true);
            delete.put("changesState",false);delete.put("toStateCode",stateCode);available.add(delete);
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("stateCode",stateCode);result.put("stateName",context.get("stateName"));
        result.put("lockVersion",context.get("lockVersion"));result.put("availableActions",available);
        result.put("canEdit",available.stream().anyMatch(a->"SAVE".equals(a.get("code"))));
        return result;
    }

    @Transactional
    public Map<String,Object> executeWorkflowAction(UUID id,Map<String,Object> body,String actor) {
        String action=required(body,"action").toUpperCase(Locale.ROOT);
        Map<String,Object> context=workflowContext(id);
        Map<String,Object> availability=workflowActions(id,actor);
        List<?> actions=(List<?>)availability.get("availableActions");
        Map<String,Object> selected=null;
        if(actions!=null)for(Object raw:actions)if(raw instanceof Map<?,?> m && action.equals(Objects.toString(m.get("code"),""))){
            selected=objectMap(m);break;
        }
        if(selected==null)throw new AccessDeniedException("You are not allowed to perform workflow action "+action+" in state "+context.get("stateCode"));
        long lockVersion=number(body.get("expectedLockVersion"),number(context.get("lockVersion"),0));
        long currentLock=number(context.get("lockVersion"),0);
        if(lockVersion!=currentLock)throw new IllegalStateException("Workflow state was changed by another user. Reload before continuing.");
        String from=Objects.toString(context.get("stateCode"),"");
        UUID workflowInstanceId=(UUID)context.get("workflowInstanceId");
        UUID workflowVersionId=(UUID)context.get("workflowVersionId");
        UUID stateId=(UUID)context.get("stateId");
        List<UUID> targets=jdbc.query("""
            SELECT t.to_state_id
            FROM cms_workflow_transitions t
            JOIN cms_workflow_actions a ON a.id=t.action_id
            WHERE t.workflow_version_id=? AND t.from_state_id=? AND a.action_code=?
            """,(rs,n)->rs.getObject(1,UUID.class),workflowVersionId,stateId,action);
        UUID targetStateId=targets.isEmpty()?stateId:targets.getFirst();
        String to=jdbc.queryForObject("SELECT state_code FROM cms_workflow_states WHERE id=?",String.class,targetStateId);
        int changed=jdbc.update("""
            UPDATE cms_workflow_instances SET current_state_id=?,lock_version=lock_version+1,updated_at=now()
            WHERE id=? AND lock_version=?
            """,targetStateId,workflowInstanceId,lockVersion);
        if(changed!=1)throw new IllegalStateException("Workflow state was changed by another user. Reload before continuing.");
        String comment=body.get("comment")==null?null:String.valueOf(body.get("comment"));
        jdbc.update("""
            INSERT INTO cms_workflow_history(workflow_instance_id,action_code,from_state_code,to_state_code,actor_username,comment)
            VALUES (?,?,?,?,?,?)
            """,workflowInstanceId,action,from,to,actor,comment);
        if("DELETE".equals(action)) {
            jdbc.update("UPDATE cms_content_items SET deleted_at=now(),updated_at=now(),updated_by=? WHERE id=? AND deleted_at IS NULL",actor,id);
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("contentItemId",id);result.put("action",action);result.put("fromState",from);
        result.put("toState",to);result.put("lockVersion",lockVersion+1);result.put("deleted","DELETE".equals(action));
        return result;
    }

    @Transactional
    public void archive(UUID id,String actor) {
        executeWorkflowAction(id,Map.of("action","DELETE","comment","Deleted by content owner"),actor);
    }

    private Map<String,Object> workflowContext(UUID id) {
        return jdbc.queryForMap("""
            SELECT wi.id AS "workflowInstanceId",wi.workflow_version_id AS "workflowVersionId",
                   wi.current_state_id AS "stateId",wi.lock_version AS "lockVersion",
                   i.product_id AS "productId",i.owner_username AS "ownerUsername",
                   ws.state_code AS "stateCode",ws.state_name AS "stateName",ws.is_initial AS "isInitial"
            FROM cms_content_items i
            JOIN cms_workflow_instances wi ON wi.content_item_id=i.id
            JOIN cms_workflow_states ws ON ws.id=wi.current_state_id
            WHERE i.id=? AND i.deleted_at IS NULL
            """,id);
    }

    private Set<String> userRoles(String actor,UUID productId) {
        if(actor==null||productId==null)return Set.of();
        return new HashSet<>(jdbc.query("""
            SELECT DISTINCT r.code
            FROM app_users u
            JOIN user_product_roles upr ON upr.user_id=u.id
            JOIN roles r ON r.id=upr.role_id
            WHERE (u.username=? OR u.external_subject=?) AND u.status='ACTIVE'
              AND upr.product_id=? AND (upr.expires_at IS NULL OR upr.expires_at>now())
            """,(rs,n)->rs.getString(1),actor,actor,productId));
    }

    private boolean matchesWorkflowRule(Map<String,Object> rule,Set<String> roles,String actor,String owner) {
        Object rawRoles=rule.get("roles");
        if(!(rawRoles instanceof List<?> requiredRoles)||requiredRoles.isEmpty())return false;
        boolean roleMatch=roles.contains("SYSTEM_ADMIN")||requiredRoles.stream().anyMatch(raw->{
            String required=String.valueOf(raw).toUpperCase(Locale.ROOT);
            if(roles.contains(required))return true;
            // Default VIB role aliases: BU acts as Editor; PO_ADMIN as Manager;
            // DEV_ADMIN/PO_ADMIN can act as Publisher when assigned to the Product.
            return switch(required){
                case "EDITOR" -> roles.contains("BU");
                case "MANAGER" -> roles.contains("PO_ADMIN");
                case "PUBLISHER" -> roles.contains("DEV_ADMIN")||roles.contains("PO_ADMIN");
                default -> false;
            };
        });
        if(!roleMatch)return false;
        String scope=Objects.toString(rule.get("scope"),"PRODUCT").toUpperCase(Locale.ROOT);
        if(Set.of("OWNER","OWNED","CREATED_BY_ME").contains(scope))return actor!=null&&actor.equals(owner);
        return true;
    }

    private UUID resolveWorkflow(UUID productId, UUID contentTypeId) {
        List<UUID> overrides = jdbc.query("""
            SELECT workflow_version_id FROM cms_workflow_bindings
            WHERE product_id=? AND content_type_id=? AND binding_type='CONTENT_TYPE_OVERRIDE'
            """, (rs,n)->rs.getObject(1,UUID.class), productId, contentTypeId);
        if (!overrides.isEmpty()) return overrides.getFirst();
        List<UUID> defaults = jdbc.query("""
            SELECT workflow_version_id FROM cms_workflow_bindings
            WHERE product_id=? AND binding_type='PRODUCT_DEFAULT'
            """, (rs,n)->rs.getObject(1,UUID.class), productId);
        if (defaults.isEmpty()) throw new IllegalStateException("No Workflow is bound to this Content Type or Product");
        return defaults.getFirst();
    }

    private void validateFields(Map<String,Object> schema, Map<String,Object> values) {
        Object raw = schema.get("fields");
        if (!(raw instanceof List<?> fields)) return;
        for (Object entry : fields) {
            if (!(entry instanceof Map<?,?> field)) continue;
            String code = Objects.toString(field.get("code"), "");
            String type = Objects.toString(field.get("type"), "text");
            Object value = values.get(code);
            boolean required = Boolean.TRUE.equals(field.get("required"));
            boolean localized = Boolean.TRUE.equals(field.get("localized"));
            if (localized && value != null && !(value instanceof Map<?,?>)) {
                throw new IllegalArgumentException(code+" must contain locale-keyed values");
            }
            if (localized && value instanceof Map<?,?> localeValues) {
                if (required && localeValues.values().stream().allMatch(this::blank)) throw new IllegalArgumentException(code+" is required");
            } else if (required && blank(value)) {
                throw new IllegalArgumentException(code+" is required");
            }
            boolean repeater = "repeater".equals(type) || "REPEATER".equals(Objects.toString(field.get("cardinality"), ""));
            if (blank(value) && !repeater) continue;
            if (repeater) {
                List<?> rows;
                if (value == null) rows=List.of();
                else if (value instanceof List<?> list) rows=list;
                else throw new IllegalArgumentException(code+" must be a list");
                Map<String,Object> options=objectMap(field.get("options"));
                int min=number(options.get("minItems"),0), max=number(options.get("maxItems"),0);
                if (rows.size()<min) throw new IllegalArgumentException(code+" requires at least "+min+" items");
                if (max>0 && rows.size()>max) throw new IllegalArgumentException(code+" allows at most "+max+" items");
                Set<String> childCodes=new HashSet<>();
                Object rawChildren=field.get("itemFields");
                if(rawChildren instanceof List<?> children) for(Object child:children) childCodes.add(String.valueOf(child));
                Set<String> requiredChildren=new HashSet<>();
                Object rawSchemaFields=schema.get("fields");
                if(rawSchemaFields instanceof List<?> schemaFields) {
                    for(Object rawChild:schemaFields) if(rawChild instanceof Map<?,?> child
                            && childCodes.contains(Objects.toString(child.get("code"),""))
                            && Boolean.TRUE.equals(child.get("required"))) {
                        requiredChildren.add(Objects.toString(child.get("code"),""));
                    }
                }
                for(int index=0;index<rows.size();index++) {
                    Map<String,Object> row=objectMap(rows.get(index));
                    for(String childCode:requiredChildren) {
                        Object childValue=row.get(childCode);
                        if(blank(childValue)) throw new IllegalArgumentException(code+" item "+(index+1)+": "+childCode+" is required");
                    }
                }
            } else if (Boolean.TRUE.equals(field.get("multiple")) || "MULTIPLE".equals(field.get("cardinality"))) {
                if (!(value instanceof List<?>)) throw new IllegalArgumentException(code+" must be a list");
            }
            Map<String,Object> validation = objectMap(field.get("validation"));
            String sample = localized && value instanceof Map<?,?> m
                ? Objects.toString(m.get("vi-VN"), m.values().stream().findFirst().map(String::valueOf).orElse("")) : Objects.toString(value,"");
            if (validation.get("minLength") instanceof Number min && sample.length()<min.intValue())
                throw new IllegalArgumentException(code+" must contain at least "+min+" characters");
            if (validation.get("maxLength") instanceof Number max && sample.length()>max.intValue())
                throw new IllegalArgumentException(code+" must contain at most "+max+" characters");
            Object pattern = validation.get("pattern");
            if (pattern instanceof String p && !p.isBlank() && !sample.matches(p))
                throw new IllegalArgumentException(code+" has invalid format");
            if (Set.of("number","currency","percentage","slider","rating").contains(type)) {
                try {
                    java.math.BigDecimal numeric = new java.math.BigDecimal(sample);
                    if (validation.get("min") != null
                            && numeric.compareTo(new java.math.BigDecimal(String.valueOf(validation.get("min")))) < 0)
                        throw new IllegalArgumentException(code+" must be greater than or equal to "+validation.get("min"));
                    if (validation.get("max") != null
                            && numeric.compareTo(new java.math.BigDecimal(String.valueOf(validation.get("max")))) > 0)
                        throw new IllegalArgumentException(code+" must be less than or equal to "+validation.get("max"));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(code+" must be a valid number");
                }
            }
            Object allowedRaw=objectMap(field.get("options")).get("values");
            if (allowedRaw instanceof List<?> allowed && !allowed.isEmpty()) {
                if (value instanceof List<?> selected) {
                    for (Object option:selected) if (!allowed.contains(option))
                        throw new IllegalArgumentException(code+" contains an unsupported option");
                } else if (Set.of("select","radio").contains(type) && !allowed.contains(value)) {
                    throw new IllegalArgumentException(code+" contains an unsupported option");
                }
            }
            if ("email".equals(type) && !sample.matches("^[^\\\\s@]+@[^\\\\s@]+\\\\.[^\\\\s@]+$"))
                throw new IllegalArgumentException(code+" must be a valid email address");
        }
    }

    private boolean blank(Object value) {
        if (value==null) return true;
        if (value instanceof String s) return s.isBlank();
        if (value instanceof Collection<?> c) return c.isEmpty();
        if (value instanceof Map<?,?> m) return m.isEmpty() || m.values().stream().allMatch(this::blank);
        return false;
    }
    private Map<String,Object> objectMap(Object value) {
        if (value instanceof Map<?,?> m) {
            Map<String,Object> out=new LinkedHashMap<>();
            m.forEach((k,v)->out.put(String.valueOf(k),v));
            return out;
        }
        return new LinkedHashMap<>();
    }
    private Map<String,Object> parseObject(String value) {
        try { return mapper.readValue(value, MAP_TYPE); }
        catch(Exception e) { throw new IllegalStateException("Unable to read stored JSON",e); }
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch(Exception e) { throw new IllegalArgumentException("Invalid content JSON",e); }
    }
    private String required(Map<String,Object> body,String key) {
        Object value=body.get(key);
        if(value==null||String.valueOf(value).isBlank()) throw new IllegalArgumentException(key+" is required");
        return String.valueOf(value).trim();
    }
    private int number(Object value,int fallback) {
        if(value instanceof Number n)return n.intValue();
        if(value==null)return fallback;
        return Integer.parseInt(String.valueOf(value));
    }
}
