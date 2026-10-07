import os
import urllib.request
import json
import sys

PROJECT_REF = os.environ.get("SUPABASE_PROJECT_REF", sys.argv[1] if len(sys.argv) > 1 else "")
ACCESS_TOKEN = os.environ.get("SUPABASE_ACCESS_TOKEN", sys.argv[2] if len(sys.argv) > 2 else "")

if not PROJECT_REF or not ACCESS_TOKEN:
    print("Usage: python3 supabase_setup.py <SUPABASE_PROJECT_REF> <SUPABASE_ACCESS_TOKEN>")
    print("Or set SUPABASE_PROJECT_REF and SUPABASE_ACCESS_TOKEN environment variables.")
    print("Alternatively, you can copy supabase/migrations/20261004_initial_schema.sql into Supabase SQL Editor.")
    sys.exit(1)

API_URL = f"https://api.supabase.com/v1/projects/{PROJECT_REF}/database/query"


def execute_sql(sql):
    req = urllib.request.Request(
        API_URL,
        data=json.dumps({"query": sql}).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {ACCESS_TOKEN}",
            "Content-Type": "application/json"
        },
        method="POST"
    )
    try:
        with urllib.request.urlopen(req) as resp:
            data = resp.read().decode("utf-8")
            return json.loads(data)
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8")
        print(f"HTTP Error {e.code}: {err}", file=sys.stderr)
        raise

# Step 1: Create Tables
create_tables_sql = """
-- Drop existing tables to have a clean, perfectly structured schema
DROP TABLE IF EXISTS public.questions CASCADE;
DROP TABLE IF EXISTS public.tests CASCADE;
DROP TABLE IF EXISTS public.chapters CASCADE;
DROP TABLE IF EXISTS public.subjects CASCADE;
DROP TABLE IF EXISTS public.categories CASCADE;
DROP TABLE IF EXISTS public.classes CASCADE;
DROP TABLE IF EXISTS public.courses CASCADE;
DROP TABLE IF EXISTS public.leaderboard CASCADE;

-- 1. Classes Table
CREATE TABLE public.classes (
    id text PRIMARY KEY,
    name text NOT NULL,
    subject_count int NOT NULL DEFAULT 5,
    display_order int NOT NULL DEFAULT 1,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 2. Categories Table
CREATE TABLE public.categories (
    id text PRIMARY KEY,
    name text NOT NULL,
    type text NOT NULL, -- SUBJECT, MOCK, PREVIOUS, DAILY
    icon_key text NOT NULL DEFAULT 'category',
    subtitle text NOT NULL DEFAULT '',
    test_count int NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT now()
);

-- 3. Subjects Table
CREATE TABLE public.subjects (
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
CREATE TABLE public.chapters (
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
CREATE TABLE public.tests (
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

-- 6. Questions Table
CREATE TABLE public.questions (
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

-- 7. Leaderboard Table
CREATE TABLE public.leaderboard (
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

-- Row Level Security (RLS) setup
ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chapters ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leaderboard ENABLE ROW LEVEL SECURITY;

-- Allow public read (anon + authenticated)
CREATE POLICY "Public Read Classes" ON public.classes FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Categories" ON public.categories FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Subjects" ON public.subjects FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Chapters" ON public.chapters FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Tests" ON public.tests FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Questions" ON public.questions FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read Leaderboard" ON public.leaderboard FOR SELECT TO anon, authenticated USING (true);

-- Allow public insert on Leaderboard (submitting test scores)
CREATE POLICY "Public Insert Leaderboard" ON public.leaderboard FOR INSERT TO anon, authenticated WITH CHECK (true);

-- Grant schema permissions to PostgREST roles
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;

NOTIFY pgrst, 'reload schema';
"""

print("Creating tables in Supabase...")
res = execute_sql(create_tables_sql)
print("Tables created successfully:", res)
