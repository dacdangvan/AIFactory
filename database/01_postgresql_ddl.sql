BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE products (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code varchar(100) NOT NULL UNIQUE,
 name varchar(255) NOT NULL, description text, status varchar(20) NOT NULL DEFAULT 'ACTIVE'
 CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED')), created_at timestamptz NOT NULL DEFAULT now(),
 created_by varchar(255), updated_at timestamptz NOT NULL DEFAULT now(), updated_by varchar(255)
);
CREATE TABLE app_users (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), username varchar(150) NOT NULL UNIQUE,
 email varchar(320), display_name varchar(255), identity_source varchar(50) NOT NULL DEFAULT 'MICROSOFT_SSO',
 external_subject varchar(255), status varchar(20) NOT NULL DEFAULT 'ACTIVE'
 CHECK (status IN ('ACTIVE','INACTIVE','LOCKED')), created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(), UNIQUE(identity_source, external_subject)
);
CREATE TABLE roles (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code varchar(100) NOT NULL UNIQUE,
 name varchar(255) NOT NULL, description text, is_system boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE permissions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), resource_type varchar(80) NOT NULL,
 action_code varchar(80) NOT NULL, state_code varchar(80),
 scope_type varchar(40) NOT NULL DEFAULT 'PRODUCT' CHECK(scope_type IN ('GLOBAL','PRODUCT','RESOURCE')),
 effect varchar(10) NOT NULL DEFAULT 'ALLOW' CHECK(effect IN ('ALLOW','DENY')),
 description text, UNIQUE(resource_type, action_code, state_code, scope_type, effect)
);
CREATE TABLE role_permissions (
 role_id uuid NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
 permission_id uuid NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
 PRIMARY KEY(role_id, permission_id)
);
CREATE TABLE user_product_roles (
 user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
 role_id uuid NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
 assigned_by varchar(255), assigned_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz,
 PRIMARY KEY(user_id, product_id, role_id), CHECK(expires_at IS NULL OR expires_at > assigned_at)
);

CREATE TABLE themes (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 code varchar(100) NOT NULL, name varchar(255) NOT NULL, root_base varchar(500) NOT NULL DEFAULT '/',
 domains jsonb NOT NULL DEFAULT '[]', head_config jsonb NOT NULL DEFAULT '{}',
 body_config jsonb NOT NULL DEFAULT '{}', status varchar(20) NOT NULL DEFAULT 'ACTIVE'
 CHECK(status IN ('ACTIVE','INACTIVE','ARCHIVED')), created_at timestamptz NOT NULL DEFAULT now(),
 created_by varchar(255), updated_at timestamptz NOT NULL DEFAULT now(), updated_by varchar(255),
 UNIQUE(product_id, code), CHECK(jsonb_typeof(domains)='array'),
 CHECK(jsonb_typeof(head_config)='object'), CHECK(jsonb_typeof(body_config)='object')
);
CREATE TABLE layouts (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 code varchar(100) NOT NULL, name varchar(255) NOT NULL, description text,
 status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE','ARCHIVED')),
 created_at timestamptz NOT NULL DEFAULT now(), created_by varchar(255),
 updated_at timestamptz NOT NULL DEFAULT now(), updated_by varchar(255), UNIQUE(product_id, code)
);
CREATE TABLE layout_versions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), layout_id uuid NOT NULL REFERENCES layouts(id),
 version_no integer NOT NULL CHECK(version_no>0), definition jsonb NOT NULL DEFAULT '{}',
 status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','APPROVED','RETIRED')),
 created_at timestamptz NOT NULL DEFAULT now(), created_by varchar(255),
 UNIQUE(layout_id, version_no), CHECK(jsonb_typeof(definition)='object')
);
CREATE TABLE pages (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), theme_id uuid NOT NULL REFERENCES themes(id),
 parent_id uuid REFERENCES pages(id), code varchar(150) NOT NULL, slug varchar(500) NOT NULL,
 page_type varchar(20) NOT NULL DEFAULT 'NORMAL' CHECK(page_type IN ('NORMAL','TAG')),
 sort_order integer NOT NULL DEFAULT 0, status varchar(20) NOT NULL DEFAULT 'ACTIVE'
 CHECK(status IN ('ACTIVE','INACTIVE','ARCHIVED')), created_at timestamptz NOT NULL DEFAULT now(),
 created_by varchar(255), updated_at timestamptz NOT NULL DEFAULT now(), updated_by varchar(255),
 UNIQUE(theme_id, code), CHECK(parent_id IS NULL OR parent_id<>id)
);
CREATE INDEX idx_pages_theme_parent_order ON pages(theme_id,parent_id,sort_order);
CREATE INDEX idx_pages_theme_slug ON pages(theme_id,slug);
CREATE TABLE page_versions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), page_id uuid NOT NULL REFERENCES pages(id),
 version_no integer NOT NULL CHECK(version_no>0), layout_version_id uuid REFERENCES layout_versions(id),
 redirect_config jsonb NOT NULL DEFAULT '{}', seo_snapshot jsonb NOT NULL DEFAULT '{}',
 locale_config jsonb NOT NULL DEFAULT '["vi-VN","en-US"]', snapshot jsonb NOT NULL DEFAULT '{}',
 workflow_state varchar(50) NOT NULL DEFAULT 'DRAFT', created_at timestamptz NOT NULL DEFAULT now(),
 created_by varchar(255), UNIQUE(page_id,version_no),
 CHECK(jsonb_typeof(redirect_config)='object'), CHECK(jsonb_typeof(seo_snapshot)='object'),
 CHECK(jsonb_typeof(locale_config)='array'), CHECK(jsonb_typeof(snapshot)='object')
);

CREATE TABLE templates (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 code varchar(150) NOT NULL, name varchar(255) NOT NULL, description text,
 status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE','ARCHIVED')),
 created_at timestamptz NOT NULL DEFAULT now(), created_by varchar(255),
 updated_at timestamptz NOT NULL DEFAULT now(), updated_by varchar(255), UNIQUE(product_id,code)
);
CREATE TABLE template_versions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), template_id uuid NOT NULL REFERENCES templates(id),
 version_no integer NOT NULL CHECK(version_no>0), schema_snapshot jsonb NOT NULL,
 workflow_id uuid, workflow_version_id uuid,
 status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','APPROVED','RETIRED')),
 created_at timestamptz NOT NULL DEFAULT now(), created_by varchar(255),
 UNIQUE(template_id,version_no), CHECK(jsonb_typeof(schema_snapshot)='object')
);
CREATE TABLE field_groups (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), template_version_id uuid NOT NULL REFERENCES template_versions(id) ON DELETE CASCADE,
 code varchar(150) NOT NULL, name varchar(255) NOT NULL, sort_order integer NOT NULL DEFAULT 0,
 UNIQUE(template_version_id,code)
);
CREATE TABLE field_definitions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), field_group_id uuid NOT NULL REFERENCES field_groups(id) ON DELETE CASCADE,
 field_key varchar(150) NOT NULL, label varchar(255) NOT NULL,
 field_type varchar(30) NOT NULL CHECK(field_type IN
 ('TEXT','DATE_TIME','FILE','HTML','IMAGE','LINK','NUMBER','OPTION_SELECTION','RICH_TEXT','SLUG')),
 required boolean NOT NULL DEFAULT false, repeatable boolean NOT NULL DEFAULT false,
 localized boolean NOT NULL DEFAULT false, show_breadcrumb boolean NOT NULL DEFAULT false,
 validation_rules jsonb NOT NULL DEFAULT '{}', display_config jsonb NOT NULL DEFAULT '{}',
 options_config jsonb NOT NULL DEFAULT '{}', sort_order integer NOT NULL DEFAULT 0,
 UNIQUE(field_group_id,field_key), CHECK(jsonb_typeof(validation_rules)='object'),
 CHECK(jsonb_typeof(display_config)='object'), CHECK(jsonb_typeof(options_config)='object')
);
CREATE TABLE contents (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 template_id uuid NOT NULL REFERENCES templates(id), code varchar(150) NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE','ARCHIVED')),
 created_at timestamptz NOT NULL DEFAULT now(), created_by varchar(255),
 updated_at timestamptz NOT NULL DEFAULT now(), updated_by varchar(255), UNIQUE(product_id,code)
);
CREATE TABLE content_versions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), content_id uuid NOT NULL REFERENCES contents(id),
 version_no integer NOT NULL CHECK(version_no>0),
 template_version_id uuid NOT NULL REFERENCES template_versions(id),
 -- Canonical shared-locale snapshot: {"vi-VN":{...},"en-US":{...}}
 data_snapshot jsonb NOT NULL, locale_metadata jsonb NOT NULL DEFAULT '{}',
 workflow_state varchar(50) NOT NULL DEFAULT 'DRAFT', created_at timestamptz NOT NULL DEFAULT now(),
 created_by varchar(255), UNIQUE(content_id,version_no),
 CHECK(jsonb_typeof(data_snapshot)='object'), CHECK(jsonb_typeof(locale_metadata)='object')
);
CREATE TABLE content_field_values (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 content_version_id uuid NOT NULL REFERENCES content_versions(id) ON DELETE CASCADE,
 field_definition_id uuid NOT NULL REFERENCES field_definitions(id),
 locale varchar(20) NOT NULL, item_index integer NOT NULL DEFAULT 0 CHECK(item_index>=0),
 value jsonb, search_text text,
 UNIQUE(content_version_id,field_definition_id,locale,item_index)
);
CREATE INDEX idx_content_field_value_lookup ON content_field_values(field_definition_id,locale);
CREATE INDEX idx_content_field_value_json ON content_field_values USING gin(value jsonb_path_ops);
CREATE INDEX idx_content_versions_state ON content_versions(content_id,workflow_state);

CREATE TABLE components (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 code varchar(150) NOT NULL, name varchar(255) NOT NULL,
 component_type varchar(20) NOT NULL CHECK(component_type IN ('HTML','NEXTJS','FILE')),
 description text, status varchar(20) NOT NULL DEFAULT 'ACTIVE'
 CHECK(status IN ('ACTIVE','INACTIVE','ARCHIVED')), created_at timestamptz NOT NULL DEFAULT now(),
 created_by varchar(255), updated_at timestamptz NOT NULL DEFAULT now(), updated_by varchar(255),
 UNIQUE(product_id,code)
);
CREATE TABLE component_versions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), component_id uuid NOT NULL REFERENCES components(id),
 version_no integer NOT NULL CHECK(version_no>0), runtime_code varchar(150),
 runtime_version varchar(50), deployment_version varchar(150),
 settings_schema jsonb NOT NULL DEFAULT '{}', content_schema jsonb NOT NULL DEFAULT '{}',
 default_settings jsonb NOT NULL DEFAULT '{}',
 status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','APPROVED','DEPLOYED','RETIRED')),
 created_at timestamptz NOT NULL DEFAULT now(), created_by varchar(255),
 UNIQUE(component_id,version_no), CHECK(jsonb_typeof(settings_schema)='object'),
 CHECK(jsonb_typeof(content_schema)='object'), CHECK(jsonb_typeof(default_settings)='object')
);

CREATE TABLE page_sections (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), page_version_id uuid NOT NULL REFERENCES page_versions(id) ON DELETE CASCADE,
 section_code varchar(150) NOT NULL, component_version_id uuid NOT NULL REFERENCES component_versions(id),
 content_version_id uuid REFERENCES content_versions(id), sort_order integer NOT NULL DEFAULT 0,
 use_for_seo boolean NOT NULL DEFAULT false, settings_snapshot jsonb NOT NULL DEFAULT '{}',
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(page_version_id,section_code),
 CHECK(jsonb_typeof(settings_snapshot)='object')
);
CREATE INDEX idx_page_sections_order ON page_sections(page_version_id,sort_order);

CREATE TABLE asset_folders (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 parent_id uuid REFERENCES asset_folders(id), name varchar(255) NOT NULL, path varchar(1000),
 created_at timestamptz NOT NULL DEFAULT now(), created_by varchar(255)
);
CREATE TABLE assets (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 folder_id uuid REFERENCES asset_folders(id), file_name varchar(500) NOT NULL,
 mime_type varchar(150) NOT NULL, file_size bigint NOT NULL CHECK(file_size>=0),
 checksum_sha256 varchar(64), status varchar(30) NOT NULL DEFAULT 'UPLOADING'
 CHECK(status IN ('UPLOADING','SCANNING','READY','REJECTED','DELETED')),
 current_version_no integer NOT NULL DEFAULT 1, created_at timestamptz NOT NULL DEFAULT now(),
 created_by varchar(255), updated_at timestamptz NOT NULL DEFAULT now(), updated_by varchar(255)
);
CREATE TABLE asset_versions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), asset_id uuid NOT NULL REFERENCES assets(id),
 version_no integer NOT NULL CHECK(version_no>0), bucket_name varchar(255) NOT NULL,
 object_key varchar(1000) NOT NULL, checksum_sha256 varchar(64),
 file_size bigint NOT NULL CHECK(file_size>=0), mime_type varchar(150) NOT NULL,
 scan_status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK(scan_status IN ('PENDING','CLEAN','INFECTED','ERROR')),
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(asset_id,version_no), UNIQUE(bucket_name,object_key)
);
CREATE TABLE asset_references (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), asset_id uuid NOT NULL REFERENCES assets(id),
 owner_type varchar(50) NOT NULL, owner_id uuid NOT NULL, usage_type varchar(80) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(asset_id,owner_type,owner_id,usage_type)
);
CREATE INDEX idx_asset_references_owner ON asset_references(owner_type,owner_id);

CREATE TABLE workflows (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 code varchar(150) NOT NULL, name varchar(255) NOT NULL, resource_type varchar(80) NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE','ARCHIVED')),
 UNIQUE(product_id,code)
);
CREATE TABLE workflow_versions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), workflow_id uuid NOT NULL REFERENCES workflows(id),
 version_no integer NOT NULL CHECK(version_no>0), status varchar(20) NOT NULL DEFAULT 'DRAFT'
 CHECK(status IN ('DRAFT','APPROVED','RETIRED')), definition jsonb NOT NULL DEFAULT '{}',
 created_at timestamptz NOT NULL DEFAULT now(), created_by varchar(255),
 UNIQUE(workflow_id,version_no), CHECK(jsonb_typeof(definition)='object')
);
ALTER TABLE template_versions ADD CONSTRAINT fk_template_workflow FOREIGN KEY(workflow_id) REFERENCES workflows(id);
ALTER TABLE template_versions ADD CONSTRAINT fk_template_workflow_version FOREIGN KEY(workflow_version_id) REFERENCES workflow_versions(id);
CREATE TABLE workflow_states (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), workflow_version_id uuid NOT NULL REFERENCES workflow_versions(id) ON DELETE CASCADE,
 state_code varchar(80) NOT NULL, state_name varchar(150) NOT NULL,
 is_initial boolean NOT NULL DEFAULT false, is_terminal boolean NOT NULL DEFAULT false,
 UNIQUE(workflow_version_id,state_code)
);
CREATE UNIQUE INDEX uq_workflow_initial_state ON workflow_states(workflow_version_id) WHERE is_initial;
CREATE TABLE workflow_transitions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), workflow_version_id uuid NOT NULL REFERENCES workflow_versions(id) ON DELETE CASCADE,
 action_code varchar(80) NOT NULL, from_state_id uuid NOT NULL REFERENCES workflow_states(id),
 to_state_id uuid NOT NULL REFERENCES workflow_states(id), permission_code varchar(100) NOT NULL,
 config jsonb NOT NULL DEFAULT '{}', UNIQUE(workflow_version_id,action_code,from_state_id),
 CHECK(from_state_id<>to_state_id), CHECK(jsonb_typeof(config)='object')
);
CREATE TABLE workflow_instances (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), workflow_id uuid NOT NULL REFERENCES workflows(id),
 workflow_version_id uuid NOT NULL REFERENCES workflow_versions(id), product_id uuid NOT NULL REFERENCES products(id),
 resource_type varchar(80) NOT NULL, resource_id uuid NOT NULL,
 current_state_id uuid NOT NULL REFERENCES workflow_states(id), lock_version bigint NOT NULL DEFAULT 0,
 started_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(resource_type,resource_id)
);
CREATE TABLE workflow_histories (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), workflow_instance_id uuid NOT NULL REFERENCES workflow_instances(id),
 action_code varchar(80) NOT NULL, from_state_code varchar(80), to_state_code varchar(80) NOT NULL,
 actor_user_id uuid REFERENCES app_users(id), actor_username varchar(150), comment text,
 correlation_id varchar(100), created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE publish_releases (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 theme_id uuid REFERENCES themes(id), release_code varchar(150) NOT NULL,
 status varchar(30) NOT NULL DEFAULT 'DRAFT'
 CHECK(status IN ('DRAFT','VALIDATING','READY','PUBLISHING','PUBLISHED','FAILED','ROLLED_BACK')),
 manifest jsonb NOT NULL DEFAULT '{}', idempotency_key varchar(255), comment text,
 created_by varchar(255), created_at timestamptz NOT NULL DEFAULT now(),
 published_by varchar(255), published_at timestamptz,
 UNIQUE(product_id,release_code), UNIQUE(product_id,idempotency_key),
 CHECK(jsonb_typeof(manifest)='object')
);
CREATE TABLE publish_release_items (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), release_id uuid NOT NULL REFERENCES publish_releases(id) ON DELETE CASCADE,
 resource_type varchar(40) NOT NULL CHECK(resource_type IN ('PAGE','CONTENT','COMPONENT','LAYOUT','TEMPLATE','THEME')),
 resource_id uuid NOT NULL, version_id uuid NOT NULL, version_no integer NOT NULL CHECK(version_no>0),
 locale_scope jsonb NOT NULL DEFAULT '["vi-VN","en-US"]', checksum_sha256 varchar(64),
 UNIQUE(release_id,resource_type,resource_id), CHECK(jsonb_typeof(locale_scope)='array')
);
CREATE TABLE publish_events (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), release_id uuid NOT NULL REFERENCES publish_releases(id),
 event_type varchar(80) NOT NULL, status varchar(20) NOT NULL DEFAULT 'PENDING'
 CHECK(status IN ('PENDING','PROCESSING','SUCCEEDED','FAILED')),
 payload jsonb NOT NULL DEFAULT '{}', created_at timestamptz NOT NULL DEFAULT now(), processed_at timestamptz
);
CREATE TABLE outbox_events (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), aggregate_type varchar(80) NOT NULL,
 aggregate_id uuid NOT NULL, event_type varchar(100) NOT NULL, payload jsonb NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'PENDING'
 CHECK(status IN ('PENDING','PROCESSING','PUBLISHED','FAILED')),
 retry_count integer NOT NULL DEFAULT 0 CHECK(retry_count>=0), next_retry_at timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(), published_at timestamptz,
 CHECK(jsonb_typeof(payload)='object')
);
CREATE INDEX idx_outbox_dispatch ON outbox_events(status,next_retry_at,created_at);
CREATE TABLE publish_history (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), release_id uuid NOT NULL REFERENCES publish_releases(id),
 action_code varchar(80) NOT NULL, actor_username varchar(150), details jsonb NOT NULL DEFAULT '{}',
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE audit_logs (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid REFERENCES products(id),
 actor_user_id uuid REFERENCES app_users(id), actor_username varchar(150), action_code varchar(100) NOT NULL,
 resource_type varchar(80) NOT NULL, resource_id uuid, before_data jsonb, after_data jsonb,
 correlation_id varchar(100), ip_address inet, user_agent text, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_resource_time ON audit_logs(resource_type,resource_id,created_at DESC);
CREATE INDEX idx_audit_product_time ON audit_logs(product_id,created_at DESC);

COMMIT;

-- Application/service invariants:
-- A. data_snapshot is canonical; content_field_values is a transactional projection only.
-- B. Shared locale snapshot example: {"vi-VN":{"title":"..."}, "en-US":{"title":"..."}}.
-- C. Validate release item (resource_type, resource_id, version_id, version_no) in service layer.
-- D. Immutable snapshots: edits create a new version; enforce with service policy/DB trigger.
-- E. Workflow authorization checks User + Product + Resource + Action + State on every backend write.
-- F. NEXTJS component versions require deployed runtime_code/runtime_version/deployment_version.
-- G. Asset becomes READY only after S3 object, size, MIME, checksum and malware scan are verified.
