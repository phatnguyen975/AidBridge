-- ============================================================================
-- AidBridge Database Schema v3.0
-- File: 005_triggers.sql
-- Description: Trigger functions and triggers
-- ============================================================================
-- ============================================================================
-- TRIGGER FUNCTIONS
-- ============================================================================
-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION trigger_set_updated_at() RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Update volunteer avg_rating after new rating
CREATE OR REPLACE FUNCTION trigger_update_volunteer_rating() RETURNS TRIGGER AS $$ BEGIN
UPDATE volunteer_profiles
SET avg_rating = (
        SELECT COALESCE(AVG(r.score), 0)
        FROM ratings r
            JOIN missions m ON r.mission_id = m.id
        WHERE m.volunteer_id = (
                SELECT user_id
                FROM volunteer_profiles
                WHERE user_id = NEW.ratee_id
            )
    ),
    updated_at = NOW()
WHERE user_id = NEW.ratee_id;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Update sponsor stats after donation received
CREATE OR REPLACE FUNCTION trigger_update_sponsor_stats() RETURNS TRIGGER AS $$
DECLARE v_total_items INTEGER;
BEGIN IF NEW.status = 'RECEIVED'
AND (
    OLD.status IS NULL
    OR OLD.status != 'RECEIVED'
) THEN -- Calculate total items in this donation
SELECT COALESCE(SUM(quantity), 0) INTO v_total_items
FROM donation_items
WHERE donation_id = NEW.id;
-- Update sponsor profile
UPDATE sponsor_profiles
SET donation_count = donation_count + 1,
    total_items_donated = total_items_donated + v_total_items,
    total_points = total_points + (v_total_items * 10),
    -- 10 points per item
    badge_level = CASE
        WHEN total_points + (v_total_items * 10) >= 10000 THEN 'PLATINUM'
        WHEN total_points + (v_total_items * 10) >= 5000 THEN 'GOLD'
        WHEN total_points + (v_total_items * 10) >= 1000 THEN 'SILVER'
        ELSE 'BRONZE'
    END,
    updated_at = NOW()
WHERE user_id = NEW.sponsor_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Update volunteer stats after mission completed
CREATE OR REPLACE FUNCTION trigger_update_volunteer_stats() RETURNS TRIGGER AS $$ BEGIN IF NEW.status = 'COMPLETED'
    AND (
        OLD.status IS NULL
        OR OLD.status != 'COMPLETED'
    ) THEN
UPDATE volunteer_profiles
SET total_tasks_completed = total_tasks_completed + 1,
    updated_at = NOW()
WHERE user_id = NEW.volunteer_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Update hub inventory after donation received
CREATE OR REPLACE FUNCTION trigger_update_inventory_on_donation() RETURNS TRIGGER AS $$ BEGIN IF NEW.status = 'RECEIVED'
    AND (
        OLD.status IS NULL
        OR OLD.status != 'RECEIVED'
    ) THEN -- Insert or update inventory for each donation item
INSERT INTO hub_inventories (hub_id, item_category_id, current_quantity)
SELECT NEW.hub_id,
    di.item_category_id,
    di.quantity
FROM donation_items di
WHERE di.donation_id = NEW.id ON CONFLICT (hub_id, item_category_id) DO
UPDATE
SET current_quantity = hub_inventories.current_quantity + EXCLUDED.current_quantity,
    updated_at = NOW();
-- Log the inventory changes
INSERT INTO inventory_logs (
        hub_inventory_id,
        change_type,
        quantity_delta,
        reference_type,
        reference_id,
        performed_by,
        quantity_after
    )
SELECT hi.id,
    'DONATION_IN',
    di.quantity,
    'DONATION',
    NEW.id,
    NEW.received_by,
    hi.current_quantity
FROM donation_items di
    JOIN hub_inventories hi ON hi.hub_id = NEW.hub_id
    AND hi.item_category_id = di.item_category_id
WHERE di.donation_id = NEW.id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- ============================================================================
-- TRIGGERS
-- ============================================================================
-- Auto-update updated_at triggers
CREATE TRIGGER set_updated_at_users BEFORE
UPDATE ON users FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_volunteer_profiles BEFORE
UPDATE ON volunteer_profiles FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_sponsor_profiles BEFORE
UPDATE ON sponsor_profiles FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_hubs BEFORE
UPDATE ON hubs FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_shelters BEFORE
UPDATE ON shelters FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_hub_inventories BEFORE
UPDATE ON hub_inventories FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_sos_requests BEFORE
UPDATE ON sos_requests FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_aid_requests BEFORE
UPDATE ON aid_requests FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_donations BEFORE
UPDATE ON donations FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
CREATE TRIGGER set_updated_at_missions BEFORE
UPDATE ON missions FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
-- Business logic triggers
CREATE TRIGGER update_volunteer_rating
AFTER
INSERT ON ratings FOR EACH ROW EXECUTE FUNCTION trigger_update_volunteer_rating();
CREATE TRIGGER update_sponsor_stats
AFTER
UPDATE ON donations FOR EACH ROW EXECUTE FUNCTION trigger_update_sponsor_stats();
CREATE TRIGGER update_volunteer_stats
AFTER
UPDATE ON missions FOR EACH ROW EXECUTE FUNCTION trigger_update_volunteer_stats();
CREATE TRIGGER update_inventory_on_donation
AFTER
UPDATE ON donations FOR EACH ROW EXECUTE FUNCTION trigger_update_inventory_on_donation();