-- ==============================================================================
-- Supabase Schema: Content-Only Schema (No User Registration / No Users Table)
-- Version: 20261011_admin_user_sync_schema.sql
-- ==============================================================================

-- 1. Permanently remove any legacy user / student tables (Zero login / registration app)
DROP TABLE IF EXISTS public.students CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;

-- 2. Classes Table
CREATE TABLE IF NOT EXISTS public.classes (
    id text PRIMARY KEY,
    name text NOT NULL,
    subject_count int NOT NULL DEFAULT 5,
    display_order int NOT NULL DEFAULT 1,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 3. Categories Table
CREATE TABLE IF NOT EXISTS public.categories (
    id text PRIMARY KEY,
    name text NOT NULL,
    type text NOT NULL, -- SUBJECT, MOCK, PREVIOUS, DAILY
    icon_key text NOT NULL DEFAULT 'category',
    subtitle text NOT NULL DEFAULT '',
    test_count int NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 4. Subjects Table
CREATE TABLE IF NOT EXISTS public.subjects (
    id text PRIMARY KEY,
    class_id text NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    name text NOT NULL,
    icon_key text NOT NULL DEFAULT 'subject',
    chapter_count int NOT NULL DEFAULT 6,
    progress_percent int NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 5. Chapters Table
CREATE TABLE IF NOT EXISTS public.chapters (
    id text PRIMARY KEY,
    subject_id text NOT NULL REFERENCES public.subjects(id) ON DELETE CASCADE,
    chapter_number int NOT NULL,
    name text NOT NULL,
    test_count int NOT NULL DEFAULT 3,
    progress_percent int NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 6. Tests Table
CREATE TABLE IF NOT EXISTS public.tests (
    id text PRIMARY KEY,
    class_id text NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    category_id text NOT NULL,
    subject_id text,
    subject_name text,
    chapter_id text,
    chapter_name text,
    title text NOT NULL,
    subtitle text NOT NULL DEFAULT '',
    question_count int NOT NULL DEFAULT 20,
    duration_minutes int NOT NULL DEFAULT 30,
    total_marks real NOT NULL DEFAULT 50.0,
    negative_marking real NOT NULL DEFAULT 0.25,
    difficulty text NOT NULL DEFAULT 'MEDIUM',
    status text NOT NULL DEFAULT 'NOT_ATTEMPTED',
    is_daily boolean NOT NULL DEFAULT false,
    expiry_note text,
    is_published boolean NOT NULL DEFAULT true,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 7. Questions Table
CREATE TABLE IF NOT EXISTS public.questions (
    id text PRIMARY KEY,
    test_id text NOT NULL REFERENCES public.tests(id) ON DELETE CASCADE,
    question_index int NOT NULL,
    text text NOT NULL,
    options jsonb NOT NULL,
    correct_index int NOT NULL DEFAULT 0,
    explanation text NOT NULL DEFAULT '',
    chapter_name text NOT NULL DEFAULT '',
    marks real NOT NULL DEFAULT 1.0,
    text_hindi text,
    options_hindi jsonb,
    explanation_hindi text,
    created_at timestamptz DEFAULT now()
);

-- 8. Leaderboard Table (Records test scores and rank for practice tests)
CREATE TABLE IF NOT EXISTS public.leaderboard (
    id serial PRIMARY KEY,
    student_id text NOT NULL,
    student_name text NOT NULL DEFAULT 'Student',
    roll_no text,
    course_name text,
    class_name text NOT NULL,
    date_key text NOT NULL,
    score real NOT NULL,
    total_marks real NOT NULL DEFAULT 50.0,
    completion_time_seconds bigint NOT NULL DEFAULT 300,
    attempts_count int NOT NULL DEFAULT 1,
    submitted_at bigint NOT NULL DEFAULT 0,
    created_at timestamptz DEFAULT now()
);

-- 9. Daily Participants Table
CREATE TABLE IF NOT EXISTS public.daily_participants (
    id serial PRIMARY KEY,
    student_id text NOT NULL,
    student_name text NOT NULL DEFAULT 'Student',
    course_name text NOT NULL DEFAULT '',
    class_name text NOT NULL DEFAULT 'Class 10',
    roll_no text NOT NULL,
    date_key text NOT NULL,
    score real DEFAULT 0.0,
    total_marks real DEFAULT 50.0,
    has_attempted boolean DEFAULT false,
    registered_at timestamptz DEFAULT now(),
    CONSTRAINT uq_daily_participants_student_date UNIQUE (student_id, date_key),
    CONSTRAINT uq_daily_participants_roll_date UNIQUE (roll_no, date_key)
);

-- 10. Dynamic App Config Table
CREATE TABLE IF NOT EXISTS public.app_config (
    key text PRIMARY KEY,
    value text NOT NULL,
    description text,
    updated_at timestamptz DEFAULT now()
);

-- Row Level Security (RLS) Configuration
ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chapters ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leaderboard ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;

-- Policies for public (anon and authenticated) access
DO $$
BEGIN
    DROP POLICY IF EXISTS "Public Read Classes" ON public.classes;
    DROP POLICY IF EXISTS "Public Read Categories" ON public.categories;
    DROP POLICY IF EXISTS "Public Read Subjects" ON public.subjects;
    DROP POLICY IF EXISTS "Public Read Chapters" ON public.chapters;
    DROP POLICY IF EXISTS "Public Read Tests" ON public.tests;
    DROP POLICY IF EXISTS "Public Read Questions" ON public.questions;
    DROP POLICY IF EXISTS "Public Read Leaderboard" ON public.leaderboard;
    DROP POLICY IF EXISTS "Public Insert Leaderboard" ON public.leaderboard;
    DROP POLICY IF EXISTS "Public Read Daily Participants" ON public.daily_participants;
    DROP POLICY IF EXISTS "Public Insert Daily Participants" ON public.daily_participants;
    DROP POLICY IF EXISTS "Public Update Daily Participants" ON public.daily_participants;
    DROP POLICY IF EXISTS "Public Read App Config" ON public.app_config;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

CREATE POLICY "Public Read Classes" ON public.classes FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Categories" ON public.categories FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Subjects" ON public.subjects FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Chapters" ON public.chapters FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Tests" ON public.tests FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Questions" ON public.questions FOR SELECT TO anon, authenticated USING (true);

CREATE POLICY "Public Read Leaderboard" ON public.leaderboard FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Insert Leaderboard" ON public.leaderboard FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Public Delete Leaderboard" ON public.leaderboard FOR DELETE TO anon, authenticated USING (true);

CREATE POLICY "Public Read Daily Participants" ON public.daily_participants FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Insert Daily Participants" ON public.daily_participants FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Public Update Daily Participants" ON public.daily_participants FOR UPDATE TO anon, authenticated USING (true);
CREATE POLICY "Public Delete Daily Participants" ON public.daily_participants FOR DELETE TO anon, authenticated USING (true);

CREATE POLICY "Public Read App Config" ON public.app_config FOR SELECT TO anon, authenticated USING (true);

-- Stored Procedure: Reset & permanently delete previous days at 12:00 AM midnight IST
CREATE OR REPLACE FUNCTION public.reset_daily_participants(
    p_current_date_key text
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    DELETE FROM public.daily_participants WHERE date_key < p_current_date_key;
    DELETE FROM public.leaderboard WHERE date_key < p_current_date_key;
    DELETE FROM public.daily_participants WHERE has_attempted = false;
    RETURN true;
END;
$$;

CREATE OR REPLACE FUNCTION public.cleanup_midnight_daily_leaderboard(
    p_current_date text DEFAULT NULL,
    p_current_date_key text DEFAULT NULL
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_date text := COALESCE(p_current_date_key, p_current_date, to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD'));
BEGIN
    RETURN public.reset_daily_participants(v_date);
END;
$$;

GRANT EXECUTE ON FUNCTION public.reset_daily_participants TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.cleanup_midnight_daily_leaderboard TO anon, authenticated, service_role;

-- Grant privileges to standard roles
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;

NOTIFY pgrst, 'reload schema';
