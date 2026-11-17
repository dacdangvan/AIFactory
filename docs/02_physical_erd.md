# VIB CMS Portal – Physical ERD v1.0

## Decisions
- Hybrid content values: `content_versions.data_snapshot` is canonical JSONB; `content_field_values` is a derived query projection.
- Snapshot versioning: every version stores a complete immutable snapshot.
- Release publishing: `publish_releases` pins exact resource versions through `publish_release_items`.
- Shared localization: one version contains locale-keyed objects such as `vi-VN` and `en-US`.

## Product and access control
```mermaid
erDiagram
 PRODUCTS ||--o{ THEMES : owns
 PRODUCTS ||--o{ TEMPLATES : owns
 PRODUCTS ||--o{ CONTENTS : scopes
 PRODUCTS ||--o{ COMPONENTS : scopes
 PRODUCTS ||--o{ WORKFLOWS : scopes
 APP_USERS ||--o{ USER_PRODUCT_ROLES : assigned
 PRODUCTS ||--o{ USER_PRODUCT_ROLES : scopes
 ROLES ||--o{ USER_PRODUCT_ROLES : grants
 ROLES ||--o{ ROLE_PERMISSIONS : contains
 PERMISSIONS ||--o{ ROLE_PERMISSIONS : granted
 PRODUCTS { uuid id PK; varchar code UK }
 APP_USERS { uuid id PK; varchar username UK }
 ROLES { uuid id PK; varchar code UK }
 PERMISSIONS { uuid id PK; varchar resource_type; varchar action_code; varchar state_code }
 USER_PRODUCT_ROLES { uuid user_id FK; uuid product_id FK; uuid role_id FK }
 ROLE_PERMISSIONS { uuid role_id FK; uuid permission_id FK }
```

## Theme, page, layout and sections
```mermaid
erDiagram
 PRODUCTS ||--o{ THEMES : owns
 THEMES ||--o{ PAGES : contains
 PAGES ||--o{ PAGES : parent_child
 PAGES ||--o{ PAGE_VERSIONS : versions
 LAYOUTS ||--o{ LAYOUT_VERSIONS : versions
 LAYOUT_VERSIONS ||--o{ PAGE_VERSIONS : selected_by
 PAGE_VERSIONS ||--o{ PAGE_SECTIONS : contains
 COMPONENT_VERSIONS ||--o{ PAGE_SECTIONS : renders
 CONTENT_VERSIONS ||--o{ PAGE_SECTIONS : supplies
 THEMES { uuid id PK; uuid product_id FK; varchar code; varchar root_base; jsonb head_config; jsonb body_config }
 PAGES { uuid id PK; uuid theme_id FK; uuid parent_id FK; varchar code; varchar slug; varchar page_type }
 PAGE_VERSIONS { uuid id PK; uuid page_id FK; int version_no; uuid layout_version_id FK; jsonb seo_snapshot }
 PAGE_SECTIONS { uuid id PK; uuid page_version_id FK; uuid component_version_id FK; uuid content_version_id FK; int sort_order }
 LAYOUTS { uuid id PK; uuid product_id FK; varchar code }
 LAYOUT_VERSIONS { uuid id PK; uuid layout_id FK; int version_no; jsonb definition }
```

## Template and content
```mermaid
erDiagram
 TEMPLATES ||--o{ TEMPLATE_VERSIONS : versions
 TEMPLATE_VERSIONS ||--o{ FIELD_GROUPS : groups
 FIELD_GROUPS ||--o{ FIELD_DEFINITIONS : defines
 TEMPLATES ||--o{ CONTENTS : structures
 CONTENTS ||--o{ CONTENT_VERSIONS : versions
 TEMPLATE_VERSIONS ||--o{ CONTENT_VERSIONS : validates
 CONTENT_VERSIONS ||--o{ CONTENT_FIELD_VALUES : projects
 TEMPLATES { uuid id PK; uuid product_id FK; varchar code }
 TEMPLATE_VERSIONS { uuid id PK; uuid template_id FK; int version_no; jsonb schema_snapshot }
 FIELD_GROUPS { uuid id PK; uuid template_version_id FK; varchar code }
 FIELD_DEFINITIONS { uuid id PK; uuid field_group_id FK; varchar field_key; varchar field_type }
 CONTENTS { uuid id PK; uuid product_id FK; uuid template_id FK; varchar code }
 CONTENT_VERSIONS { uuid id PK; uuid content_id FK; int version_no; uuid template_version_id FK; jsonb data_snapshot }
 CONTENT_FIELD_VALUES { uuid id PK; uuid content_version_id FK; uuid field_definition_id FK; varchar locale; jsonb value }
```

## Component and assets
```mermaid
erDiagram
 PRODUCTS ||--o{ COMPONENTS : owns
 COMPONENTS ||--o{ COMPONENT_VERSIONS : versions
 PRODUCTS ||--o{ ASSET_FOLDERS : owns
 ASSET_FOLDERS ||--o{ ASSETS : contains
 ASSETS ||--o{ ASSET_VERSIONS : versions
 ASSETS ||--o{ ASSET_REFERENCES : referenced_by
 COMPONENTS { uuid id PK; uuid product_id FK; varchar code; varchar component_type }
 COMPONENT_VERSIONS { uuid id PK; uuid component_id FK; int version_no; varchar runtime_code; varchar deployment_version; jsonb settings_schema }
 ASSET_FOLDERS { uuid id PK; uuid product_id FK; uuid parent_id FK; varchar name }
 ASSETS { uuid id PK; uuid product_id FK; uuid folder_id FK; varchar file_name; varchar status }
 ASSET_VERSIONS { uuid id PK; uuid asset_id FK; int version_no; varchar bucket_name; varchar object_key; varchar scan_status }
 ASSET_REFERENCES { uuid id PK; uuid asset_id FK; varchar owner_type; uuid owner_id }
```

## Workflow and publishing
```mermaid
erDiagram
 WORKFLOWS ||--o{ WORKFLOW_VERSIONS : versions
 WORKFLOW_VERSIONS ||--o{ WORKFLOW_STATES : contains
 WORKFLOW_VERSIONS ||--o{ WORKFLOW_TRANSITIONS : defines
 WORKFLOW_INSTANCES ||--o{ WORKFLOW_HISTORIES : records
 PRODUCTS ||--o{ PUBLISH_RELEASES : scopes
 PUBLISH_RELEASES ||--o{ PUBLISH_RELEASE_ITEMS : contains
 PUBLISH_RELEASES ||--o{ PUBLISH_EVENTS : emits
 PUBLISH_RELEASES ||--o{ PUBLISH_HISTORY : records
 WORKFLOWS { uuid id PK; uuid product_id FK; varchar code; varchar resource_type }
 WORKFLOW_VERSIONS { uuid id PK; uuid workflow_id FK; int version_no }
 WORKFLOW_STATES { uuid id PK; uuid workflow_version_id FK; varchar state_code }
 WORKFLOW_TRANSITIONS { uuid id PK; uuid workflow_version_id FK; uuid from_state_id FK; uuid to_state_id FK }
 WORKFLOW_INSTANCES { uuid id PK; uuid workflow_version_id FK; uuid product_id FK; uuid resource_id }
 WORKFLOW_HISTORIES { uuid id PK; uuid workflow_instance_id FK; varchar action_code; uuid actor_user_id FK }
 PUBLISH_RELEASES { uuid id PK; uuid product_id FK; varchar release_code; varchar status; jsonb manifest }
 PUBLISH_RELEASE_ITEMS { uuid id PK; uuid release_id FK; varchar resource_type; uuid resource_id; uuid version_id }
 OUTBOX_EVENTS { uuid id PK; uuid aggregate_id; varchar event_type; jsonb payload; varchar status }
 AUDIT_LOGS { uuid id PK; uuid product_id FK; uuid actor_user_id FK; varchar resource_type; uuid resource_id }
```

## Important physical rules
1. Locale keys live inside the version snapshot. For example:
   `{"vi-VN":{"title":"Vay mua nhà"},"en-US":{"title":"Home loan"}}`.
2. `content_field_values` is a denormalized projection for search/filter/reporting; update it in the same transaction as the canonical snapshot.
3. `publish_release_items.version_id` is polymorphic. Validate `(resource_type, resource_id, version_id, version_no)` in the service. If strict FK enforcement is required, use typed child tables per resource type.
4. `asset_references` and `workflow_instances` also use polymorphic owner/resource identifiers and require service-level validation.
5. Approved/published snapshots and release manifests are immutable. Create new versions for edits.
