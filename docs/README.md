
# VIB CMS Portal – Technical Design

## 1. Overview

Headless CMS platform for VIB Website.

Architecture:
- CMS Admin UI: ReactJS
- CMS Backend: Java Spring Boot
- Database: PostgreSQL
- Cache: Redis (CMS Backend only)
- Asset Storage: Amazon S3
- Public Website: Next.js on AWS EKS
- Ingress: AWS Application Load Balancer

## 2. Design Decisions

| Area | Decision |
|---|---|
| Content Field Value | Hybrid |
| Versioning | Snapshot |
| Publishing | Release |
| Localization | Shared |

### Hybrid Content Storage
- `content_versions.data_snapshot` is canonical JSONB.
- `content_field_values` is a derived projection.
- Both are updated in the same database transaction.
- Projection can be rebuilt from immutable snapshots.

### Snapshot Versioning
- Every edit creates a new immutable version.
- Existing snapshots must never be overwritten.
- Working version and published version are tracked separately.

### Release Publishing
- A release manifest pins exact resource version IDs.
- A release is immutable after creation.
- Publishing activates a validated release.
- Rollback activates a previous valid release.

### Shared Localization
- One version contains all locale data.
- Example: `vi-VN`, `en-US`.
- Locales share the same approval and publication lifecycle.

## 3. Repository Structure

```text
vib-cms/
├── database/
│   └── 01_postgresql_ddl.sql
├── docs/
│   ├── 02_physical_erd.md
│   └── README.md
├── openapi/
│   └── cms-portal-openapi.yaml
└── migrations/
    └── README.md
```

## 4. Database Setup

1. Provision PostgreSQL.
2. Create a dedicated CMS database.
3. Create a non-owner application role.
4. Apply DDL in a controlled migration.
5. Verify constraints and indexes.
6. Configure backup and point-in-time recovery.

Do not run the baseline DDL directly against production
without schema review and migration testing.

## 5. Backend Modules

Recommended modular monolith:

- Product Management
- Identity & Access
- Theme Management
- Page Management
- Layout Management
- Template Management
- Content Management
- Component Registry
- Asset Management
- Workflow Engine
- Publishing Service
- Delivery Service
- Audit Service

## 6. API Security

- JWT / SSO authentication.
- Product-scoped RBAC.
- Server-side resource authorization.
- Preview APIs require authentication.
- Public Delivery APIs expose published data only.
- Presigned S3 URLs must be short-lived.
- Never expose database or Redis credentials to Next.js.

## 7. Next.js Integration

Browser -> ALB -> Next.js -> CMS Delivery API

Next.js must:
- Resolve page by product, path and locale.
- Consume published release data only.
- Render registered runtime components.
- Respect cache-control and release version.
- Never execute arbitrary React/JavaScript received from CMS.

## 8. Publishing Lifecycle

Draft
  -> Submit Approval
  -> Approved
  -> Release Validation
  -> Release Created
  -> Publish
  -> Cache Invalidation

Rollback must reference a previous immutable release.

## 9. Operational Requirements

- Structured logs and trace IDs.
- Audit trail for privileged actions.
- Database backup and restore tests.
- Redis availability monitoring.
- S3 lifecycle and versioning policies.
- CI/CD security scanning.
- API contract testing.
- Release integrity checks.
