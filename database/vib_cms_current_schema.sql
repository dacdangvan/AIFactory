--
-- PostgreSQL database dump
--

\restrict vhryFcTmACtMGERmHaLknmocFAdb0SB5BN5RoS2oegmW9jfxv5CNdryChuCcfpz

-- Dumped from database version 16.16 (Debian 16.16-1.pgdg13+1)
-- Dumped by pg_dump version 16.16 (Debian 16.16-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: app_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    username character varying(150) NOT NULL,
    email character varying(320),
    display_name character varying(255),
    identity_source character varying(50) DEFAULT 'MICROSOFT_SSO'::character varying NOT NULL,
    external_subject character varying(255),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT app_users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'LOCKED'::character varying])::text[])))
);


--
-- Name: asset_folders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asset_folders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    parent_id uuid,
    name character varying(255) NOT NULL,
    path character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255)
);


--
-- Name: asset_references; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asset_references (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    asset_id uuid NOT NULL,
    owner_type character varying(50) NOT NULL,
    owner_id uuid NOT NULL,
    usage_type character varying(80) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: asset_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asset_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    asset_id uuid NOT NULL,
    version_no integer NOT NULL,
    bucket_name character varying(255) NOT NULL,
    object_key character varying(1000) NOT NULL,
    checksum_sha256 character varying(64),
    file_size bigint NOT NULL,
    mime_type character varying(150) NOT NULL,
    scan_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT asset_versions_file_size_check CHECK ((file_size >= 0)),
    CONSTRAINT asset_versions_scan_status_check CHECK (((scan_status)::text = ANY ((ARRAY['PENDING'::character varying, 'CLEAN'::character varying, 'INFECTED'::character varying, 'ERROR'::character varying])::text[]))),
    CONSTRAINT asset_versions_version_no_check CHECK ((version_no > 0))
);


--
-- Name: assets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    folder_id uuid,
    file_name character varying(500) NOT NULL,
    mime_type character varying(150) NOT NULL,
    file_size bigint NOT NULL,
    checksum_sha256 character varying(64),
    status character varying(30) DEFAULT 'UPLOADING'::character varying NOT NULL,
    current_version_no integer DEFAULT 1 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(255),
    CONSTRAINT assets_file_size_check CHECK ((file_size >= 0)),
    CONSTRAINT assets_status_check CHECK (((status)::text = ANY ((ARRAY['UPLOADING'::character varying, 'SCANNING'::character varying, 'READY'::character varying, 'REJECTED'::character varying, 'DELETED'::character varying])::text[])))
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid,
    actor_user_id uuid,
    actor_username character varying(150),
    action_code character varying(100) NOT NULL,
    resource_type character varying(80) NOT NULL,
    resource_id uuid,
    before_data jsonb,
    after_data jsonb,
    correlation_id character varying(100),
    ip_address inet,
    user_agent text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: component_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.component_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    component_id uuid NOT NULL,
    version_no integer NOT NULL,
    runtime_code character varying(150),
    runtime_version character varying(50),
    deployment_version character varying(150),
    settings_schema jsonb DEFAULT '{}'::jsonb NOT NULL,
    content_schema jsonb DEFAULT '{}'::jsonb NOT NULL,
    default_settings jsonb DEFAULT '{}'::jsonb NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    CONSTRAINT component_versions_content_schema_check CHECK ((jsonb_typeof(content_schema) = 'object'::text)),
    CONSTRAINT component_versions_default_settings_check CHECK ((jsonb_typeof(default_settings) = 'object'::text)),
    CONSTRAINT component_versions_settings_schema_check CHECK ((jsonb_typeof(settings_schema) = 'object'::text)),
    CONSTRAINT component_versions_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'APPROVED'::character varying, 'DEPLOYED'::character varying, 'RETIRED'::character varying])::text[]))),
    CONSTRAINT component_versions_version_no_check CHECK ((version_no > 0))
);


--
-- Name: components; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.components (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    code character varying(150) NOT NULL,
    name character varying(255) NOT NULL,
    component_type character varying(20) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(255),
    CONSTRAINT components_component_type_check CHECK (((component_type)::text = ANY ((ARRAY['HTML'::character varying, 'NEXTJS'::character varying, 'FILE'::character varying])::text[]))),
    CONSTRAINT components_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: content_field_values; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.content_field_values (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    content_version_id uuid NOT NULL,
    field_definition_id uuid NOT NULL,
    locale character varying(20) NOT NULL,
    item_index integer DEFAULT 0 NOT NULL,
    value jsonb,
    search_text text,
    CONSTRAINT content_field_values_item_index_check CHECK ((item_index >= 0))
);


--
-- Name: content_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.content_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    content_id uuid NOT NULL,
    version_no integer NOT NULL,
    template_version_id uuid NOT NULL,
    data_snapshot jsonb NOT NULL,
    locale_metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    workflow_state character varying(50) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    CONSTRAINT content_versions_data_snapshot_check CHECK ((jsonb_typeof(data_snapshot) = 'object'::text)),
    CONSTRAINT content_versions_locale_metadata_check CHECK ((jsonb_typeof(locale_metadata) = 'object'::text)),
    CONSTRAINT content_versions_version_no_check CHECK ((version_no > 0))
);


--
-- Name: contents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    template_id uuid NOT NULL,
    code character varying(150) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(255),
    CONSTRAINT contents_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: field_definitions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.field_definitions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    field_group_id uuid NOT NULL,
    field_key character varying(150) NOT NULL,
    label character varying(255) NOT NULL,
    field_type character varying(30) NOT NULL,
    required boolean DEFAULT false NOT NULL,
    repeatable boolean DEFAULT false NOT NULL,
    localized boolean DEFAULT false NOT NULL,
    show_breadcrumb boolean DEFAULT false NOT NULL,
    validation_rules jsonb DEFAULT '{}'::jsonb NOT NULL,
    display_config jsonb DEFAULT '{}'::jsonb NOT NULL,
    options_config jsonb DEFAULT '{}'::jsonb NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    CONSTRAINT field_definitions_display_config_check CHECK ((jsonb_typeof(display_config) = 'object'::text)),
    CONSTRAINT field_definitions_field_type_check CHECK (((field_type)::text = ANY ((ARRAY['TEXT'::character varying, 'DATE_TIME'::character varying, 'FILE'::character varying, 'HTML'::character varying, 'IMAGE'::character varying, 'LINK'::character varying, 'NUMBER'::character varying, 'OPTION_SELECTION'::character varying, 'RICH_TEXT'::character varying, 'SLUG'::character varying])::text[]))),
    CONSTRAINT field_definitions_options_config_check CHECK ((jsonb_typeof(options_config) = 'object'::text)),
    CONSTRAINT field_definitions_validation_rules_check CHECK ((jsonb_typeof(validation_rules) = 'object'::text))
);


--
-- Name: field_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.field_groups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    template_version_id uuid NOT NULL,
    code character varying(150) NOT NULL,
    name character varying(255) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);


--
-- Name: layout_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.layout_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    layout_id uuid NOT NULL,
    version_no integer NOT NULL,
    definition jsonb DEFAULT '{}'::jsonb NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    CONSTRAINT layout_versions_definition_check CHECK ((jsonb_typeof(definition) = 'object'::text)),
    CONSTRAINT layout_versions_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'APPROVED'::character varying, 'RETIRED'::character varying])::text[]))),
    CONSTRAINT layout_versions_version_no_check CHECK ((version_no > 0))
);


--
-- Name: layouts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.layouts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(255),
    CONSTRAINT layouts_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: outbox_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.outbox_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    aggregate_type character varying(80) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type character varying(100) NOT NULL,
    payload jsonb NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    retry_count integer DEFAULT 0 NOT NULL,
    next_retry_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    published_at timestamp with time zone,
    CONSTRAINT outbox_events_payload_check CHECK ((jsonb_typeof(payload) = 'object'::text)),
    CONSTRAINT outbox_events_retry_count_check CHECK ((retry_count >= 0)),
    CONSTRAINT outbox_events_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'PUBLISHED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: page_sections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.page_sections (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    page_version_id uuid NOT NULL,
    section_code character varying(150) NOT NULL,
    component_version_id uuid NOT NULL,
    content_version_id uuid,
    sort_order integer DEFAULT 0 NOT NULL,
    use_for_seo boolean DEFAULT false NOT NULL,
    settings_snapshot jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT page_sections_settings_snapshot_check CHECK ((jsonb_typeof(settings_snapshot) = 'object'::text))
);


--
-- Name: page_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.page_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    page_id uuid NOT NULL,
    version_no integer NOT NULL,
    layout_version_id uuid,
    redirect_config jsonb DEFAULT '{}'::jsonb NOT NULL,
    seo_snapshot jsonb DEFAULT '{}'::jsonb NOT NULL,
    locale_config jsonb DEFAULT '["vi-VN", "en-US"]'::jsonb NOT NULL,
    snapshot jsonb DEFAULT '{}'::jsonb NOT NULL,
    workflow_state character varying(50) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    CONSTRAINT page_versions_locale_config_check CHECK ((jsonb_typeof(locale_config) = 'array'::text)),
    CONSTRAINT page_versions_redirect_config_check CHECK ((jsonb_typeof(redirect_config) = 'object'::text)),
    CONSTRAINT page_versions_seo_snapshot_check CHECK ((jsonb_typeof(seo_snapshot) = 'object'::text)),
    CONSTRAINT page_versions_snapshot_check CHECK ((jsonb_typeof(snapshot) = 'object'::text)),
    CONSTRAINT page_versions_version_no_check CHECK ((version_no > 0))
);


--
-- Name: pages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    theme_id uuid NOT NULL,
    parent_id uuid,
    code character varying(150) NOT NULL,
    slug character varying(500) NOT NULL,
    page_type character varying(20) DEFAULT 'NORMAL'::character varying NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(255),
    CONSTRAINT pages_check CHECK (((parent_id IS NULL) OR (parent_id <> id))),
    CONSTRAINT pages_page_type_check CHECK (((page_type)::text = ANY ((ARRAY['NORMAL'::character varying, 'TAG'::character varying])::text[]))),
    CONSTRAINT pages_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    resource_type character varying(80) NOT NULL,
    action_code character varying(80) NOT NULL,
    state_code character varying(80),
    scope_type character varying(40) DEFAULT 'PRODUCT'::character varying NOT NULL,
    effect character varying(10) DEFAULT 'ALLOW'::character varying NOT NULL,
    description text,
    CONSTRAINT permissions_effect_check CHECK (((effect)::text = ANY ((ARRAY['ALLOW'::character varying, 'DENY'::character varying])::text[]))),
    CONSTRAINT permissions_scope_type_check CHECK (((scope_type)::text = ANY ((ARRAY['GLOBAL'::character varying, 'PRODUCT'::character varying, 'RESOURCE'::character varying])::text[])))
);


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.products (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(255),
    CONSTRAINT products_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: publish_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.publish_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    release_id uuid NOT NULL,
    event_type character varying(80) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    processed_at timestamp with time zone,
    CONSTRAINT publish_events_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'SUCCEEDED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: publish_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.publish_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    release_id uuid NOT NULL,
    action_code character varying(80) NOT NULL,
    actor_username character varying(150),
    details jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: publish_release_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.publish_release_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    release_id uuid NOT NULL,
    resource_type character varying(40) NOT NULL,
    resource_id uuid NOT NULL,
    version_id uuid NOT NULL,
    version_no integer NOT NULL,
    locale_scope jsonb DEFAULT '["vi-VN", "en-US"]'::jsonb NOT NULL,
    checksum_sha256 character varying(64),
    CONSTRAINT publish_release_items_locale_scope_check CHECK ((jsonb_typeof(locale_scope) = 'array'::text)),
    CONSTRAINT publish_release_items_resource_type_check CHECK (((resource_type)::text = ANY ((ARRAY['PAGE'::character varying, 'CONTENT'::character varying, 'COMPONENT'::character varying, 'LAYOUT'::character varying, 'TEMPLATE'::character varying, 'THEME'::character varying])::text[]))),
    CONSTRAINT publish_release_items_version_no_check CHECK ((version_no > 0))
);


--
-- Name: publish_releases; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.publish_releases (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    theme_id uuid,
    release_code character varying(150) NOT NULL,
    status character varying(30) DEFAULT 'DRAFT'::character varying NOT NULL,
    manifest jsonb DEFAULT '{}'::jsonb NOT NULL,
    idempotency_key character varying(255),
    comment text,
    created_by character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    published_by character varying(255),
    published_at timestamp with time zone,
    CONSTRAINT publish_releases_manifest_check CHECK ((jsonb_typeof(manifest) = 'object'::text)),
    CONSTRAINT publish_releases_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALIDATING'::character varying, 'READY'::character varying, 'PUBLISHING'::character varying, 'PUBLISHED'::character varying, 'FAILED'::character varying, 'ROLLED_BACK'::character varying])::text[])))
);


--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL
);


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    is_system boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: template_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.template_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    template_id uuid NOT NULL,
    version_no integer NOT NULL,
    schema_snapshot jsonb NOT NULL,
    workflow_id uuid,
    workflow_version_id uuid,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    CONSTRAINT template_versions_schema_snapshot_check CHECK ((jsonb_typeof(schema_snapshot) = 'object'::text)),
    CONSTRAINT template_versions_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'APPROVED'::character varying, 'RETIRED'::character varying])::text[]))),
    CONSTRAINT template_versions_version_no_check CHECK ((version_no > 0))
);


--
-- Name: templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    code character varying(150) NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(255),
    CONSTRAINT templates_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: themes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.themes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    root_base character varying(500) DEFAULT '/'::character varying NOT NULL,
    domains jsonb DEFAULT '[]'::jsonb NOT NULL,
    head_config jsonb DEFAULT '{}'::jsonb NOT NULL,
    body_config jsonb DEFAULT '{}'::jsonb NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(255),
    CONSTRAINT themes_body_config_check CHECK ((jsonb_typeof(body_config) = 'object'::text)),
    CONSTRAINT themes_domains_check CHECK ((jsonb_typeof(domains) = 'array'::text)),
    CONSTRAINT themes_head_config_check CHECK ((jsonb_typeof(head_config) = 'object'::text)),
    CONSTRAINT themes_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: user_product_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_product_roles (
    user_id uuid NOT NULL,
    product_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_by character varying(255),
    assigned_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone,
    CONSTRAINT user_product_roles_check CHECK (((expires_at IS NULL) OR (expires_at > assigned_at)))
);


--
-- Name: workflow_histories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workflow_histories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workflow_instance_id uuid NOT NULL,
    action_code character varying(80) NOT NULL,
    from_state_code character varying(80),
    to_state_code character varying(80) NOT NULL,
    actor_user_id uuid,
    actor_username character varying(150),
    comment text,
    correlation_id character varying(100),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: workflow_instances; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workflow_instances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workflow_id uuid NOT NULL,
    workflow_version_id uuid NOT NULL,
    product_id uuid NOT NULL,
    resource_type character varying(80) NOT NULL,
    resource_id uuid NOT NULL,
    current_state_id uuid NOT NULL,
    lock_version bigint DEFAULT 0 NOT NULL,
    started_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: workflow_states; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workflow_states (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workflow_version_id uuid NOT NULL,
    state_code character varying(80) NOT NULL,
    state_name character varying(150) NOT NULL,
    is_initial boolean DEFAULT false NOT NULL,
    is_terminal boolean DEFAULT false NOT NULL
);


--
-- Name: workflow_transitions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workflow_transitions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workflow_version_id uuid NOT NULL,
    action_code character varying(80) NOT NULL,
    from_state_id uuid NOT NULL,
    to_state_id uuid NOT NULL,
    permission_code character varying(100) NOT NULL,
    config jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT workflow_transitions_check CHECK ((from_state_id <> to_state_id)),
    CONSTRAINT workflow_transitions_config_check CHECK ((jsonb_typeof(config) = 'object'::text))
);


--
-- Name: workflow_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workflow_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workflow_id uuid NOT NULL,
    version_no integer NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    definition jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    CONSTRAINT workflow_versions_definition_check CHECK ((jsonb_typeof(definition) = 'object'::text)),
    CONSTRAINT workflow_versions_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'APPROVED'::character varying, 'RETIRED'::character varying])::text[]))),
    CONSTRAINT workflow_versions_version_no_check CHECK ((version_no > 0))
);


--
-- Name: workflows; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workflows (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    code character varying(150) NOT NULL,
    name character varying(255) NOT NULL,
    resource_type character varying(80) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    CONSTRAINT workflows_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


--
-- Name: app_users app_users_identity_source_external_subject_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_identity_source_external_subject_key UNIQUE (identity_source, external_subject);


--
-- Name: app_users app_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (id);


--
-- Name: app_users app_users_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_username_key UNIQUE (username);


--
-- Name: asset_folders asset_folders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_folders
    ADD CONSTRAINT asset_folders_pkey PRIMARY KEY (id);


--
-- Name: asset_references asset_references_asset_id_owner_type_owner_id_usage_type_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_references
    ADD CONSTRAINT asset_references_asset_id_owner_type_owner_id_usage_type_key UNIQUE (asset_id, owner_type, owner_id, usage_type);


--
-- Name: asset_references asset_references_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_references
    ADD CONSTRAINT asset_references_pkey PRIMARY KEY (id);


--
-- Name: asset_versions asset_versions_asset_id_version_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_versions
    ADD CONSTRAINT asset_versions_asset_id_version_no_key UNIQUE (asset_id, version_no);


--
-- Name: asset_versions asset_versions_bucket_name_object_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_versions
    ADD CONSTRAINT asset_versions_bucket_name_object_key_key UNIQUE (bucket_name, object_key);


--
-- Name: asset_versions asset_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_versions
    ADD CONSTRAINT asset_versions_pkey PRIMARY KEY (id);


--
-- Name: assets assets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assets
    ADD CONSTRAINT assets_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: component_versions component_versions_component_id_version_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component_versions
    ADD CONSTRAINT component_versions_component_id_version_no_key UNIQUE (component_id, version_no);


--
-- Name: component_versions component_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component_versions
    ADD CONSTRAINT component_versions_pkey PRIMARY KEY (id);


--
-- Name: components components_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.components
    ADD CONSTRAINT components_pkey PRIMARY KEY (id);


--
-- Name: components components_product_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.components
    ADD CONSTRAINT components_product_id_code_key UNIQUE (product_id, code);


--
-- Name: content_field_values content_field_values_content_version_id_field_definition_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_field_values
    ADD CONSTRAINT content_field_values_content_version_id_field_definition_id_key UNIQUE (content_version_id, field_definition_id, locale, item_index);


--
-- Name: content_field_values content_field_values_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_field_values
    ADD CONSTRAINT content_field_values_pkey PRIMARY KEY (id);


--
-- Name: content_versions content_versions_content_id_version_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_versions
    ADD CONSTRAINT content_versions_content_id_version_no_key UNIQUE (content_id, version_no);


--
-- Name: content_versions content_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_versions
    ADD CONSTRAINT content_versions_pkey PRIMARY KEY (id);


--
-- Name: contents contents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contents
    ADD CONSTRAINT contents_pkey PRIMARY KEY (id);


--
-- Name: contents contents_product_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contents
    ADD CONSTRAINT contents_product_id_code_key UNIQUE (product_id, code);


--
-- Name: field_definitions field_definitions_field_group_id_field_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.field_definitions
    ADD CONSTRAINT field_definitions_field_group_id_field_key_key UNIQUE (field_group_id, field_key);


--
-- Name: field_definitions field_definitions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.field_definitions
    ADD CONSTRAINT field_definitions_pkey PRIMARY KEY (id);


--
-- Name: field_groups field_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.field_groups
    ADD CONSTRAINT field_groups_pkey PRIMARY KEY (id);


--
-- Name: field_groups field_groups_template_version_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.field_groups
    ADD CONSTRAINT field_groups_template_version_id_code_key UNIQUE (template_version_id, code);


--
-- Name: layout_versions layout_versions_layout_id_version_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.layout_versions
    ADD CONSTRAINT layout_versions_layout_id_version_no_key UNIQUE (layout_id, version_no);


--
-- Name: layout_versions layout_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.layout_versions
    ADD CONSTRAINT layout_versions_pkey PRIMARY KEY (id);


--
-- Name: layouts layouts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.layouts
    ADD CONSTRAINT layouts_pkey PRIMARY KEY (id);


--
-- Name: layouts layouts_product_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.layouts
    ADD CONSTRAINT layouts_product_id_code_key UNIQUE (product_id, code);


--
-- Name: outbox_events outbox_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);


--
-- Name: page_sections page_sections_page_version_id_section_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_sections
    ADD CONSTRAINT page_sections_page_version_id_section_code_key UNIQUE (page_version_id, section_code);


--
-- Name: page_sections page_sections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_sections
    ADD CONSTRAINT page_sections_pkey PRIMARY KEY (id);


--
-- Name: page_versions page_versions_page_id_version_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_versions
    ADD CONSTRAINT page_versions_page_id_version_no_key UNIQUE (page_id, version_no);


--
-- Name: page_versions page_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_versions
    ADD CONSTRAINT page_versions_pkey PRIMARY KEY (id);


--
-- Name: pages pages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pages
    ADD CONSTRAINT pages_pkey PRIMARY KEY (id);


--
-- Name: pages pages_theme_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pages
    ADD CONSTRAINT pages_theme_id_code_key UNIQUE (theme_id, code);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: permissions permissions_resource_type_action_code_state_code_scope_type_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_resource_type_action_code_state_code_scope_type_key UNIQUE (resource_type, action_code, state_code, scope_type, effect);


--
-- Name: products products_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_code_key UNIQUE (code);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: publish_events publish_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_events
    ADD CONSTRAINT publish_events_pkey PRIMARY KEY (id);


--
-- Name: publish_history publish_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_history
    ADD CONSTRAINT publish_history_pkey PRIMARY KEY (id);


--
-- Name: publish_release_items publish_release_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_release_items
    ADD CONSTRAINT publish_release_items_pkey PRIMARY KEY (id);


--
-- Name: publish_release_items publish_release_items_release_id_resource_type_resource_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_release_items
    ADD CONSTRAINT publish_release_items_release_id_resource_type_resource_id_key UNIQUE (release_id, resource_type, resource_id);


--
-- Name: publish_releases publish_releases_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_releases
    ADD CONSTRAINT publish_releases_pkey PRIMARY KEY (id);


--
-- Name: publish_releases publish_releases_product_id_idempotency_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_releases
    ADD CONSTRAINT publish_releases_product_id_idempotency_key_key UNIQUE (product_id, idempotency_key);


--
-- Name: publish_releases publish_releases_product_id_release_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_releases
    ADD CONSTRAINT publish_releases_product_id_release_code_key UNIQUE (product_id, release_code);


--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);


--
-- Name: roles roles_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_code_key UNIQUE (code);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: template_versions template_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_versions
    ADD CONSTRAINT template_versions_pkey PRIMARY KEY (id);


--
-- Name: template_versions template_versions_template_id_version_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_versions
    ADD CONSTRAINT template_versions_template_id_version_no_key UNIQUE (template_id, version_no);


--
-- Name: templates templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.templates
    ADD CONSTRAINT templates_pkey PRIMARY KEY (id);


--
-- Name: templates templates_product_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.templates
    ADD CONSTRAINT templates_product_id_code_key UNIQUE (product_id, code);


--
-- Name: themes themes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT themes_pkey PRIMARY KEY (id);


--
-- Name: themes themes_product_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT themes_product_id_code_key UNIQUE (product_id, code);


--
-- Name: user_product_roles user_product_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_product_roles
    ADD CONSTRAINT user_product_roles_pkey PRIMARY KEY (user_id, product_id, role_id);


--
-- Name: workflow_histories workflow_histories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_histories
    ADD CONSTRAINT workflow_histories_pkey PRIMARY KEY (id);


--
-- Name: workflow_instances workflow_instances_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_instances
    ADD CONSTRAINT workflow_instances_pkey PRIMARY KEY (id);


--
-- Name: workflow_instances workflow_instances_resource_type_resource_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_instances
    ADD CONSTRAINT workflow_instances_resource_type_resource_id_key UNIQUE (resource_type, resource_id);


--
-- Name: workflow_states workflow_states_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_states
    ADD CONSTRAINT workflow_states_pkey PRIMARY KEY (id);


--
-- Name: workflow_states workflow_states_workflow_version_id_state_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_states
    ADD CONSTRAINT workflow_states_workflow_version_id_state_code_key UNIQUE (workflow_version_id, state_code);


--
-- Name: workflow_transitions workflow_transitions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_transitions
    ADD CONSTRAINT workflow_transitions_pkey PRIMARY KEY (id);


--
-- Name: workflow_transitions workflow_transitions_workflow_version_id_action_code_from_s_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_transitions
    ADD CONSTRAINT workflow_transitions_workflow_version_id_action_code_from_s_key UNIQUE (workflow_version_id, action_code, from_state_id);


--
-- Name: workflow_versions workflow_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_versions
    ADD CONSTRAINT workflow_versions_pkey PRIMARY KEY (id);


--
-- Name: workflow_versions workflow_versions_workflow_id_version_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_versions
    ADD CONSTRAINT workflow_versions_workflow_id_version_no_key UNIQUE (workflow_id, version_no);


--
-- Name: workflows workflows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflows
    ADD CONSTRAINT workflows_pkey PRIMARY KEY (id);


--
-- Name: workflows workflows_product_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflows
    ADD CONSTRAINT workflows_product_id_code_key UNIQUE (product_id, code);


--
-- Name: idx_asset_references_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asset_references_owner ON public.asset_references USING btree (owner_type, owner_id);


--
-- Name: idx_audit_product_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_product_time ON public.audit_logs USING btree (product_id, created_at DESC);


--
-- Name: idx_audit_resource_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_resource_time ON public.audit_logs USING btree (resource_type, resource_id, created_at DESC);


--
-- Name: idx_content_field_value_json; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_field_value_json ON public.content_field_values USING gin (value jsonb_path_ops);


--
-- Name: idx_content_field_value_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_field_value_lookup ON public.content_field_values USING btree (field_definition_id, locale);


--
-- Name: idx_content_versions_state; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_versions_state ON public.content_versions USING btree (content_id, workflow_state);


--
-- Name: idx_outbox_dispatch; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbox_dispatch ON public.outbox_events USING btree (status, next_retry_at, created_at);


--
-- Name: idx_page_sections_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_page_sections_order ON public.page_sections USING btree (page_version_id, sort_order);


--
-- Name: idx_pages_theme_parent_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pages_theme_parent_order ON public.pages USING btree (theme_id, parent_id, sort_order);


--
-- Name: idx_pages_theme_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pages_theme_slug ON public.pages USING btree (theme_id, slug);


--
-- Name: uq_workflow_initial_state; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_workflow_initial_state ON public.workflow_states USING btree (workflow_version_id) WHERE is_initial;


--
-- Name: asset_folders asset_folders_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_folders
    ADD CONSTRAINT asset_folders_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.asset_folders(id);


--
-- Name: asset_folders asset_folders_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_folders
    ADD CONSTRAINT asset_folders_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: asset_references asset_references_asset_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_references
    ADD CONSTRAINT asset_references_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.assets(id);


--
-- Name: asset_versions asset_versions_asset_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asset_versions
    ADD CONSTRAINT asset_versions_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.assets(id);


--
-- Name: assets assets_folder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assets
    ADD CONSTRAINT assets_folder_id_fkey FOREIGN KEY (folder_id) REFERENCES public.asset_folders(id);


--
-- Name: assets assets_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assets
    ADD CONSTRAINT assets_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: audit_logs audit_logs_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.app_users(id);


--
-- Name: audit_logs audit_logs_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: component_versions component_versions_component_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component_versions
    ADD CONSTRAINT component_versions_component_id_fkey FOREIGN KEY (component_id) REFERENCES public.components(id);


--
-- Name: components components_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.components
    ADD CONSTRAINT components_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: content_field_values content_field_values_content_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_field_values
    ADD CONSTRAINT content_field_values_content_version_id_fkey FOREIGN KEY (content_version_id) REFERENCES public.content_versions(id) ON DELETE CASCADE;


--
-- Name: content_field_values content_field_values_field_definition_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_field_values
    ADD CONSTRAINT content_field_values_field_definition_id_fkey FOREIGN KEY (field_definition_id) REFERENCES public.field_definitions(id);


--
-- Name: content_versions content_versions_content_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_versions
    ADD CONSTRAINT content_versions_content_id_fkey FOREIGN KEY (content_id) REFERENCES public.contents(id);


--
-- Name: content_versions content_versions_template_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_versions
    ADD CONSTRAINT content_versions_template_version_id_fkey FOREIGN KEY (template_version_id) REFERENCES public.template_versions(id);


--
-- Name: contents contents_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contents
    ADD CONSTRAINT contents_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: contents contents_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contents
    ADD CONSTRAINT contents_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.templates(id);


--
-- Name: field_definitions field_definitions_field_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.field_definitions
    ADD CONSTRAINT field_definitions_field_group_id_fkey FOREIGN KEY (field_group_id) REFERENCES public.field_groups(id) ON DELETE CASCADE;


--
-- Name: field_groups field_groups_template_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.field_groups
    ADD CONSTRAINT field_groups_template_version_id_fkey FOREIGN KEY (template_version_id) REFERENCES public.template_versions(id) ON DELETE CASCADE;


--
-- Name: template_versions fk_template_workflow; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_versions
    ADD CONSTRAINT fk_template_workflow FOREIGN KEY (workflow_id) REFERENCES public.workflows(id);


--
-- Name: template_versions fk_template_workflow_version; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_versions
    ADD CONSTRAINT fk_template_workflow_version FOREIGN KEY (workflow_version_id) REFERENCES public.workflow_versions(id);


--
-- Name: layout_versions layout_versions_layout_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.layout_versions
    ADD CONSTRAINT layout_versions_layout_id_fkey FOREIGN KEY (layout_id) REFERENCES public.layouts(id);


--
-- Name: layouts layouts_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.layouts
    ADD CONSTRAINT layouts_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: page_sections page_sections_component_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_sections
    ADD CONSTRAINT page_sections_component_version_id_fkey FOREIGN KEY (component_version_id) REFERENCES public.component_versions(id);


--
-- Name: page_sections page_sections_content_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_sections
    ADD CONSTRAINT page_sections_content_version_id_fkey FOREIGN KEY (content_version_id) REFERENCES public.content_versions(id);


--
-- Name: page_sections page_sections_page_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_sections
    ADD CONSTRAINT page_sections_page_version_id_fkey FOREIGN KEY (page_version_id) REFERENCES public.page_versions(id) ON DELETE CASCADE;


--
-- Name: page_versions page_versions_layout_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_versions
    ADD CONSTRAINT page_versions_layout_version_id_fkey FOREIGN KEY (layout_version_id) REFERENCES public.layout_versions(id);


--
-- Name: page_versions page_versions_page_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_versions
    ADD CONSTRAINT page_versions_page_id_fkey FOREIGN KEY (page_id) REFERENCES public.pages(id);


--
-- Name: pages pages_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pages
    ADD CONSTRAINT pages_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.pages(id);


--
-- Name: pages pages_theme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pages
    ADD CONSTRAINT pages_theme_id_fkey FOREIGN KEY (theme_id) REFERENCES public.themes(id);


--
-- Name: publish_events publish_events_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_events
    ADD CONSTRAINT publish_events_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.publish_releases(id);


--
-- Name: publish_history publish_history_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_history
    ADD CONSTRAINT publish_history_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.publish_releases(id);


--
-- Name: publish_release_items publish_release_items_release_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_release_items
    ADD CONSTRAINT publish_release_items_release_id_fkey FOREIGN KEY (release_id) REFERENCES public.publish_releases(id) ON DELETE CASCADE;


--
-- Name: publish_releases publish_releases_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_releases
    ADD CONSTRAINT publish_releases_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: publish_releases publish_releases_theme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publish_releases
    ADD CONSTRAINT publish_releases_theme_id_fkey FOREIGN KEY (theme_id) REFERENCES public.themes(id);


--
-- Name: role_permissions role_permissions_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions(id) ON DELETE CASCADE;


--
-- Name: role_permissions role_permissions_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;


--
-- Name: template_versions template_versions_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_versions
    ADD CONSTRAINT template_versions_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.templates(id);


--
-- Name: templates templates_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.templates
    ADD CONSTRAINT templates_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: themes themes_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT themes_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: user_product_roles user_product_roles_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_product_roles
    ADD CONSTRAINT user_product_roles_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE CASCADE;


--
-- Name: user_product_roles user_product_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_product_roles
    ADD CONSTRAINT user_product_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;


--
-- Name: user_product_roles user_product_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_product_roles
    ADD CONSTRAINT user_product_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_users(id) ON DELETE CASCADE;


--
-- Name: workflow_histories workflow_histories_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_histories
    ADD CONSTRAINT workflow_histories_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.app_users(id);


--
-- Name: workflow_histories workflow_histories_workflow_instance_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_histories
    ADD CONSTRAINT workflow_histories_workflow_instance_id_fkey FOREIGN KEY (workflow_instance_id) REFERENCES public.workflow_instances(id);


--
-- Name: workflow_instances workflow_instances_current_state_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_instances
    ADD CONSTRAINT workflow_instances_current_state_id_fkey FOREIGN KEY (current_state_id) REFERENCES public.workflow_states(id);


--
-- Name: workflow_instances workflow_instances_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_instances
    ADD CONSTRAINT workflow_instances_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: workflow_instances workflow_instances_workflow_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_instances
    ADD CONSTRAINT workflow_instances_workflow_id_fkey FOREIGN KEY (workflow_id) REFERENCES public.workflows(id);


--
-- Name: workflow_instances workflow_instances_workflow_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_instances
    ADD CONSTRAINT workflow_instances_workflow_version_id_fkey FOREIGN KEY (workflow_version_id) REFERENCES public.workflow_versions(id);


--
-- Name: workflow_states workflow_states_workflow_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_states
    ADD CONSTRAINT workflow_states_workflow_version_id_fkey FOREIGN KEY (workflow_version_id) REFERENCES public.workflow_versions(id) ON DELETE CASCADE;


--
-- Name: workflow_transitions workflow_transitions_from_state_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_transitions
    ADD CONSTRAINT workflow_transitions_from_state_id_fkey FOREIGN KEY (from_state_id) REFERENCES public.workflow_states(id);


--
-- Name: workflow_transitions workflow_transitions_to_state_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_transitions
    ADD CONSTRAINT workflow_transitions_to_state_id_fkey FOREIGN KEY (to_state_id) REFERENCES public.workflow_states(id);


--
-- Name: workflow_transitions workflow_transitions_workflow_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_transitions
    ADD CONSTRAINT workflow_transitions_workflow_version_id_fkey FOREIGN KEY (workflow_version_id) REFERENCES public.workflow_versions(id) ON DELETE CASCADE;


--
-- Name: workflow_versions workflow_versions_workflow_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflow_versions
    ADD CONSTRAINT workflow_versions_workflow_id_fkey FOREIGN KEY (workflow_id) REFERENCES public.workflows(id);


--
-- Name: workflows workflows_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workflows
    ADD CONSTRAINT workflows_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- PostgreSQL database dump complete
--

\unrestrict vhryFcTmACtMGERmHaLknmocFAdb0SB5BN5RoS2oegmW9jfxv5CNdryChuCcfpz

