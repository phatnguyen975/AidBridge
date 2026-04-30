-- Seed data for STAFF Inventory Management.
--
-- Purpose:
-- - Create the parent categories used by Staff Inventory chips.
-- - Create leaf item categories shown as inventory products.
-- - Seed hub inventory rows for all ACTIVE hubs.
-- - Assign the current STAFF test user to one ACTIVE hub if they are not assigned yet.
--
-- Safe to run multiple times:
-- - Parent categories use fixed UUIDs with ON CONFLICT.
-- - Leaf categories are inserted only when the same parent/name pair does not exist.
-- - Inventory rows are inserted only when the same hub/item pair does not exist.

BEGIN;

-- 1) Parent categories used by Staff Inventory filters.
WITH seed_parents(id, name, sort_order) AS (
    VALUES
        ('10000000-0000-4000-8000-000000000001'::uuid, 'Nước uống', 10),
        ('10000000-0000-4000-8000-000000000002'::uuid, 'Nhu yếu phẩm khác', 20),
        ('10000000-0000-4000-8000-000000000003'::uuid, 'Quần áo', 30),
        ('10000000-0000-4000-8000-000000000004'::uuid, 'Thuốc', 40),
        ('10000000-0000-4000-8000-000000000005'::uuid, 'Thức ăn', 50)
)
INSERT INTO item_categories (id, parent_id, name, unit, icon_url, is_leaf, sort_order)
SELECT id, NULL, name, 'nhóm', NULL, false, sort_order
FROM seed_parents
ON CONFLICT (id) DO UPDATE SET
    parent_id = NULL,
    name = EXCLUDED.name,
    unit = EXCLUDED.unit,
    icon_url = EXCLUDED.icon_url,
    is_leaf = false,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

-- If an equivalent root category already existed with a different UUID, normalize it as a parent too.
WITH seed_parent_names(name, sort_order) AS (
    VALUES
        ('Nước uống', 10),
        ('Nhu yếu phẩm khác', 20),
        ('Quần áo', 30),
        ('Thuốc', 40),
        ('Thức ăn', 50)
)
UPDATE item_categories parent
SET
    unit = 'nhóm',
    is_leaf = false,
    sort_order = seed_parent_names.sort_order,
    updated_at = NOW()
FROM seed_parent_names
WHERE parent.parent_id IS NULL
  AND LOWER(parent.name) = LOWER(seed_parent_names.name);

-- 2) Leaf item categories. Only these rows are displayed as products in Staff Inventory.
WITH seed_items(parent_name, item_name, unit, icon_url, item_sort_order) AS (
    VALUES
        ('Nước uống', 'Nước suối 500ml', 'chai', NULL, 11),
        ('Nước uống', 'Nước điện giải', 'chai', NULL, 12),
        ('Nước uống', 'Bình nước 20L', 'bình', NULL, 13),

        ('Nhu yếu phẩm khác', 'Pin tiểu', 'vỉ', NULL, 21),
        ('Nhu yếu phẩm khác', 'Đèn pin', 'cái', NULL, 22),
        ('Nhu yếu phẩm khác', 'Khăn ướt', 'gói', NULL, 23),

        ('Quần áo', 'Áo mưa', 'cái', NULL, 31),
        ('Quần áo', 'Áo thun', 'cái', NULL, 32),
        ('Quần áo', 'Chăn mỏng', 'cái', NULL, 33),

        ('Thuốc', 'Thuốc hạ sốt', 'vỉ', NULL, 41),
        ('Thuốc', 'Băng gạc y tế', 'hộp', NULL, 42),
        ('Thuốc', 'Dung dịch sát khuẩn', 'chai', NULL, 43),

        ('Thức ăn', 'Gạo', 'kg', NULL, 51),
        ('Thức ăn', 'Mì gói', 'gói', NULL, 52),
        ('Thức ăn', 'Bánh mì đóng gói', 'gói', NULL, 53),
        ('Thức ăn', 'Sữa hộp', 'hộp', NULL, 54)
)
INSERT INTO item_categories (parent_id, name, unit, icon_url, is_leaf, sort_order)
SELECT
    parent.id,
    seed_items.item_name,
    seed_items.unit,
    seed_items.icon_url,
    true,
    seed_items.item_sort_order
FROM seed_items
JOIN item_categories parent
  ON parent.parent_id IS NULL
 AND LOWER(parent.name) = LOWER(seed_items.parent_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM item_categories child
    WHERE child.parent_id = parent.id
      AND LOWER(child.name) = LOWER(seed_items.item_name)
);

-- Normalize existing matching leaf categories.
WITH seed_items(parent_name, item_name, unit, icon_url, item_sort_order) AS (
    VALUES
        ('Nước uống', 'Nước suối 500ml', 'chai', NULL, 11),
        ('Nước uống', 'Nước điện giải', 'chai', NULL, 12),
        ('Nước uống', 'Bình nước 20L', 'bình', NULL, 13),
        ('Nhu yếu phẩm khác', 'Pin tiểu', 'vỉ', NULL, 21),
        ('Nhu yếu phẩm khác', 'Đèn pin', 'cái', NULL, 22),
        ('Nhu yếu phẩm khác', 'Khăn ướt', 'gói', NULL, 23),
        ('Quần áo', 'Áo mưa', 'cái', NULL, 31),
        ('Quần áo', 'Áo thun', 'cái', NULL, 32),
        ('Quần áo', 'Chăn mỏng', 'cái', NULL, 33),
        ('Thuốc', 'Thuốc hạ sốt', 'vỉ', NULL, 41),
        ('Thuốc', 'Băng gạc y tế', 'hộp', NULL, 42),
        ('Thuốc', 'Dung dịch sát khuẩn', 'chai', NULL, 43),
        ('Thức ăn', 'Gạo', 'kg', NULL, 51),
        ('Thức ăn', 'Mì gói', 'gói', NULL, 52),
        ('Thức ăn', 'Bánh mì đóng gói', 'gói', NULL, 53),
        ('Thức ăn', 'Sữa hộp', 'hộp', NULL, 54)
)
UPDATE item_categories child
SET
    unit = seed_items.unit,
    icon_url = seed_items.icon_url,
    is_leaf = true,
    sort_order = seed_items.item_sort_order,
    updated_at = NOW()
FROM item_categories parent, seed_items
WHERE child.parent_id = parent.id
  AND parent.parent_id IS NULL
  AND LOWER(parent.name) = LOWER(seed_items.parent_name)
  AND LOWER(child.name) = LOWER(seed_items.item_name);

-- 3) Seed inventory for every ACTIVE hub, including a few low-stock rows for the UI badge.
WITH seed_inventory(parent_name, item_name, current_quantity, low_stock_threshold) AS (
    VALUES
        ('Nước uống', 'Nước suối 500ml', 120, 24),
        ('Nước uống', 'Nước điện giải', 18, 20),
        ('Nước uống', 'Bình nước 20L', 32, 8),

        ('Nhu yếu phẩm khác', 'Pin tiểu', 40, 10),
        ('Nhu yếu phẩm khác', 'Đèn pin', 15, 8),
        ('Nhu yếu phẩm khác', 'Khăn ướt', 60, 15),

        ('Quần áo', 'Áo mưa', 25, 12),
        ('Quần áo', 'Áo thun', 80, 20),
        ('Quần áo', 'Chăn mỏng', 12, 15),

        ('Thuốc', 'Thuốc hạ sốt', 50, 20),
        ('Thuốc', 'Băng gạc y tế', 8, 10),
        ('Thuốc', 'Dung dịch sát khuẩn', 30, 12),

        ('Thức ăn', 'Gạo', 120, 30),
        ('Thức ăn', 'Mì gói', 300, 80),
        ('Thức ăn', 'Bánh mì đóng gói', 45, 20),
        ('Thức ăn', 'Sữa hộp', 70, 25)
),
leaf_items AS (
    SELECT
        child.id AS item_category_id,
        seed_inventory.current_quantity,
        seed_inventory.low_stock_threshold
    FROM seed_inventory
    JOIN item_categories parent
      ON parent.parent_id IS NULL
     AND LOWER(parent.name) = LOWER(seed_inventory.parent_name)
    JOIN item_categories child
      ON child.parent_id = parent.id
     AND child.is_leaf = true
     AND LOWER(child.name) = LOWER(seed_inventory.item_name)
),
active_hubs AS (
    SELECT id AS hub_id
    FROM hubs
    WHERE status::text = 'ACTIVE'
)
INSERT INTO hub_inventories (
    hub_id,
    item_category_id,
    current_quantity,
    low_stock_threshold,
    last_restocked_at
)
SELECT
    active_hubs.hub_id,
    leaf_items.item_category_id,
    leaf_items.current_quantity,
    leaf_items.low_stock_threshold,
    NOW()
FROM active_hubs
CROSS JOIN leaf_items
WHERE NOT EXISTS (
    SELECT 1
    FROM hub_inventories existing
    WHERE existing.hub_id = active_hubs.hub_id
      AND existing.item_category_id = leaf_items.item_category_id
);

-- Fill zero existing quantities for the same seeded items without overwriting real inventory counts.
WITH seed_inventory(parent_name, item_name, current_quantity, low_stock_threshold) AS (
    VALUES
        ('Nước uống', 'Nước suối 500ml', 120, 24),
        ('Nước uống', 'Nước điện giải', 18, 20),
        ('Nước uống', 'Bình nước 20L', 32, 8),
        ('Nhu yếu phẩm khác', 'Pin tiểu', 40, 10),
        ('Nhu yếu phẩm khác', 'Đèn pin', 15, 8),
        ('Nhu yếu phẩm khác', 'Khăn ướt', 60, 15),
        ('Quần áo', 'Áo mưa', 25, 12),
        ('Quần áo', 'Áo thun', 80, 20),
        ('Quần áo', 'Chăn mỏng', 12, 15),
        ('Thuốc', 'Thuốc hạ sốt', 50, 20),
        ('Thuốc', 'Băng gạc y tế', 8, 10),
        ('Thuốc', 'Dung dịch sát khuẩn', 30, 12),
        ('Thức ăn', 'Gạo', 120, 30),
        ('Thức ăn', 'Mì gói', 300, 80),
        ('Thức ăn', 'Bánh mì đóng gói', 45, 20),
        ('Thức ăn', 'Sữa hộp', 70, 25)
),
seeded_items AS (
    SELECT
        child.id AS item_category_id,
        seed_inventory.current_quantity,
        seed_inventory.low_stock_threshold
    FROM seed_inventory
    JOIN item_categories parent
      ON parent.parent_id IS NULL
     AND LOWER(parent.name) = LOWER(seed_inventory.parent_name)
    JOIN item_categories child
      ON child.parent_id = parent.id
     AND child.is_leaf = true
     AND LOWER(child.name) = LOWER(seed_inventory.item_name)
)
UPDATE hub_inventories inventory
SET
    current_quantity = CASE
        WHEN inventory.current_quantity = 0 THEN seeded_items.current_quantity
        ELSE inventory.current_quantity
    END,
    low_stock_threshold = seeded_items.low_stock_threshold,
    last_restocked_at = COALESCE(inventory.last_restocked_at, NOW()),
    updated_at = NOW()
FROM seeded_items
WHERE inventory.item_category_id = seeded_items.item_category_id;

-- Optional but useful for existing donation/import flows that rely on accepted categories.
INSERT INTO hub_accepted_categories (hub_id, item_category_id)
SELECT hubs.id, child.id
FROM hubs
JOIN item_categories parent
  ON parent.parent_id IS NULL
 AND parent.name IN ('Nước uống', 'Nhu yếu phẩm khác', 'Quần áo', 'Thuốc', 'Thức ăn')
JOIN item_categories child
  ON child.parent_id = parent.id
 AND child.is_leaf = true
WHERE hubs.status::text = 'ACTIVE'
ON CONFLICT (hub_id, item_category_id) DO NOTHING;

-- 4) Ensure the STAFF account currently used in local logs has an active hub assignment.
DO $$
DECLARE
    target_staff_id uuid;
    target_hub_id uuid;
BEGIN
    SELECT id
    INTO target_staff_id
    FROM users
    WHERE id = '372db56a-c8e5-484c-b35d-73b9236d0f64'::uuid
      AND role::text = 'STAFF'
      AND is_active = true
    LIMIT 1;

    IF target_staff_id IS NULL THEN
        SELECT id
        INTO target_staff_id
        FROM users
        WHERE role::text = 'STAFF'
          AND is_active = true
        ORDER BY created_at ASC
        LIMIT 1;
    END IF;

    IF target_staff_id IS NULL THEN
        RAISE NOTICE 'No active STAFF user found. Inventory data was seeded, but no hub_staff assignment was created.';
        RETURN;
    END IF;

    SELECT hub_id
    INTO target_hub_id
    FROM hub_staff
    WHERE user_id = target_staff_id
      AND is_available = true
      AND unassigned_at IS NULL
    ORDER BY assigned_at DESC
    LIMIT 1;

    IF target_hub_id IS NULL THEN
        SELECT id
        INTO target_hub_id
        FROM hubs
        WHERE status::text = 'ACTIVE'
        ORDER BY created_at ASC
        LIMIT 1;
    END IF;

    IF target_hub_id IS NULL THEN
        RAISE NOTICE 'No ACTIVE hub found. Inventory categories were seeded, but no inventory or hub_staff assignment can be used by STAFF.';
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM hub_staff
        WHERE user_id = target_staff_id
          AND is_available = true
          AND unassigned_at IS NULL
    ) THEN
        INSERT INTO hub_staff (hub_id, user_id, is_available, assigned_at, unassigned_at)
        VALUES (target_hub_id, target_staff_id, true, NOW(), NULL);

        RAISE NOTICE 'Assigned STAFF % to hub % for inventory testing.', target_staff_id, target_hub_id;
    ELSE
        RAISE NOTICE 'STAFF % already has an active hub assignment.', target_staff_id;
    END IF;
END $$;

COMMIT;

-- Quick verification:
-- SELECT parent.name AS parent_category,
--        child.name AS item_name,
--        inventory.current_quantity,
--        inventory.low_stock_threshold
-- FROM hub_inventories inventory
-- JOIN item_categories child ON child.id = inventory.item_category_id
-- LEFT JOIN item_categories parent ON parent.id = child.parent_id
-- JOIN hub_staff assignment ON assignment.hub_id = inventory.hub_id
-- WHERE assignment.user_id = '372db56a-c8e5-484c-b35d-73b9236d0f64'::uuid
--   AND assignment.is_available = true
--   AND assignment.unassigned_at IS NULL
--   AND child.is_leaf = true
-- ORDER BY parent.sort_order, child.sort_order, child.name;
