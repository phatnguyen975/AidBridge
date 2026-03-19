-- ============================================================================
-- AidBridge Database Schema v3.0
-- File: 004_indexes.sql
-- Description: Index definitions (~35 indexes)
-- ============================================================================
-- ============================================================================
-- AUTH INDEXES
-- ============================================================================
-- Users
CREATE INDEX idx_users_email ON users(email)
WHERE email IS NOT NULL;
CREATE INDEX idx_users_phone ON users(phone_number)
WHERE phone_number IS NOT NULL;
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(is_active)
WHERE is_active = TRUE;
CREATE INDEX idx_users_otp_expires ON users(otp_expires_at)
WHERE otp_code IS NOT NULL;
-- Refresh tokens
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at)
WHERE is_revoked = FALSE;
-- ============================================================================
-- PROFILE INDEXES
-- ============================================================================
-- Volunteer profiles
CREATE INDEX idx_volunteer_user ON volunteer_profiles(user_id);
CREATE INDEX idx_volunteer_online ON volunteer_profiles(is_online)
WHERE is_online = TRUE;
CREATE INDEX idx_volunteer_location ON volunteer_profiles(current_lat, current_lng)
WHERE is_online = TRUE;
CREATE INDEX idx_volunteer_rating ON volunteer_profiles(avg_rating DESC);
-- Sponsor profiles
CREATE INDEX idx_sponsor_user ON sponsor_profiles(user_id);
CREATE INDEX idx_sponsor_points ON sponsor_profiles(total_points DESC);
CREATE INDEX idx_sponsor_badge ON sponsor_profiles(badge_level);
-- ============================================================================
-- INFRASTRUCTURE INDEXES
-- ============================================================================
-- Hubs
CREATE INDEX idx_hubs_status ON hubs(status)
WHERE status = 'ACTIVE';
CREATE INDEX idx_hubs_location ON hubs(lat, lng);
-- Hub staff
CREATE INDEX idx_hub_staff_hub ON hub_staff(hub_id);
CREATE INDEX idx_hub_staff_user ON hub_staff(user_id);
CREATE UNIQUE INDEX idx_hub_staff_active ON hub_staff(hub_id, user_id)
WHERE unassigned_at IS NULL;
-- Shelters
CREATE INDEX idx_shelters_status ON shelters(status);
CREATE INDEX idx_shelters_location ON shelters(lat, lng);
CREATE INDEX idx_shelters_capacity ON shelters(current_capacity, max_capacity)
WHERE status = 'ACTIVE';
-- ============================================================================
-- CATALOG & INVENTORY INDEXES
-- ============================================================================
-- Item categories
CREATE INDEX idx_item_categories_parent ON item_categories(parent_id);
CREATE INDEX idx_item_categories_leaf ON item_categories(is_leaf)
WHERE is_leaf = TRUE;
-- Hub inventories
CREATE INDEX idx_hub_inventories_hub ON hub_inventories(hub_id);
CREATE INDEX idx_hub_inventories_category ON hub_inventories(item_category_id);
CREATE INDEX idx_hub_inventories_low_stock ON hub_inventories(hub_id, current_quantity)
WHERE current_quantity <= low_stock_threshold;
-- Inventory logs
CREATE INDEX idx_inventory_logs_hub_inv ON inventory_logs(hub_inventory_id);
CREATE INDEX idx_inventory_logs_created ON inventory_logs(created_at DESC);
CREATE INDEX idx_inventory_logs_reference ON inventory_logs(reference_type, reference_id);
-- ============================================================================
-- REQUEST INDEXES
-- ============================================================================
-- SOS requests
CREATE INDEX idx_sos_requester ON sos_requests(requester_id);
CREATE INDEX idx_sos_status ON sos_requests(status);
CREATE INDEX idx_sos_location ON sos_requests(victim_lat, victim_lng);
CREATE INDEX idx_sos_created ON sos_requests(created_at DESC);
CREATE INDEX idx_sos_pending ON sos_requests(urgency_level, created_at)
WHERE status = 'PENDING';
-- Aid requests
CREATE INDEX idx_aid_requester ON aid_requests(requester_id);
CREATE INDEX idx_aid_status ON aid_requests(status);
CREATE INDEX idx_aid_location ON aid_requests(lat, lng);
CREATE INDEX idx_aid_created ON aid_requests(created_at DESC);
CREATE INDEX idx_aid_pending ON aid_requests(urgency_level, created_at)
WHERE status = 'PENDING';
-- Aid request items
CREATE INDEX idx_aid_items_request ON aid_request_items(aid_request_id);
CREATE INDEX idx_aid_items_category ON aid_request_items(item_category_id);
-- ============================================================================
-- DONATION INDEXES
-- ============================================================================
-- Donations
CREATE INDEX idx_donations_sponsor ON donations(sponsor_id);
CREATE INDEX idx_donations_hub ON donations(hub_id);
CREATE INDEX idx_donations_status ON donations(status);
CREATE INDEX idx_donations_qr ON donations(qr_code_token)
WHERE qr_code_token IS NOT NULL;
CREATE INDEX idx_donations_created ON donations(created_at DESC);
-- Donation items
CREATE INDEX idx_donation_items_donation ON donation_items(donation_id);
CREATE INDEX idx_donation_items_category ON donation_items(item_category_id);
CREATE INDEX idx_donation_items_expiry ON donation_items(expiry_date)
WHERE expiry_date IS NOT NULL;
-- ============================================================================
-- MISSION INDEXES
-- ============================================================================
-- Missions
CREATE INDEX idx_missions_type ON missions(mission_type);
CREATE INDEX idx_missions_sos ON missions(sos_request_id)
WHERE sos_request_id IS NOT NULL;
CREATE INDEX idx_missions_aid ON missions(aid_request_id)
WHERE aid_request_id IS NOT NULL;
CREATE INDEX idx_missions_volunteer ON missions(volunteer_id)
WHERE volunteer_id IS NOT NULL;
CREATE INDEX idx_missions_hub ON missions(hub_id)
WHERE hub_id IS NOT NULL;
CREATE INDEX idx_missions_status ON missions(status);
CREATE INDEX idx_missions_qr ON missions(qr_code_token)
WHERE qr_code_token IS NOT NULL;
CREATE INDEX idx_missions_priority ON missions(priority_score DESC)
WHERE status = 'PENDING';
CREATE INDEX idx_missions_created ON missions(created_at DESC);
-- Dispatch attempts
CREATE INDEX idx_dispatch_mission ON dispatch_attempts(mission_id);
CREATE INDEX idx_dispatch_volunteer ON dispatch_attempts(volunteer_id);
CREATE INDEX idx_dispatch_pending ON dispatch_attempts(response, sent_at)
WHERE response = 'PENDING';
-- ============================================================================
-- COMMUNICATION INDEXES
-- ============================================================================
-- Chat messages
CREATE INDEX idx_chat_mission ON chat_messages(mission_id);
CREATE INDEX idx_chat_sender ON chat_messages(sender_id);
CREATE INDEX idx_chat_created ON chat_messages(mission_id, created_at DESC);
CREATE INDEX idx_chat_unread ON chat_messages(mission_id, is_read)
WHERE is_read = FALSE;
-- Ratings
CREATE INDEX idx_ratings_rater ON ratings(rater_id);
CREATE INDEX idx_ratings_ratee ON ratings(ratee_id);
CREATE INDEX idx_ratings_score ON ratings(ratee_id, score);
-- Notifications
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, created_at DESC)
WHERE is_read = FALSE;
CREATE INDEX idx_notifications_related ON notifications(related_type, related_id);