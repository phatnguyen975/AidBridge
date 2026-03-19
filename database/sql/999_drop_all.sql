-- ============================================================================
-- AidBridge Database Schema v3.0
-- File: 999_drop_all.sql
-- Description: Drop all tables, types, and extensions (USE WITH CAUTION!)
--
-- WARNING: This will permanently delete ALL data!
-- ============================================================================
\ echo '==================================================' \ echo 'WARNING: Dropping ALL AidBridge database objects!' \ echo '==================================================' -- Drop all tables (in reverse dependency order)
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS ratings CASCADE;
DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS dispatch_attempts CASCADE;
DROP TABLE IF EXISTS missions CASCADE;
DROP TABLE IF EXISTS donation_items CASCADE;
DROP TABLE IF EXISTS donations CASCADE;
DROP TABLE IF EXISTS aid_request_items CASCADE;
DROP TABLE IF EXISTS aid_requests CASCADE;
DROP TABLE IF EXISTS sos_requests CASCADE;
DROP TABLE IF EXISTS inventory_logs CASCADE;
DROP TABLE IF EXISTS hub_inventories CASCADE;
DROP TABLE IF EXISTS hub_accepted_categories CASCADE;
DROP TABLE IF EXISTS item_categories CASCADE;
DROP TABLE IF EXISTS system_config CASCADE;
DROP TABLE IF EXISTS shelters CASCADE;
DROP TABLE IF EXISTS hub_staff CASCADE;
DROP TABLE IF EXISTS hubs CASCADE;
DROP TABLE IF EXISTS sponsor_profiles CASCADE;
DROP TABLE IF EXISTS volunteer_profiles CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;
-- Drop all ENUM types
DROP TYPE IF EXISTS notification_related_type CASCADE;
DROP TYPE IF EXISTS message_type CASCADE;
DROP TYPE IF EXISTS inventory_reference_type CASCADE;
DROP TYPE IF EXISTS inventory_change_type CASCADE;
DROP TYPE IF EXISTS otp_type CASCADE;
DROP TYPE IF EXISTS vehicle_type CASCADE;
DROP TYPE IF EXISTS badge_level CASCADE;
DROP TYPE IF EXISTS dispatch_response CASCADE;
DROP TYPE IF EXISTS mission_status CASCADE;
DROP TYPE IF EXISTS mission_type CASCADE;
DROP TYPE IF EXISTS donation_status CASCADE;
DROP TYPE IF EXISTS aid_status CASCADE;
DROP TYPE IF EXISTS sos_status CASCADE;
DROP TYPE IF EXISTS urgency_level CASCADE;
DROP TYPE IF EXISTS shelter_status CASCADE;
DROP TYPE IF EXISTS hub_status CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;
-- Drop trigger functions
DROP FUNCTION IF EXISTS trigger_set_updated_at CASCADE;
DROP FUNCTION IF EXISTS trigger_update_volunteer_rating CASCADE;
DROP FUNCTION IF EXISTS trigger_update_sponsor_stats CASCADE;
DROP FUNCTION IF EXISTS trigger_update_volunteer_stats CASCADE;
DROP FUNCTION IF EXISTS trigger_update_inventory_on_donation CASCADE;
\ echo '' \ echo 'All database objects dropped successfully.' \ echo 'Run 000_full_migration.sql to recreate the schema.'