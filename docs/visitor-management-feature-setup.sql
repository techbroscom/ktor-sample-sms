-- Visitor Management Feature Setup
-- This script adds the visitor_management feature to the Features catalog in the public schema

-- Insert visitor_management feature into the Features table
INSERT INTO features (feature_key, name, description, category, is_active, default_enabled, has_limit, limit_type, limit_value, limit_unit)
VALUES (
    'visitor_management',
    'Visitor Management',
    'Track and manage visitors, check-ins, and check-outs with host notifications. Includes visitor registration, check-in/out tracking, host assignment, and visitor pass management.',
    'operations',
    true,
    false,
    true,
    'monthly_visitors',
    1000,
    'visitors'
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
-- WHERE f.feature_key = 'visitor_management'
--   AND NOT EXISTS (
--     SELECT 1 FROM tenant_features tf
--     WHERE tf.tenant_id = tc.tenant_id
--       AND tf.feature_id = f.id
--   );

-- Verify the feature was added
SELECT id, feature_key, name, description, is_active, has_limit, limit_value
FROM features
WHERE feature_key = 'visitor_management';
