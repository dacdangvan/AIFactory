-- Identity and RBAC tables are part of database/01_postgresql_ddl.sql baseline.
INSERT INTO roles(code,name,description,is_system) VALUES
('SYSTEM_ADMIN','System Administrator','Platform-wide identity and access administration',true),
('DEV_ADMIN','Developer Administrator','Technical configuration and developer-managed resources',true),
('PO_ADMIN','Product Owner Administrator','Product-level content and release administration',true),
('BU','Business User','Business content operations within assigned product and workflow state',true)
ON CONFLICT(code) DO NOTHING;
INSERT INTO permissions(resource_type,action_code,state_code,scope_type,effect,description)
VALUES('ADMINISTRATION','MANAGE',NULL,'GLOBAL','ALLOW','Manage users, roles, permissions and assignments')
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code='SYSTEM_ADMIN'
AND p.resource_type='ADMINISTRATION' AND p.action_code='MANAGE' AND p.scope_type='GLOBAL' AND p.effect='ALLOW'
ON CONFLICT DO NOTHING;