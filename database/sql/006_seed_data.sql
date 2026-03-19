-- ============================================================================
-- AidBridge Database Schema v3.0
-- File: 006_seed_data.sql
-- Description: Initial seed data (item categories, system config, admin user)
-- ============================================================================
-- ============================================================================
-- SYSTEM CONFIGURATION
-- ============================================================================
INSERT INTO system_config (key, value, description)
VALUES (
        'dispatch_radius_initial_km',
        '5',
        'Initial radius (km) for volunteer dispatch'
    ),
    (
        'dispatch_radius_max_km',
        '20',
        'Maximum radius (km) for volunteer dispatch'
    ),
    (
        'dispatch_timeout_seconds',
        '120',
        'Timeout (seconds) for volunteer to respond'
    ),
    (
        'dispatch_batch_size',
        '5',
        'Number of volunteers per dispatch batch'
    ),
    (
        'mission_auto_cancel_hours',
        '24',
        'Hours before unassigned mission auto-cancels'
    ),
    (
        'otp_expiry_minutes',
        '5',
        'OTP expiration time in minutes'
    ),
    (
        'jwt_access_token_hours',
        '1',
        'JWT access token expiration in hours'
    ),
    (
        'jwt_refresh_token_days',
        '30',
        'JWT refresh token expiration in days'
    ),
    (
        'donation_points_per_item',
        '10',
        'Points awarded per donated item'
    ),
    (
        'badge_silver_threshold',
        '1000',
        'Points required for Silver badge'
    ),
    (
        'badge_gold_threshold',
        '5000',
        'Points required for Gold badge'
    ),
    (
        'badge_platinum_threshold',
        '10000',
        'Points required for Platinum badge'
    );
-- ============================================================================
-- ITEM CATEGORIES (Hierarchical)
-- ============================================================================
-- Root categories
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0000-000000000001',
        NULL,
        'Food & Water',
        'Thực phẩm & Nước',
        'unit',
        FALSE
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        NULL,
        'Medical Supplies',
        'Vật tư y tế',
        'unit',
        FALSE
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        NULL,
        'Clothing',
        'Quần áo',
        'piece',
        FALSE
    ),
    (
        '00000000-0000-0000-0000-000000000004',
        NULL,
        'Shelter Supplies',
        'Vật dụng trú ẩn',
        'unit',
        FALSE
    ),
    (
        '00000000-0000-0000-0000-000000000005',
        NULL,
        'Hygiene',
        'Vệ sinh',
        'unit',
        FALSE
    );
-- Food & Water subcategories
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0001-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'Drinking Water',
        'Nước uống',
        'liter',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000002',
        '00000000-0000-0000-0000-000000000001',
        'Rice',
        'Gạo',
        'kg',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000003',
        '00000000-0000-0000-0000-000000000001',
        'Instant Noodles',
        'Mì gói',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000004',
        '00000000-0000-0000-0000-000000000001',
        'Canned Food',
        'Đồ hộp',
        'can',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000005',
        '00000000-0000-0000-0000-000000000001',
        'Baby Formula',
        'Sữa bột trẻ em',
        'box',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000006',
        '00000000-0000-0000-0000-000000000001',
        'Cooking Oil',
        'Dầu ăn',
        'liter',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000007',
        '00000000-0000-0000-0000-000000000001',
        'Salt/Sugar',
        'Muối/Đường',
        'kg',
        TRUE
    ),
    (
        '00000000-0000-0000-0001-000000000008',
        '00000000-0000-0000-0000-000000000001',
        'Biscuits',
        'Bánh quy',
        'pack',
        TRUE
    );
-- Medical Supplies subcategories
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0002-000000000001',
        '00000000-0000-0000-0000-000000000002',
        'First Aid Kit',
        'Bộ sơ cứu',
        'kit',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000002',
        '00000000-0000-0000-0000-000000000002',
        'Pain Relievers',
        'Thuốc giảm đau',
        'box',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000003',
        '00000000-0000-0000-0000-000000000002',
        'Bandages',
        'Băng gạc',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000004',
        '00000000-0000-0000-0000-000000000002',
        'Antiseptic',
        'Thuốc sát trùng',
        'bottle',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000005',
        '00000000-0000-0000-0000-000000000002',
        'Oral Rehydration',
        'Oresol',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0002-000000000006',
        '00000000-0000-0000-0000-000000000002',
        'Face Masks',
        'Khẩu trang',
        'pack',
        TRUE
    );
-- Clothing subcategories
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0003-000000000001',
        '00000000-0000-0000-0000-000000000003',
        'T-Shirts',
        'Áo thun',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000002',
        '00000000-0000-0000-0000-000000000003',
        'Pants',
        'Quần',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000003',
        '00000000-0000-0000-0000-000000000003',
        'Underwear',
        'Đồ lót',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000004',
        '00000000-0000-0000-0000-000000000003',
        'Raincoats',
        'Áo mưa',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000005',
        '00000000-0000-0000-0000-000000000003',
        'Blankets',
        'Chăn',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0003-000000000006',
        '00000000-0000-0000-0000-000000000003',
        'Baby Clothes',
        'Quần áo trẻ em',
        'piece',
        TRUE
    );
-- Shelter Supplies subcategories
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0004-000000000001',
        '00000000-0000-0000-0000-000000000004',
        'Tarpaulin',
        'Bạt che',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000002',
        '00000000-0000-0000-0000-000000000004',
        'Sleeping Mats',
        'Chiếu',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000003',
        '00000000-0000-0000-0000-000000000004',
        'Flashlights',
        'Đèn pin',
        'piece',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000004',
        '00000000-0000-0000-0000-000000000004',
        'Batteries',
        'Pin',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000005',
        '00000000-0000-0000-0000-000000000004',
        'Ropes',
        'Dây thừng',
        'meter',
        TRUE
    ),
    (
        '00000000-0000-0000-0004-000000000006',
        '00000000-0000-0000-0000-000000000004',
        'Candles',
        'Nến',
        'pack',
        TRUE
    );
-- Hygiene subcategories
INSERT INTO item_categories (id, parent_id, name, name_vi, unit, is_leaf)
VALUES (
        '00000000-0000-0000-0005-000000000001',
        '00000000-0000-0000-0000-000000000005',
        'Soap',
        'Xà phòng',
        'bar',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000002',
        '00000000-0000-0000-0000-000000000005',
        'Toothbrush/Toothpaste',
        'Bàn chải/Kem đánh răng',
        'set',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000003',
        '00000000-0000-0000-0000-000000000005',
        'Sanitary Pads',
        'Băng vệ sinh',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000004',
        '00000000-0000-0000-0000-000000000005',
        'Diapers',
        'Tã/Bỉm',
        'pack',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000005',
        '00000000-0000-0000-0000-000000000005',
        'Toilet Paper',
        'Giấy vệ sinh',
        'roll',
        TRUE
    ),
    (
        '00000000-0000-0000-0005-000000000006',
        '00000000-0000-0000-0000-000000000005',
        'Hand Sanitizer',
        'Nước rửa tay',
        'bottle',
        TRUE
    );
-- ============================================================================
-- DEFAULT ADMIN USER (password: Admin@123 - should be changed immediately)
-- ============================================================================
INSERT INTO users (
        id,
        full_name,
        email,
        phone_number,
        password_hash,
        role,
        is_verified,
        is_active
    )
VALUES (
        '00000000-0000-0000-0000-000000000000',
        'System Administrator',
        'admin@aidbridge.vn',
        '0900000000',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4lOQQZ9ZP.EXAMPLE',
        'ADMIN',
        TRUE,
        TRUE
    );
-- Note: The password hash above is a placeholder. Generate a real bcrypt hash for production.