-- ============================================================================
-- AidBridge Database Schema v3.0
-- File: 002_enums.sql
-- Description: ENUM type definitions (10 ENUMs)
-- ============================================================================
-- User roles in the system
CREATE TYPE user_role AS ENUM (
    'VICTIM',
    'VOLUNTEER',
    'SPONSOR',
    'STAFF',
    'ADMIN'
);
-- Hub operational status
CREATE TYPE hub_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'EMERGENCY'
);
-- Shelter operational status
CREATE TYPE shelter_status AS ENUM ('ACTIVE', 'INACTIVE', 'FULL');
-- Request urgency levels
CREATE TYPE urgency_level AS ENUM (
    'CRITICAL',
    'HIGH',
    'MEDIUM',
    'LOW'
);
-- SOS request status
CREATE TYPE sos_status AS ENUM (
    'PENDING',
    'DISPATCHING',
    'ASSIGNED',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
);
-- Aid request status
CREATE TYPE aid_status AS ENUM (
    'PENDING',
    'DISPATCHING',
    'ASSIGNED',
    'PICKED_UP',
    'IN_TRANSIT',
    'COMPLETED',
    'CANCELLED'
);
-- Donation status
CREATE TYPE donation_status AS ENUM (
    'REGISTERED',
    'QR_GENERATED',
    'RECEIVED',
    'REJECTED'
);
-- Mission types
CREATE TYPE mission_type AS ENUM ('RESCUE', 'DELIVERY');
-- Mission status
CREATE TYPE mission_status AS ENUM (
    'PENDING',
    'DISPATCHING',
    'ASSIGNED',
    'PICKING_UP',
    'PICKED_UP',
    'IN_TRANSIT',
    'COMPLETED',
    'CANCELLED'
);
-- Dispatch response status
CREATE TYPE dispatch_response AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'TIMEOUT'
);
-- Sponsor badge levels
CREATE TYPE badge_level AS ENUM (
    'BRONZE',
    'SILVER',
    'GOLD',
    'PLATINUM'
);
-- Additional ENUMs (inferred from schema)
-- Vehicle types for volunteers
CREATE TYPE vehicle_type AS ENUM (
    'MOTORBIKE',
    'CAR',
    'BICYCLE',
    'WALKING'
);
-- OTP types
CREATE TYPE otp_type AS ENUM (
    'REGISTER',
    'FORGOT_PASSWORD',
    'VERIFY_PHONE'
);
-- Inventory change types
CREATE TYPE inventory_change_type AS ENUM (
    'DONATION_IN',
    'MISSION_OUT',
    'ADJUSTMENT',
    'INITIAL',
    'EXPIRED'
);
-- Reference types for inventory logs
CREATE TYPE inventory_reference_type AS ENUM (
    'DONATION',
    'MISSION',
    'MANUAL'
);
-- Chat message types
CREATE TYPE message_type AS ENUM ('TEXT', 'IMAGE');
-- Notification related types
CREATE TYPE notification_related_type AS ENUM (
    'MISSION',
    'DONATION',
    'SOS_REQUEST',
    'AID_REQUEST',
    'SYSTEM'
);