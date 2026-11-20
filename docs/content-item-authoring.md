# Content Item Authoring

## Flow

1. Select a Product.
2. Select an active Content Type belonging to the Product.
3. The authoring screen loads the Content Type's default Authoring Template Version.
4. The backend resolves the workflow: Content Type override first, otherwise Product default.
5. Create a Content Item with a unique Product-scoped Content Code.
6. The backend validates the submitted fields against the immutable template schema, stores a JSONB snapshot as version 1, and creates a workflow instance at the workflow's initial state.
7. Editing creates a new immutable content version. The item keeps the exact template version captured at creation even if the Content Type later binds a newer template version.

## APIs

- `GET /api/v1/content-items?productId={uuid}&contentTypeId={uuid}&keyword={text}&limit=100`
- `GET /api/v1/content-items/authoring-definition?contentTypeId={uuid}`
- `GET /api/v1/content-items/{id}`
- `GET /api/v1/content-items/{id}/authoring-definition` (returns the exact Template Version pinned to the item)
- `POST /api/v1/content-items?productId={uuid}&contentTypeId={uuid}`
- `PUT /api/v1/content-items/{id}`
- `DELETE /api/v1/content-items/{id}` (soft archive)

Create/update payload:

```json
{
  "code": "HOME_LOAN_OVERVIEW",
  "fields": {
    "title": {
      "vi-VN": "Vay mua nhà",
      "en-US": "Home loan"
    },
    "summary": "Product summary",
    "gallery": ["https://assets.example/image-1.jpg"],
    "documents": [
      {"documentName": "Brochure", "file": "https://assets.example/brochure.pdf"}
    ]
  },
  "expectedVersion": 1
}
```

The `expectedVersion` is required for safe updates in practice; the current API defaults it to the latest version when omitted. The backend uses compare-and-set on `current_version` and rejects stale updates.

## Supported authoring controls

- Text, textarea, rich text/HTML/Markdown/code (currently rendered as textareas).
- Number, email, phone, URL, date, time and datetime.
- Select, radio, checkbox options, multiselect and toggle.
- Localized fields with separate `vi-VN` and `en-US` inputs.
- Multiple-value fields.
- Repeater rows with selected child fields, including localized child fields.
- Media reference fields currently accept an Asset URL or Asset ID.

## Current scope and limitations

- Content data is stored in `cms_content_versions.data_snapshot`; old snapshots are immutable.
- Workflow instance is initialized from the configured initial state.
- Content can currently be edited in its initial workflow state or REJECTED state.
- This increment does not yet implement workflow action execution (submit, recall, approve, reject, publish), runtime evaluation of workflow action permissions/visibility rules, full RBAC enforcement for each Content Item action, or publish/release routing.
- Media fields accept a URL/Asset ID. Browser upload, asset picker, scanning and asset-reference management require the Media Library implementation.
- Field-level min/max and regex validation are applied where configured. Cross-field custom rules and all type-specific validation are not yet evaluated server-side.
- Content item routes, slug collision handling and 301 redirects are not activated by this increment.


## SEO metadata and URL preview

- Each Content Item version snapshot now stores a sibling `seo` object alongside `fields`; the SEO values are immutable with that content version.
- SEO values are localized for `vi-VN` and `en-US`: title, meta description, keywords, URL slug, Open Graph title, Open Graph description and Open Graph image URL.
- The editor derives a suggested slug from the first localized field whose code or label resembles title/name/heading. An explicitly entered slug overrides the suggestion.
- The UI displays a URL preview using the locale, Product code/name slug and Content Item slug.
- The preview is advisory only. This increment does not activate public routes, check slug collisions, retain route history, create 301 redirects, update release manifests or invalidate CDN/cache. Those remain part of Publishing Runtime.
- SEO metadata is sent in the same create/update request as content fields and persisted in the same immutable `cms_content_versions.data_snapshot`. Existing items without SEO metadata load with empty localized SEO values.
