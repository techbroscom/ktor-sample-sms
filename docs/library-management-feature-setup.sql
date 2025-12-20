-- Library Management Feature Setup
-- This script adds the library_management feature to the Features catalog in the public schema

-- Insert library_management feature into the Features table
INSERT INTO features (feature_key, name, description, category, is_active, default_enabled, has_limit, limit_type, limit_value, limit_unit)
VALUES (
    'library_management',
    'Library Management',
    'Complete library management system with book catalog, borrowing/return tracking, reservations, and fine management. Includes ISBN tracking, multi-copy support, renewal management, and automated overdue fine calculation.',
    'operations',
    true,
    false,
    true,
    'total_books',
    10000,
    'books'
)
ON CONFLICT (feature_key) DO NOTHING;

-- Enable the feature for all existing tenants (optional - uncomment if you want to auto-enable)
-- INSERT INTO tenant_features (tenant_id, feature_id, is_enabled, enabled_at)
-- SELECT
--     tc.tenant_id,
--     f.id,
--     true,
--     NOW()
-- FROM tenant_config tc
-- CROSS JOIN features f
-- WHERE f.feature_key = 'library_management'
--   AND NOT EXISTS (
--     SELECT 1 FROM tenant_features tf
--     WHERE tf.tenant_id = tc.tenant_id
--       AND tf.feature_id = f.id
--   );

-- Verify the feature was added
SELECT id, feature_key, name, description, is_active, has_limit, limit_value, limit_unit
FROM features
WHERE feature_key = 'library_management';

-- Example: Enable for a specific tenant
-- Replace {tenant_id} with actual tenant ID
-- INSERT INTO tenant_features (tenant_id, feature_id, is_enabled, custom_limit_value, enabled_at)
-- SELECT
--     '{tenant_id}',
--     f.id,
--     true,
--     5000,  -- Custom limit (5000 books for this tenant)
--     NOW()
-- FROM features f
-- WHERE f.feature_key = 'library_management';
