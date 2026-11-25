-- Page composition, theme configuration and route history foundation.
CREATE TABLE cms_themes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id),
    code varchar(120) NOT NULL,
    name varchar(200) NOT NULL,
    description text,
    head_html text,
    body_start_html text,
    body_end_html text,
    custom_css text,
    custom_js text,
    status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','ACTIVE','INACTIVE','ARCHIVED')),
    created_by varchar(255) NOT NULL,
    updated_by varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(product_id, code)
);

CREATE TABLE cms_pages (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id),
    parent_id uuid REFERENCES cms_pages(id),
    theme_id uuid REFERENCES cms_themes(id),
    page_type varchar(20) NOT NULL DEFAULT 'NORMAL' CHECK(page_type IN ('NORMAL','TAG')),
    code varchar(150) NOT NULL,
    title varchar(300) NOT NULL,
    locale varchar(20) NOT NULL DEFAULT 'vi-VN',
    slug varchar(500) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'DRAFT'
        CHECK(status IN ('DRAFT','IN_REVIEW','APPROVED','PUBLISHED','UNPUBLISHED','ARCHIVED')),
    sort_order integer NOT NULL DEFAULT 0,
    seo jsonb NOT NULL DEFAULT '{}'::jsonb CHECK(jsonb_typeof(seo)='object'),
    settings jsonb NOT NULL DEFAULT '{}'::jsonb CHECK(jsonb_typeof(settings)='object'),
    created_by varchar(255) NOT NULL,
    updated_by varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(product_id, code)
);
CREATE INDEX idx_cms_pages_tree ON cms_pages(product_id,parent_id,sort_order);
CREATE UNIQUE INDEX uq_cms_pages_sibling_slug ON cms_pages(product_id,locale,COALESCE(parent_id,'00000000-0000-0000-0000-000000000000'::uuid),lower(slug))
    WHERE status <> 'ARCHIVED';

CREATE TABLE cms_page_sections (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id uuid NOT NULL REFERENCES cms_pages(id) ON DELETE CASCADE,
    section_key varchar(150) NOT NULL,
    section_name varchar(200) NOT NULL,
    component_key varchar(200) NOT NULL,
    content_item_id uuid REFERENCES cms_content_items(id),
    sort_order integer NOT NULL DEFAULT 0,
    config jsonb NOT NULL DEFAULT '{}'::jsonb CHECK(jsonb_typeof(config)='object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(page_id,section_key)
);
CREATE INDEX idx_cms_page_sections_order ON cms_page_sections(page_id,sort_order);

CREATE TABLE cms_page_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id uuid NOT NULL REFERENCES cms_pages(id),
    version_no integer NOT NULL CHECK(version_no>0),
    snapshot jsonb NOT NULL CHECK(jsonb_typeof(snapshot)='object'),
    created_by varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(page_id,version_no)
);

CREATE TABLE cms_page_route_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id uuid NOT NULL REFERENCES cms_pages(id),
    old_route_path varchar(1500) NOT NULL UNIQUE,
    new_route_path varchar(1500) NOT NULL,
    redirect_status integer NOT NULL DEFAULT 301 CHECK(redirect_status IN (301,302,307,308)),
    created_by varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
