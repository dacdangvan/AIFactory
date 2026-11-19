CREATE TABLE cms_articles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id),
    content_type varchar(40) NOT NULL CHECK (content_type IN ('NEWS','INVESTOR_RELATIONS','PROMOTION','GUIDE')),
    code varchar(150) NOT NULL,
    slug varchar(300) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','IN_REVIEW','APPROVED','PUBLISHED','ARCHIVED')),
    current_version integer NOT NULL DEFAULT 1 CHECK (current_version > 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by varchar(255) NOT NULL,
    UNIQUE(product_id, code),
    UNIQUE(product_id, slug)
);

CREATE TABLE cms_article_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id uuid NOT NULL REFERENCES cms_articles(id) ON DELETE CASCADE,
    version_no integer NOT NULL CHECK (version_no > 0),
    locale_data jsonb NOT NULL,
    seo_data jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL,
    UNIQUE(article_id, version_no),
    CHECK (jsonb_typeof(locale_data) = 'object'),
    CHECK (jsonb_typeof(seo_data) = 'object')
);

CREATE INDEX idx_cms_articles_product_status ON cms_articles(product_id, status, updated_at DESC);
CREATE INDEX idx_cms_articles_type ON cms_articles(product_id, content_type);
CREATE INDEX idx_cms_article_versions_latest ON cms_article_versions(article_id, version_no DESC);