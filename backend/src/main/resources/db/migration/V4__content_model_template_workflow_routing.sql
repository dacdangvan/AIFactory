-- Phase 1.3 redesign: product-scoped content types, template-owned URL rules and workflow authorization.
-- Existing legacy cms_articles tables are intentionally retained for backward compatibility.

CREATE TABLE cms_content_types (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id),
    parent_id uuid REFERENCES cms_content_types(id),
    code varchar(150) NOT NULL,
    name varchar(255) NOT NULL,
    description text,
    sort_order integer NOT NULL DEFAULT 0,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by varchar(255) NOT NULL,
    CONSTRAINT uq_cms_content_type_product_code UNIQUE(product_id, code),
    CONSTRAINT ck_cms_content_type_not_self CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX idx_cms_content_types_product_parent
    ON cms_content_types(product_id, parent_id, sort_order);

CREATE TABLE cms_authoring_templates (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id),
    code varchar(150) NOT NULL,
    name varchar(255) NOT NULL,
    description text,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by varchar(255) NOT NULL,
    CONSTRAINT uq_cms_authoring_template_product_code UNIQUE(product_id, code)
);

CREATE TABLE cms_authoring_template_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id uuid NOT NULL REFERENCES cms_authoring_templates(id),
    version_no integer NOT NULL CHECK(version_no > 0),
    field_schema jsonb NOT NULL DEFAULT '{"groups":[],"fields":[]}'::jsonb
        CHECK(jsonb_typeof(field_schema) = 'object'),
    status varchar(20) NOT NULL DEFAULT 'DRAFT'
        CHECK(status IN ('DRAFT','ACTIVE','RETIRED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    UNIQUE(template_id, version_no)
);

-- Slug segments belong to a specific immutable Authoring Template version.
-- A segment can concatenate one or more fields in a deterministic order.
CREATE TABLE cms_template_url_segments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_version_id uuid NOT NULL REFERENCES cms_authoring_template_versions(id) ON DELETE CASCADE,
    segment_order integer NOT NULL CHECK(segment_order > 0),
    segment_code varchar(100) NOT NULL,
    separator varchar(20) NOT NULL DEFAULT '-',
    is_required boolean NOT NULL DEFAULT true,
    UNIQUE(template_version_id, segment_order),
    UNIQUE(template_version_id, segment_code)
);
CREATE TABLE cms_template_url_segment_fields (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    segment_id uuid NOT NULL REFERENCES cms_template_url_segments(id) ON DELETE CASCADE,
    field_code varchar(150) NOT NULL,
    field_order integer NOT NULL CHECK(field_order > 0),
    locale_aware boolean NOT NULL DEFAULT true,
    UNIQUE(segment_id, field_order),
    UNIQUE(segment_id, field_code)
);

CREATE TABLE cms_content_type_template_bindings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    content_type_id uuid NOT NULL REFERENCES cms_content_types(id),
    template_version_id uuid NOT NULL REFERENCES cms_authoring_template_versions(id),
    is_default boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    UNIQUE(content_type_id, template_version_id)
);
CREATE UNIQUE INDEX uq_cms_content_type_default_template
    ON cms_content_type_template_bindings(content_type_id) WHERE is_default = true;

CREATE TABLE cms_workflow_definitions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid REFERENCES products(id),
    code varchar(150) NOT NULL,
    name varchar(255) NOT NULL,
    description text,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK(status IN ('ACTIVE','INACTIVE','ARCHIVED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by varchar(255) NOT NULL
);
CREATE UNIQUE INDEX uq_cms_workflow_global_code
    ON cms_workflow_definitions(code) WHERE product_id IS NULL;
CREATE UNIQUE INDEX uq_cms_workflow_product_code
    ON cms_workflow_definitions(product_id, code) WHERE product_id IS NOT NULL;

CREATE TABLE cms_workflow_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id uuid NOT NULL REFERENCES cms_workflow_definitions(id),
    version_no integer NOT NULL CHECK(version_no > 0),
    status varchar(20) NOT NULL DEFAULT 'DRAFT'
        CHECK(status IN ('DRAFT','ACTIVE','RETIRED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    UNIQUE(workflow_id, version_no)
);
CREATE TABLE cms_workflow_states (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id uuid NOT NULL REFERENCES cms_workflow_versions(id) ON DELETE CASCADE,
    state_code varchar(80) NOT NULL,
    state_name varchar(150) NOT NULL,
    is_initial boolean NOT NULL DEFAULT false,
    is_terminal boolean NOT NULL DEFAULT false,
    UNIQUE(workflow_version_id, state_code)
);
CREATE UNIQUE INDEX uq_cms_workflow_initial_state
    ON cms_workflow_states(workflow_version_id) WHERE is_initial = true;

CREATE TABLE cms_workflow_actions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id uuid NOT NULL REFERENCES cms_workflow_versions(id) ON DELETE CASCADE,
    action_code varchar(80) NOT NULL,
    action_name varchar(150) NOT NULL,
    changes_state boolean NOT NULL DEFAULT true,
    config jsonb NOT NULL DEFAULT '{}'::jsonb CHECK(jsonb_typeof(config) = 'object'),
    UNIQUE(workflow_version_id, action_code)
);
CREATE TABLE cms_workflow_transitions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id uuid NOT NULL REFERENCES cms_workflow_versions(id) ON DELETE CASCADE,
    action_id uuid NOT NULL REFERENCES cms_workflow_actions(id) ON DELETE CASCADE,
    from_state_id uuid NOT NULL REFERENCES cms_workflow_states(id),
    to_state_id uuid REFERENCES cms_workflow_states(id),
    condition_config jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK(jsonb_typeof(condition_config) = 'object'),
    CHECK(to_state_id IS NULL OR from_state_id <> to_state_id),
    UNIQUE(workflow_version_id, action_id, from_state_id)
);

-- A rule is declarative data (roles, product scope, owner/assignee, user IDs, etc.).
-- It is evaluated server-side; no user-supplied executable expressions are stored.
CREATE TABLE cms_workflow_user_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id uuid NOT NULL REFERENCES cms_workflow_versions(id) ON DELETE CASCADE,
    rule_code varchar(100) NOT NULL,
    rule_name varchar(150) NOT NULL,
    rule_definition jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK(jsonb_typeof(rule_definition) = 'object'),
    UNIQUE(workflow_version_id, rule_code)
);
CREATE TABLE cms_workflow_action_permissions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id uuid NOT NULL REFERENCES cms_workflow_versions(id) ON DELETE CASCADE,
    state_id uuid NOT NULL REFERENCES cms_workflow_states(id),
    action_id uuid NOT NULL REFERENCES cms_workflow_actions(id),
    user_rule_id uuid NOT NULL REFERENCES cms_workflow_user_rules(id),
    effect varchar(10) NOT NULL DEFAULT 'ALLOW' CHECK(effect IN ('ALLOW','DENY')),
    condition_config jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK(jsonb_typeof(condition_config) = 'object'),
    UNIQUE(state_id, action_id, user_rule_id)
);
CREATE TABLE cms_workflow_visibility_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id uuid NOT NULL REFERENCES cms_workflow_versions(id) ON DELETE CASCADE,
    state_id uuid NOT NULL REFERENCES cms_workflow_states(id),
    user_rule_id uuid NOT NULL REFERENCES cms_workflow_user_rules(id),
    visibility_scope varchar(30) NOT NULL
        CHECK(visibility_scope IN ('ALL','OWNED','CREATED_BY_ME','ASSIGNED_TO_ME','NONE')),
    condition_config jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK(jsonb_typeof(condition_config) = 'object'),
    UNIQUE(state_id, user_rule_id)
);

-- Workflow may be shared globally, default at Product, or overridden by Content Type.
CREATE TABLE cms_workflow_bindings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id),
    content_type_id uuid REFERENCES cms_content_types(id),
    workflow_version_id uuid NOT NULL REFERENCES cms_workflow_versions(id),
    binding_type varchar(20) NOT NULL CHECK(binding_type IN ('PRODUCT_DEFAULT','CONTENT_TYPE_OVERRIDE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    CHECK((binding_type = 'PRODUCT_DEFAULT' AND content_type_id IS NULL)
       OR (binding_type = 'CONTENT_TYPE_OVERRIDE' AND content_type_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_cms_product_default_workflow
    ON cms_workflow_bindings(product_id) WHERE binding_type = 'PRODUCT_DEFAULT';
CREATE UNIQUE INDEX uq_cms_content_type_workflow
    ON cms_workflow_bindings(content_type_id) WHERE binding_type = 'CONTENT_TYPE_OVERRIDE';

CREATE TABLE cms_content_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id),
    content_type_id uuid NOT NULL REFERENCES cms_content_types(id),
    template_version_id uuid NOT NULL REFERENCES cms_authoring_template_versions(id),
    workflow_version_id uuid NOT NULL REFERENCES cms_workflow_versions(id),
    code varchar(150) NOT NULL,
    owner_username varchar(255) NOT NULL,
    created_by varchar(255) NOT NULL,
    updated_by varchar(255) NOT NULL,
    current_version integer NOT NULL DEFAULT 1 CHECK(current_version > 0),
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(product_id, code)
);
CREATE TABLE cms_content_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    content_item_id uuid NOT NULL REFERENCES cms_content_items(id),
    version_no integer NOT NULL CHECK(version_no > 0),
    data_snapshot jsonb NOT NULL CHECK(jsonb_typeof(data_snapshot) = 'object'),
    locale_metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK(jsonb_typeof(locale_metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    UNIQUE(content_item_id, version_no)
);
CREATE TABLE cms_workflow_instances (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id uuid NOT NULL REFERENCES cms_workflow_versions(id),
    content_item_id uuid NOT NULL UNIQUE REFERENCES cms_content_items(id),
    current_state_id uuid NOT NULL REFERENCES cms_workflow_states(id),
    lock_version bigint NOT NULL DEFAULT 0,
    started_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE cms_workflow_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id uuid NOT NULL REFERENCES cms_workflow_instances(id),
    action_code varchar(80) NOT NULL,
    from_state_code varchar(80),
    to_state_code varchar(80) NOT NULL,
    actor_username varchar(255) NOT NULL,
    comment text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE cms_content_item_routes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    content_item_id uuid NOT NULL REFERENCES cms_content_items(id),
    locale varchar(20) NOT NULL,
    product_slug varchar(300) NOT NULL,
    route_path varchar(1500) NOT NULL,
    is_published boolean NOT NULL DEFAULT false,
    content_version_no integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(content_item_id, locale),
    UNIQUE(route_path)
);
CREATE TABLE cms_content_item_route_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    content_item_id uuid NOT NULL REFERENCES cms_content_items(id),
    locale varchar(20) NOT NULL,
    old_route_path varchar(1500) NOT NULL,
    new_route_path varchar(1500) NOT NULL,
    redirect_status integer NOT NULL DEFAULT 301 CHECK(redirect_status IN (301,302,307,308)),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    UNIQUE(old_route_path)
);

CREATE INDEX idx_cms_content_items_product_type
    ON cms_content_items(product_id, content_type_id, updated_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_cms_content_versions_latest
    ON cms_content_versions(content_item_id, version_no DESC);
CREATE INDEX idx_cms_workflow_instances_state
    ON cms_workflow_instances(workflow_version_id, current_state_id);
CREATE INDEX idx_cms_routes_path_published
    ON cms_content_item_routes(route_path) WHERE is_published = true;
