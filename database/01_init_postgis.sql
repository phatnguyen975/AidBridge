-- ============================================================
-- AidBridge: PostGIS Extension Initialization
-- Run this script ONCE on a fresh Supabase/PostgreSQL database
-- before executing 02_schema.sql.
-- ============================================================

-- Enable PostGIS for geospatial data types (GEOMETRY, GEOGRAPHY)
-- and spatial functions (ST_DWithin, ST_Distance, ST_SetSRID, etc.).
-- Required for all location-based queries: radius search for volunteers,
-- Hub/Shelter map display, and SOS heatmap generation.
CREATE EXTENSION IF NOT EXISTS postgis;

-- Enable the uuid-ossp extension for UUID generation (gen_random_uuid() fallback).
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
