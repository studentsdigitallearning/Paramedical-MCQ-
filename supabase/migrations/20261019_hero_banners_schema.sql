-- ==============================================================================
-- Supabase Schema Migration: Home Hero Section Promotional Banners
-- Version: 20261019_hero_banners_schema.sql
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.promotional_banners (
    id text PRIMARY KEY,
    class_id text DEFAULT 'all',
    is_daily_challenge boolean NOT NULL DEFAULT false,
    badge_text text NOT NULL DEFAULT 'FEATURED',
    meta_text text NOT NULL DEFAULT '',
    title text NOT NULL,
    description text NOT NULL DEFAULT '',
    button_text text DEFAULT 'Explore',
    action_type text NOT NULL DEFAULT 'NONE', -- OPEN_DAILY_TEST, OPEN_TEST, OPEN_INTERNAL_PAGE, OPEN_WEBSITE, OPEN_VIDEO
    target_id text,
    url text,
    display_order int NOT NULL DEFAULT 1,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

-- Index for ordering and filtering
CREATE INDEX IF NOT EXISTS idx_promotional_banners_order ON public.promotional_banners(display_order ASC, is_active);

-- Enable Row Level Security
ALTER TABLE public.promotional_banners ENABLE ROW LEVEL SECURITY;

-- Drop previous policies if any
DO $$
BEGIN
    DROP POLICY IF EXISTS "Public Read Promotional Banners" ON public.promotional_banners;
    DROP POLICY IF EXISTS "Public Manage Promotional Banners" ON public.promotional_banners;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- Policies: Anyone can view active banners
CREATE POLICY "Public Read Promotional Banners" ON public.promotional_banners
FOR SELECT TO anon, authenticated
USING (is_active = true);

-- Allow admins/anon to insert, update, delete banners
CREATE POLICY "Public Manage Promotional Banners" ON public.promotional_banners
FOR ALL TO anon, authenticated, service_role
USING (true)
WITH CHECK (true);

-- Enable Supabase Realtime for instant updates in app when edited in dashboard
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.promotional_banners;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

GRANT ALL ON TABLE public.promotional_banners TO anon, authenticated, service_role;

-- Seed initial 3 banner slides for the app
INSERT INTO public.promotional_banners (
    id, class_id, is_daily_challenge, badge_text, meta_text, title, description, button_text, action_type, target_id, url, display_order, is_active
) VALUES
(
    'banner_daily_mcq', 'all', true, 'TODAY''S CHALLENGE', '20 Questions • 30 Minutes',
    'Daily MCQ Challenge', 'Compete with all-India JAC board students in today''s live test',
    'Start Test', 'OPEN_DAILY_TEST', 'daily_test', NULL, 1, true
),
(
    'banner_mock_tests', 'all', false, 'BOARD EXAM PREP', 'Full Syllabus • Timed Exam',
    'All-India Mock Test Series', 'Practice high-scoring timed mock exams designed for JAC Board pattern',
    'Explore Mock Tests', 'OPEN_INTERNAL_PAGE', 'mock_tests', NULL, 2, true
),
(
    'banner_subject_wise', 'all', false, 'CHAPTER PRACTICE', 'All Subjects • Voice Support',
    'Subject-Wise Chapter Mastery', 'Master Science, Math & more with detailed explanations and voice reading',
    'Start Practice', 'OPEN_INTERNAL_PAGE', 'subject_wise', NULL, 3, true
)
ON CONFLICT (id) DO UPDATE SET
    badge_text = EXCLUDED.badge_text,
    meta_text = EXCLUDED.meta_text,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    button_text = EXCLUDED.button_text,
    action_type = EXCLUDED.action_type,
    target_id = EXCLUDED.target_id,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active;

NOTIFY pgrst, 'reload schema';
