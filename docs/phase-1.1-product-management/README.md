# Phase 1.1 – Product Management

## Scope

This phase completes the Product Management API using the existing PostgreSQL `products` table and `JdbcTemplate`.

- Paginated product listing.
- Keyword search by product code or name.
- Status filter: `ACTIVE`, `INACTIVE`, `ARCHIVED`.
- Product detail, create and update.
- Explicit status transition endpoint.
- Soft archive through DELETE.
- Audit fields: `created_at`, `created_by`, `updated_at`, `updated_by`.
- Unit tests for service validation and lifecycle rules.

## API

All endpoints are under `/api/v1/products`.

| Method | Path | Description |
|---|---|---|
| GET | `/` | List with pagination and filters |
| GET | `/{id}` | Product detail |
| POST | `/` | Create product (initial status ACTIVE) |
| PUT | `/{id}` | Update code, name, description |
| PATCH | `/{id}/status` | Change ACTIVE ↔ INACTIVE |
| DELETE | `/{id}` | Soft archive; repeated archive is idempotent |

### List query parameters

- `page`: zero-based, default `0`.
- `size`: default `20`, allowed range `1..100`.
- `keyword`: optional substring search against code and name.
- `status`: optional status filter.

Example:

```http
GET /api/v1/products?page=0&size=20&keyword=website&status=ACTIVE
```

The response data contains `content`, `page`, `size`, `totalElements`, and `totalPages`.

### Create/update request

```json
{
  "code": "PWS",
  "name": "Public Website",
  "description": "VIB public website"
}
```

The `status` field is intentionally not accepted in create/update. New products start as ACTIVE. Status changes use the dedicated PATCH endpoint.

### Change status

```http
PATCH /api/v1/products/{id}/status
Content-Type: application/json

{"status":"INACTIVE"}
```

Only ACTIVE and INACTIVE are accepted by PATCH. ARCHIVED is terminal and can only be reached through DELETE.

## Business rules

1. Product code is unique and limited to letters, numbers, dot, underscore and hyphen.
2. Code length is at most 100; name length is at most 255.
3. Page is zero-based; page size cannot exceed 100.
4. ACTIVE and INACTIVE can transition to each other.
5. ARCHIVED products cannot be edited or reactivated.
6. DELETE performs a soft archive; it never physically deletes a product.
7. Repeating DELETE for an existing archived product succeeds idempotently.
8. Audit actor uses the authenticated principal name; local unauthenticated development falls back to `local-dev`.

## Database and architecture

- No DDL change is required.
- No table is recreated.
- `JdbcTemplate` remains the data access mechanism for Product.
- Flyway and JPA configuration remain unchanged.
- This phase does not implement the final product-scoped RBAC policy. Existing security configuration remains responsible for authentication/route access.

## Run and test

Start local dependencies (from repository root):

```bash
docker compose up -d
```

Run tests:

```bash
cd backend
CMS_DB_PASSWORD=local_dev_password ./mvnw clean test
```

Run the backend:

```bash
CMS_DB_PASSWORD=local_dev_password ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Ensure the local PostgreSQL schema has already been initialized using the project's existing Flyway/DDL setup before running the Spring context integration test.

## Compatibility note

The previous create/update request included a `status` field. Phase 1.1 moves status changes to `PATCH /{id}/status` so product lifecycle transitions are explicit and auditable. Update any client payload that previously sent `status` in POST/PUT.
