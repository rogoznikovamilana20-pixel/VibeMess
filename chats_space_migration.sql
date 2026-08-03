-- Chats table with space column for Personal/Work focus mode
-- Run this in Supabase SQL Editor

-- Create chats table if it doesn't exist
CREATE TABLE IF NOT EXISTS chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL DEFAULT '',
    is_group BOOLEAN DEFAULT false,
    last_message TEXT DEFAULT '',
    last_message_at TIMESTAMPTZ DEFAULT now(),
    space TEXT DEFAULT 'personal',
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Add space column if table exists but column doesn't
ALTER TABLE chats ADD COLUMN IF NOT EXISTS space TEXT DEFAULT 'personal';

-- Index for fast space filtering
CREATE INDEX IF NOT EXISTS idx_chats_space ON chats(space);

-- Index for last_message_at ordering
CREATE INDEX IF NOT EXISTS idx_chats_last_message_at ON chats(last_message_at DESC);

-- Enable RLS
ALTER TABLE chats ENABLE ROW LEVEL SECURITY;

-- Chat members table
CREATE TABLE IF NOT EXISTS chat_members (
    id BIGSERIAL PRIMARY KEY,
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(chat_id, user_id)
);

ALTER TABLE chat_members ENABLE ROW LEVEL SECURITY;

-- RLS: authenticated users can read chats they are members of
DROP POLICY IF EXISTS "Members can read chats" ON chats;
CREATE POLICY "Members can read chats" ON chats
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM chat_members
            WHERE chat_members.chat_id = chats.id
            AND chat_members.user_id = auth.uid()
        )
    );

-- RLS: authenticated users can create chats
DROP POLICY IF EXISTS "Authenticated can create chats" ON chats;
CREATE POLICY "Authenticated can create chats" ON chats
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- RLS: chat creator can update
DROP POLICY IF EXISTS "Creator can update chat" ON chats;
CREATE POLICY "Creator can update chat" ON chats
    FOR UPDATE USING (created_by = auth.uid());

-- RLS: chat members can read membership
DROP POLICY IF EXISTS "Members can read members" ON chat_members;
CREATE POLICY "Members can read members" ON chat_members
    FOR SELECT USING (
        user_id = auth.uid() OR
        EXISTS (
            SELECT 1 FROM chat_members cm
            WHERE cm.chat_id = chat_members.chat_id AND cm.user_id = auth.uid()
        )
    );

-- RLS: authenticated can add members
DROP POLICY IF EXISTS "Authenticated can add members" ON chat_members;
CREATE POLICY "Authenticated can add members" ON chat_members
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');
