-- Identity and RBAC tables are part of database/01_postgresql_ddl.sql baseline.
INSERT INTO roles(code,name,description,is_system) VALUES
('SYSTEM_ADMIN','System Administrator','Platform-wide identity and access administration',true),
('DEV_ADMIN','Developer Administrator','Technical configuration and developer-managed resources',true),
('PO_ADMIN','Product Owner Administrator','Product-level content and release administration',true),
('BU','Business User','Business content operations within assigned product and workflow state',true)
ON CONFLICT(code) DO NOTHING;

-- state_code is nullable and the baseline UNIQUE constraint treats NULL values as distinct.
-- NOT EXISTS keeps this seed idempotent for the GLOBAL permission.
INSERT INTO permissions(resource_type,action_code,state_code,scope_type,effect,description)
SELECT 'ADMINISTRATION','MANAGE',NULL,'GLOBAL','ALLOW','Manage users, roles, permissions and assignments'
WHERE NOT EXISTS (
  SELECT 1 FROM permissions
  WHERE resource_type='ADMINISTRATION' AND action_code='MANAGE'
    AND state_code IS NULL AND scope_type='GLOBAL' AND effect='ALLOW'
);

INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.code='SYSTEM_ADMIN' AND p.resource_type='ADMINISTRATION'
  AND p.action_code='MANAGE' AND p.state_code IS NULL
  AND p.scope_type='GLOBAL' AND p.effect='ALLOW'
ON CONFLICT DO NOTHING;
