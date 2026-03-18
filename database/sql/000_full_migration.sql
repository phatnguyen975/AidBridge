-- ============================================================================
-- AidBridge Database Schema v3.0
-- File: 000_full_migration.sql
-- Description: Complete migration script (combines all files in order)
--
-- Usage: psql -d aidbridge -f 000_full_migration.sql
-- Or run individual files in order: 001 -> 006
-- ============================================================================
\ echo '==================================================' \ echo 'AidBridge Database Migration v3.0' \ echo '==================================================' -- Start transaction for atomic migration
BEGIN;
\ echo '' \ echo '[1/6] Installing extensions...' \ ir 001_extensions.sql \ echo '' \ echo '[2/6] Creating ENUM types...' \ ir 002_enums.sql \ echo '' \ echo '[3/6] Creating tables...' \ ir 003_tables.sql \ echo '' \ echo '[4/6] Creating indexes...' \ ir 004_indexes.sql \ echo '' \ echo '[5/6] Creating triggers...' \ ir 005_triggers.sql \ echo '' \ echo '[6/6] Inserting seed data...' \ ir 006_seed_data.sql COMMIT;
\ echo '' \ echo '==================================================' \ echo 'Migration completed successfully!' \ echo '==================================================' -- Show table summary
\ echo '' \ echo 'Table Summary:'
SELECT schemaname as schema,
    COUNT(*) as table_count
FROM pg_tables
WHERE schemaname = 'public'
GROUP BY schemaname;
\ echo '' \ echo 'ENUM Summary:'
SELECT t.typname as enum_name,
    array_agg(
        e.enumlabel
        ORDER BY e.enumsortorder
    ) as
values
FROM pg_type t
    JOIN pg_enum e ON t.oid = e.enumtypid
    JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace
WHERE n.nspname = 'public'
GROUP BY t.typname
ORDER BY t.typname;