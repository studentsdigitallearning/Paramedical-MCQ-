-- ==============================================================================
-- Supabase Schema Migration: Complete MCQ Management System Backend
-- Version: 20261004_initial_schema.sql
-- ==============================================================================

-- 1. Classes / Courses Table
CREATE TABLE IF NOT EXISTS public.classes (
    id text PRIMARY KEY,
    name text NOT NULL,
    subject_count int NOT NULL DEFAULT 5,
    display_order int NOT NULL DEFAULT 1,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 2. Categories Table
CREATE TABLE IF NOT EXISTS public.categories (
    id text PRIMARY KEY,
    name text NOT NULL,
    type text NOT NULL, -- 'SUBJECT', 'MOCK', 'PREVIOUS', 'DAILY'
    icon_key text NOT NULL DEFAULT 'category',
    subtitle text NOT NULL DEFAULT '',
    test_count int NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 3. Subjects Table
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

-- 4. Chapters Table
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

-- 5. Tests Table
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
    difficulty text NOT NULL DEFAULT 'MEDIUM', -- 'EASY', 'MEDIUM', 'HARD'
    status text NOT NULL DEFAULT 'NOT_ATTEMPTED',
    is_daily boolean NOT NULL DEFAULT false,
    expiry_note text,
    is_published boolean NOT NULL DEFAULT true,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 6. Questions Table
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

-- 7. Shared Leaderboard & Test Results
CREATE TABLE IF NOT EXISTS public.leaderboard (
    id serial PRIMARY KEY,
    student_id text NOT NULL,
    student_name text NOT NULL,
    class_name text NOT NULL,
    date_key text NOT NULL,
    score real NOT NULL,
    total_marks real NOT NULL DEFAULT 50.0,
    completion_time_seconds bigint NOT NULL DEFAULT 300,
    attempts_count int NOT NULL DEFAULT 1,
    submitted_at bigint NOT NULL DEFAULT 0,
    created_at timestamptz DEFAULT now()
);

-- Indexes for lightning fast queries
CREATE INDEX IF NOT EXISTS idx_subjects_class_id ON public.subjects(class_id);
CREATE INDEX IF NOT EXISTS idx_chapters_subject_id ON public.chapters(subject_id);
CREATE INDEX IF NOT EXISTS idx_tests_class_category ON public.tests(class_id, category_id);
CREATE INDEX IF NOT EXISTS idx_tests_is_daily ON public.tests(is_daily);
CREATE INDEX IF NOT EXISTS idx_questions_test_id ON public.questions(test_id);
CREATE INDEX IF NOT EXISTS idx_leaderboard_date_score ON public.leaderboard(date_key, score DESC, completion_time_seconds ASC);

-- Row Level Security (RLS) setup
ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chapters ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leaderboard ENABLE ROW LEVEL SECURITY;

-- Security Policies: Students can read published/active content
CREATE POLICY "Public Read Classes" ON public.classes FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Public Read Categories" ON public.categories FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Public Read Subjects" ON public.subjects FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Public Read Chapters" ON public.chapters FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Public Read Tests" ON public.tests FOR SELECT TO anon, authenticated USING (is_published = true AND is_active = true);
CREATE POLICY "Public Read Questions" ON public.questions FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Leaderboard" ON public.leaderboard FOR SELECT TO anon, authenticated USING (true);

-- Allow students to insert their test results to update the leaderboard
CREATE POLICY "Public Insert Leaderboard" ON public.leaderboard FOR INSERT TO anon, authenticated WITH CHECK (score >= 0 AND total_marks > 0);

-- Grants
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;

NOTIFY pgrst, 'reload schema';
