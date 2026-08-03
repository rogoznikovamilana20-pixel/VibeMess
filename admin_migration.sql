-- Add role column to profiles table
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'user';

-- Set sueturia as admin
UPDATE profiles SET role = 'admin' WHERE id IN (
    SELECT id FROM auth.users WHERE raw_user_meta_data->>'username' = 'sueturia'
    OR raw_user_meta_data->>'email' LIKE '%sueturia%'
);

-- Also set by display_name as fallback
UPDATE profiles SET role = 'admin' WHERE display_name = 'sueturia';

-- Create admin check function
CREATE OR REPLACE FUNCTION is_admin(user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM profiles
        WHERE id = user_id AND role = 'admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- RLS: admins can read all profiles
CREATE POLICY IF NOT EXISTS "Admins can read all profiles" ON profiles
    FOR SELECT USING (
        role = 'admin' OR auth.uid() = id
    );

-- RLS: admins can update any profile
CREATE POLICY IF NOT EXISTS "Admins can update any profile" ON profiles
    FOR UPDATE USING (
        role = 'admin' OR auth.uid() = id
    ) WITH CHECK (
        role = 'admin' OR auth.uid() = id
    );
