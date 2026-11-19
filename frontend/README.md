# VIB CMS Admin – Phase 1.2

React + TypeScript + Vite console for testing User Management and RBAC APIs.

## Prerequisites
- Backend Phase 1.2 running with the `local` Spring profile on port 8080.
- PostgreSQL schema already migrated and V2 role seed applied.
- Node.js 20+ and npm.

## One-time local bootstrap
From repository root, run the local-only SQL after backend has started once (so Flyway has applied V2):

```bash
docker compose exec -T postgres psql -U cms_local -d vib_cms < database/local_rbac_ui_bootstrap.sql
```

If your PostgreSQL container service is not named `postgres`, use the actual service name from docker-compose.yml, or connect with psql:

```bash
psql "postgresql://cms_local:local_dev_password@localhost:5432/vib_cms" -f database/local_rbac_ui_bootstrap.sql
```

This creates a local-only `admin` identity, a CMS product and grants SYSTEM_ADMIN on that product. The local-only login is `admin` / `admin`; it is not a production login.

## Run the React app
Terminal 1 – backend:

```bash
cd backend
CMS_DB_PASSWORD=local_dev_password ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Terminal 2 – frontend:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

Vite proxies `/api/*` to http://localhost:8080. The login screen uses HTTP Basic credentials `admin` / `admin`. The backend accepts these fixed credentials only when the `local` profile and `cms.security.local-permit-all=true` are active. The filter is not registered outside the local profile. Do not reuse this password in any shared environment.

## Screens
- Users: list, search, activate/deactivate/lock.
- Roles & permissions: inspect roles, create permissions and attach them to roles.
- Assignments: assign/revoke product-scoped roles and optional expiry.
- Authorization playground: invoke backend permission check.
- Products: inspect products available for assignment.

## Microsoft SSO
The local console is a development harness, not an SSO implementation. In UAT/production, use the Microsoft Entra ID login flow and send the verified access token as `Authorization: Bearer <token>`. Do not send `X-Dev-User` outside local development.
