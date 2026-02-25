DO
$$
DECLARE
    status_udt_name text;
    status_table_schema text;
BEGIN
    SELECT c.udt_name, c.table_schema
    INTO status_udt_name, status_table_schema
    FROM information_schema.columns c
    WHERE c.table_name = 'posts'
      AND c.column_name = 'status'
    ORDER BY (c.table_schema = current_schema()) DESC
    LIMIT 1;

    IF status_udt_name IS NULL THEN
        RAISE NOTICE 'posts.status column not found. skip normalization.';
        RETURN;
    END IF;

    IF status_udt_name NOT IN ('varchar', 'text') THEN
        EXECUTE format(
            'ALTER TABLE %I.posts ALTER COLUMN status TYPE varchar(20) USING status::text',
            status_table_schema
        );
        RAISE NOTICE 'normalized %.posts.status from enum-like type (%) to varchar(20).', status_table_schema, status_udt_name;
    END IF;
END
$$;
