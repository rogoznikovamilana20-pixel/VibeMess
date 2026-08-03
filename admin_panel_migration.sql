-- Admin panel migration: ban system, admin logs, role management

-- Add ban columns to profiles
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS is_banned BOOLEAN DEFAULT false;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS banned_at TIMESTAMPTZ;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS banned_by UUID REFERENCES auth.users(id);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS ban_reason TEXT;

-- Admin action log table
CREATE TABLE IF NOT EXISTS admin_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_id UUID REFERENCES auth.users(id),
    action TEXT NOT NULL,
    target_id UUID,
    target_name TEXT,
    details JSONB,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE admin_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Admins can read logs" ON admin_logs;
CREATE POLICY "Admins can read logs" ON admin_logs
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('admin', 'super_admin'))
    );

DROP POLICY IF EXISTS "Admins can insert logs" ON admin_logs;
CREATE POLICY "Admins can insert logs" ON admin_logs
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('admin', 'super_admin'))
    );

-- Updated RLS: super_admin can update any profile
DROP POLICY IF EXISTS "Users update own profile" ON profiles;
CREATE POLICY "Users update own profile" ON profiles
    FOR UPDATE USING (
        auth.uid() = id OR
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'super_admin')
    ) WITH CHECK (
        auth.uid() = id OR
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'super_admin')
    );

-- Admins (not just super_admin) can ban/unban users
DROP POLICY IF EXISTS "Admins can update any profile" ON profiles;
CREATE POLICY "Admins can update any profile" ON profiles
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('admin', 'super_admin'))
    ) WITH CHECK (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('admin', 'super_admin'))
    );

-- Function to log admin actions
CREATE OR REPLACE FUNCTION log_admin_action(
    p_action TEXT,
    p_target_id UUID DEFAULT NULL,
    p_target_name TEXT DEFAULT NULL,
    p_details JSONB DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO admin_logs (admin_id, action, target_id, target_name, details)
    VALUES (auth.uid(), p_action, p_target_id, p_target_name, p_details);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Set sueturia as super_admin
UPDATE profiles SET role = 'super_admin' WHERE display_name = 'sueturia';
