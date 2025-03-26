CREATE TABLE IF NOT EXISTS organization (
    id uuid DEFAULT public.gen_random_uuid() NOT NULL PRIMARY KEY,
    name character varying NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT NOW(),
    updated_at timestamp without time zone NOT NULL DEFAULT NOW()
);
