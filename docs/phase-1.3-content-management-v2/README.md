# Content Management V2 – implementation status

Branch: `cms-portal-platform`

This redesign replaces the Article-specific authoring model with Product → hierarchical Content Type → Authoring Template Version. Legacy `cms_articles` tables and APIs remain temporarily for backward compatibility and are not the target model.

## Implemented in this increment

- Flyway V4 schema for hierarchical Content Types.
- Product-scoped Authoring Templates and immutable Template Versions.
- Field schema stored as JSONB (groups, fields, validation and localization metadata).
- URL Slug Segments owned by a Template Version, with ordered source-field mappings.
- Content Type → Template Version binding.
- Global/Product Workflow definitions and immutable Workflow Versions.
- Workflow State, Action, Transition, User Rule, Action Permission and Visibility Rule schema.
- Product default Workflow and per-Content-Type override binding schema.
- Admin APIs to create/list Content Types and Authoring Templates, create Template Versions, bind a Template Version, create/list Workflow definitions, and bind a Product default or Content Type override Workflow.
- React Admin screen now exposes Content Types, Authoring Templates and Workflow Designer configuration.

## APIs

### Content Types
- `GET /api/v1/content-types?productId={uuid}`
- `POST /api/v1/content-types?productId={uuid}`

Example:
```json
{ "code":"NEWS", "name":"Tin tức", "parentId":null, "sortOrder":10 }
```

### Authoring Templates
- `GET /api/v1/authoring-templates?productId={uuid}`
- `POST /api/v1/authoring-templates?productId={uuid}`
- `POST /api/v1/authoring-templates/{templateId}/versions`
- `GET /api/v1/authoring-templates/{templateId}/versions`
- `POST /api/v1/content-types/{contentTypeId}/template-bindings`

Template version payload:
```json
{
  "fieldSchema": {
    "groups": [{"code":"main","name":"Thông tin chính"}],
    "fields": [
      {"code":"title","label":"Tiêu đề","type":"text","group":"main","required":true,"localized":true}
    ]
  },
  "urlSegments": [
    {"order":1,"code":"project","separator":"-","fields":["project_name"]},
    {"order":2,"code":"item","separator":"-","fields":["product_name","loan_type"]}
  ]
}
```

### Workflow Designer
- `GET /api/v1/workflows?productId={uuid}`
- `POST /api/v1/workflows`
- `POST /api/v1/products/{productId}/workflow-bindings`

Workflow definition accepts arrays: `states`, `actions`, `userRules`, `transitions`, `permissions`, `visibilityRules`. A permission row maps `state + action + userRule + effect`; a visibility row maps `state + userRule + scope` where scope is `ALL`, `OWNED`, `CREATED_BY_ME`, `ASSIGNED_TO_ME`, or `NONE`.

Workflow binding payload:
```json
{ "bindingType":"PRODUCT_DEFAULT", "workflowVersionId":"<uuid>" }
```
or
```json
{ "bindingType":"CONTENT_TYPE_OVERRIDE", "contentTypeId":"<uuid>", "workflowVersionId":"<uuid>" }
```

## Not yet complete

This increment is the configuration foundation, not a production-complete CMS runtime. The following still need implementation and integration before calling the module complete:

1. Content Item CRUD with dynamic form rendering from the bound Template Version.
2. Server-side runtime evaluation of User Rules, Action Permissions and Visibility Rules.
3. Workflow action execution, optimistic locking, audit/history and separation-of-duties checks.
4. Locale-aware slug generation from the Template Version, published route activation, collision checks, route history and 301 delivery.
5. Publish/release manifest integration and public/preview delivery APIs.
6. React screens for Content Item authoring, permission matrix editing, visibility configuration, workflow binding and route preview.
7. Automated integration tests for migration, APIs, authorization and URL routing.

Do not expose this increment to UAT/production until these runtime and authorization items are completed and tested.
