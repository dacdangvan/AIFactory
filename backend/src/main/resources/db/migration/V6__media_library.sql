CREATE TABLE cms_assets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid REFERENCES products(id),
    original_filename varchar(500) NOT NULL,
    object_key varchar(700) NOT NULL UNIQUE,
    content_type varchar(200) NOT NULL,
    file_size bigint NOT NULL CHECK (file_size > 0),
    created_by varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE INDEX idx_cms_assets_product_created ON cms_assets(product_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_cms_assets_type ON cms_assets(content_type) WHERE deleted_at IS NULL;
