# Phase 1.2 – User Management & RBAC

## Features
- Verified Microsoft Entra ID JWT identity mapping. POST /api/v1/identity/sync derives subject from the validated token, not the request body.
- User administration, role catalog, permission catalog and role-permission binding.
- Product-scoped role assignment with optional expiration.
- Permission check endpoint and audit trail.
- Seed roles: SYSTEM_ADMIN, DEV_ADMIN, PO_ADMIN, BU.

## First administrator bootstrap
After the first SSO user is synchronized, a DBA must grant the first SYSTEM_ADMIN. There is deliberately no unauthenticated bootstrap endpoint.

    INSERT INTO user_product_roles(user_id,product_id,role_id,assigned_by)
    SELECT u.id,p.id,r.id,'DBA_BOOTSTRAP'
    FROM app_users u CROSS JOIN products p CROSS JOIN roles r
    WHERE u.username='admin@vib.com.vn' AND p.code='CMS' AND r.code='SYSTEM_ADMIN'
    ON CONFLICT DO NOTHING;

Change username and product code to actual values.

## APIs
- POST /api/v1/identity/sync
- GET /api/v1/users?keyword=&status=&page=0&size=20
- GET /api/v1/users/{id}; PUT /api/v1/users/{id}
- GET /api/v1/roles
- GET /api/v1/roles/permissions; POST /api/v1/roles/permissions
- POST /api/v1/roles/{roleId}/permissions
- POST /api/v1/assignments; GET /api/v1/assignments/users/{id}; DELETE /api/v1/assignments?userId=&productId=&roleId=
- GET /api/v1/authorization/check?productId=&resource=&action=&state=

All management endpoints require an active SYSTEM_ADMIN assignment. Permission data is configurable; final DEV_ADMIN, PO_ADMIN and BU business permissions should be confirmed before granting.

## Run
    cd backend
    CMS_DB_PASSWORD=local_dev_password ./mvnw clean test
    CMS_DB_PASSWORD=local_dev_password ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

## Deployment
Configure the OAuth2 Resource Server issuer/JWK for the VIB Entra tenant. Keep all non-local endpoints authenticated. Ensure JWT Authentication.getName() matches app_users.username. Existing RBAC tables in the baseline DDL are reused; no duplicate table DDL is added.
