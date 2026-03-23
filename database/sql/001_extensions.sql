-- ============================================================================
-- AidBridge Database Schema v3.0
-- File: 001_extensions.sql
-- Description: PostgreSQL extensions
-- ============================================================================
-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- Enable case-insensitive text (for email, etc.)
CREATE EXTENSION IF NOT EXISTS "citext";