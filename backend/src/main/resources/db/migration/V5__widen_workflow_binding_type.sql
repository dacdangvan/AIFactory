-- CONTENT_TYPE_OVERRIDE is 21 characters; widen the column without rewriting V4.
ALTER TABLE cms_workflow_bindings
    ALTER COLUMN binding_type TYPE varchar(30);
