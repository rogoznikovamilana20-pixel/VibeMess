-- E2E encryption keys storage
-- Run this in Supabase SQL Editor

-- Add E2E keys column to profiles table
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS e2e_keys JSONB;

-- Create index for faster key lookups
CREATE INDEX IF NOT EXISTS idx_profiles_e2e_keys ON profiles USING gin(e2e_keys);

-- RLS: Users can read E2E keys of anyone (needed for key exchange)
-- but can only update their own keys
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- Allow reading E2E keys from any authenticated user
DROP POLICY IF EXISTS "E2E keys readable by all" ON profiles;
CREATE POLICY "E2E keys readable by all" ON profiles
    FOR SELECT
    USING (auth.role() = 'authenticated');

-- Allow users to update only their own E2E keys
DROP POLICY IF EXISTS "Users update own E2E keys" ON profiles;
CREATE POLICY "Users update own E2E keys" ON profiles
    FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Allow users to insert their own profile with E2E keys
DROP POLICY IF EXISTS "Users insert own E2E keys" ON profiles;
CREATE POLICY "Users insert own E2E keys" ON profiles
    FOR INSERT
    WITH CHECK (auth.uid() = id);
