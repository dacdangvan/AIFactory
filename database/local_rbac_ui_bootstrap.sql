-- LOCAL DEVELOPMENT ONLY. Never run this script in UAT/production.
-- Run after Flyway V2 seed has created the four system roles.
BEGIN;

INSERT INTO products(code,name,description,status,created_by,updated_by)
VALUES ('CMS','VIB CMS Portal','Local product for testing Phase 1.2 RBAC','ACTIVE','local-dev','local-dev')
ON CONFLICT(code) DO NOTHING;

INSERT INTO app_users(username,email,display_name,identity_source,external_subject,status)
VALUES ('local-dev','local-dev@localhost','Local Developer','LOCAL_DEV','local-dev','ACTIVE')
ON CONFLICT(username) DO UPDATE SET
 email=excluded.email,display_name=excluded.display_name,status='ACTIVE',updated_at=now();

INSERT INTO user_product_roles(user_id,product_id,role_id,assigned_by)
SELECT u.id,p.id,r.id,'LOCAL_BOOTSTRAP'
FROM app_users u CROSS JOIN products p CROSS JOIN roles r
WHERE u.username='local-dev' AND p.code='CMS' AND r.code='SYSTEM_ADMIN'
ON CONFLICT(user_id,product_id,role_id) DO NOTHING;

COMMIT;
