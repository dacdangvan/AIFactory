# Visual Workflow / Authoring Template Builders and Asset Storage

Branch: `cms-portal-platform`

This increment replaces the JSON text editors in the React Admin Content Management screen with structured form-based configuration.

## Visual Workflow Designer

- Workflow metadata: code, name, description, Global or Product scope.
- States: add, edit, delete, mark initial/terminal and drag to reorder.
- Actions: add, edit and delete.
- Transitions: select From State, Action and To State.
- User Rules: role list and scope.
- Action Permissions: state + action + user rule + ALLOW/DENY.
- Visibility Rules: state + user rule + ALL/OWNED/CREATED_BY_ME/ASSIGNED_TO_ME/NONE.
- Product default and Content Type override binding.

The UI submits a structured request to the existing Workflow Configuration API. JSON remains an internal transport/storage format; administrators do not edit JSON.

Current limitation: state drag-and-drop reorders states; transition edges are configured using selectors. This is not yet a free-form node-canvas connector editor. The backend currently persists Workflow Version 1 and does not yet execute workflow transitions or evaluate authorization/visibility rules at content runtime.

## Authoring Template Builder

- Template metadata and Template Version creation.
- Groups with add, rename, remove and reorder.
- Field catalog grouped into Basic, Rich Content, Selection, Date & Time, Media, Reference, Structured, Location, Advanced and System.
- Field properties: code, label, group, type, required.
- Per-field localization toggle. Localized fields declare `vi-VN` and `en-US`; non-localized fields are shared.
- Cardinality: SINGLE, MULTIPLE and REPEATER.
- Repeater item field references and min/max item settings.
- Standard validation metadata: min/max length, min/max value, regex and uniqueness.
- Declarative custom validation rule editor for cross-field conditions.
- Type-specific option metadata, including select/radio/checkbox options and placeholder/default value.
- Slug source fields, ordering, separator, lowercase, transliteration and uniqueness metadata.
- Content Type → Template Version binding.

The schema is stored as a versioned JSONB snapshot in `cms_authoring_template_versions.field_schema`. The admin user does not need to edit JSON.

Current limitation: this increment builds and stores the template definition. The Content Item dynamic authoring screen that renders two locale inputs, multi-asset pickers, repeater item forms, and executes validation rules is still a separate runtime task. Slug generation and published route/301 activation are also not implemented yet. Repeater item fields are references to sibling fields in the same Group; a fully recursive nested Group builder remains future work.

## Asset Storage

### Provider abstraction

- `AssetStorageProvider` defines upload, download, delete and exists.
- `LocalAssetStorageProvider` stores files below the configured local root and rejects path traversal.
- `S3AssetStorageProvider` uses AWS SDK for Java v2, server-side AES256 encryption and the AWS default credential chain.
- `GET /api/v1/system/asset-storage` returns the active provider and safe, non-secret settings.
- Storage configuration is environment/profile based and read-only in the Admin UI. Provider switching is not performed from Production UI.

### Local profile

`application-local.yml` configures:

- Provider: `local`
- Root: `./storage/assets`
- URL prefix: `/api/v1/media`

The repository ignores `**/storage/assets/`.

### AWS/EKS profile

Activate Spring profile `aws` and configure:

- `CMS_ASSET_S3_BUCKET`
- `AWS_REGION` (defaults to `ap-southeast-1`)
- `CMS_ASSET_S3_PREFIX` (defaults to `cms/assets`)
- Optional `CMS_ASSET_CDN_URL`

Use EKS Pod Identity or IRSA. Do not place AWS access keys in application YAML or the CMS database.

Current limitation: the provider abstraction and configuration endpoint are implemented; Media Library upload/download APIs, asset metadata persistence, virus scanning, signed URL delivery, reference tracking and Local-to-S3 migration utility are not yet implemented. Do not treat this increment as a complete Media Library.

## Local verification

From the repository root:

```bash
cd backend
CMS_DB_PASSWORD=local_dev_password ./mvnw clean test
CMS_DB_PASSWORD=local_dev_password ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

In another terminal:

```bash
cd frontend
npm install
npm run build
npm run dev
```

No local Maven or npm build has been executed by the authoring assistant. Run these commands in the developer environment and report any compiler/test errors before proceeding to Content Item runtime.

## Configuration list and lifecycle UI (follow-up)

The Content Types, Authoring Templates and Workflows tabs now open on management lists rather than opening the builder immediately.

- Content Types: create/edit metadata, select an Authoring Template version, select an optional Content Type Workflow override (or inherit Product default), activate/deactivate, archive.
- Authoring Templates: list, create, edit metadata, activate/deactivate, archive. Template schema versions remain immutable.
- Workflows: list, create, edit metadata, activate/deactivate, archive. Workflow definition versions remain immutable.
- Backend lifecycle endpoints use ACTIVE / INACTIVE / ARCHIVED. Delete is a soft archive and does not physically delete referenced configuration.
- Content Type list includes its current Template Version and Workflow Override identifiers for display.

Important remaining limitation: editing an existing Template's field schema as a new version and editing an existing Workflow's states/actions/transitions as a new version are not implemented in this follow-up. Existing template/workflow metadata can be edited; their versioned definitions are not overwritten. Product default Workflow binding is still managed from the Workflow screen. Content Type form supports a per-type override or inheritance of Product default.
