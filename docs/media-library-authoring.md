# Media Library and Content Item Authoring

## Rich text

The `richtext` field is rendered with Tiptap (React integration, StarterKit v3). The editor stores HTML in the existing `cms_content_versions.data_snapshot` JSONB snapshot.

Supported authoring controls:
- Bold, italic, underline, strike-through
- Heading levels 1–3
- Bulleted and numbered lists
- Blockquote and horizontal rule
- Text alignment
- Hyperlinks
- Insert image by URL

The standalone `image` and `image_gallery` field types use the Media Library upload and selection control.

## Media API

- `POST /api/v1/media?productId={uuid}` — multipart upload, field name `file`
- `GET /api/v1/media?productId={uuid}&limit=100` — list Product assets and global assets
- `GET /api/v1/media/{id}/file` — stream file content
- `DELETE /api/v1/media/{id}` — soft-delete metadata

Uploads are limited to 20 MB per file. Allowed MIME types include common raster images, PDF, Office documents, CSV/text, MP4/WebM video, and common audio formats. SVG is intentionally excluded because active SVG content can create script-injection risk.

The backend writes file bytes through the existing `AssetStorageProvider` abstraction:
- Local provider for local development (`./storage/assets`)
- S3 provider for AWS environments

Metadata is persisted in `cms_assets` by Flyway migration V6. The Content Item snapshot stores asset metadata (`id`, `url`, name, MIME type, and size), not binary data.

## Local setup

Flyway applies `V6__media_library.sql` automatically under the local profile. No database reset is required.

The root `application.yml` configures Spring multipart limits:
- max file size: 20 MB
- max request size: 22 MB

## Important deployment notes

1. For UAT/production, configure the S3 bucket, region, prefix, and EKS workload IAM permissions. Do not put AWS access keys in source code.
2. The current download route is authenticated by the application security chain. Frontend previews fetch the file with the same Basic-auth mechanism used by the current local admin UI. Replace this with the approved Entra ID/JWT token provider when the production SSO frontend is enabled.
3. The current API validates MIME type from the multipart request and uses generated object keys; production hardening should add file-signature inspection, malware scanning, quota/rate limiting, and RBAC checks before enabling broad uploads.
4. Rich-text HTML must be sanitized at the public-delivery boundary (and preferably on write) before it is rendered by the public Next.js frontend.
