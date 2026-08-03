-- Create profiles table (if not exists)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT DEFAULT '',
    avatar_url TEXT,
    username TEXT,
    is_online BOOLEAN DEFAULT false,
    last_seen TIMESTAMPTZ,
    role TEXT DEFAULT 'user',
    e2e_keys JSONB,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_profiles_username ON profiles(username);
CREATE INDEX IF NOT EXISTS idx_profiles_e2e_keys ON profiles USING gin(e2e_keys);

-- Enable RLS
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- Policies
CREATE POLICY "Profiles readable by authenticated" ON profiles
    FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Users insert own profile" ON profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY "Users update own profile" ON profiles
    FOR UPDATE USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

-- Auto-create profile on signup (optional trigger)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, display_name, role)
    VALUES (NEW.id, COALESCE(NEW.raw_user_meta_data->>'display_name', ''), 'user')
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Set sueturia as super_admin (creator)
UPDATE profiles SET role = 'super_admin' WHERE display_name = 'sueturia';
