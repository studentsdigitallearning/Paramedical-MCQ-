-- ==============================================================================
-- Supabase Schema Migration: Daily Test Registration, Roll Numbers, and Midnight Reset
-- Version: 20261009_daily_participants_and_cleanup.sql
-- ==============================================================================

-- 1. Daily Test Participants Table (Temporary Daily Records)
CREATE TABLE IF NOT EXISTS public.daily_participants (
    id serial PRIMARY KEY,
    student_id text NOT NULL,
    student_name text NOT NULL,
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

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_daily_participants_date ON public.daily_participants(date_key);
CREATE INDEX IF NOT EXISTS idx_daily_participants_student_date ON public.daily_participants(student_id, date_key);
CREATE INDEX IF NOT EXISTS idx_daily_participants_roll_date ON public.daily_participants(roll_no, date_key);

-- 2. Add roll_no and course_name columns to public.leaderboard if missing
ALTER TABLE public.leaderboard ADD COLUMN IF NOT EXISTS roll_no text;
ALTER TABLE public.leaderboard ADD COLUMN IF NOT EXISTS course_name text;

-- Deduplicate any existing duplicate leaderboard entries for the same student or roll_no on the same date
DELETE FROM public.leaderboard a USING public.leaderboard b
WHERE a.id < b.id
  AND a.date_key = b.date_key
  AND ((a.roll_no IS NOT NULL AND a.roll_no != '' AND a.roll_no = b.roll_no)
       OR (a.student_id = b.student_id));

-- Unique constraint / index on leaderboard to prevent duplicate data for the same roll number on the same day
CREATE UNIQUE INDEX IF NOT EXISTS idx_uq_leaderboard_roll_date 
ON public.leaderboard (roll_no, date_key) 
WHERE roll_no IS NOT NULL AND roll_no != '';

CREATE UNIQUE INDEX IF NOT EXISTS idx_uq_leaderboard_student_date 
ON public.leaderboard (student_id, date_key);

-- 3. Stored Procedure: Get or Create Temporary Roll Number
-- Format: "YYMMDDNNNN", e.g. "2610090001"
-- Enforces: Only ONE roll number per student per day across all courses.
-- Zero duplicate roll numbers: Uses atomic query + loop with unique_violation recovery.
CREATE OR REPLACE FUNCTION public.get_or_create_daily_roll_no(
    p_student_id text,
    p_student_name text,
    p_course_name text,
    p_class_name text,
    p_date_key text
) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_existing record;
    v_seq int;
    v_roll text;
    v_yy text;
    v_mm text;
    v_dd text;
BEGIN
    -- 1. Check if student already registered for today across any course
    SELECT * INTO v_existing FROM public.daily_participants
    WHERE date_key = p_date_key
      AND (student_id = p_student_id 
           OR (p_student_name != 'Student' AND p_student_name != '' AND student_name = p_student_name));

    IF FOUND THEN
        RETURN jsonb_build_object(
            'roll_no', v_existing.roll_no,
            'already_attempted', v_existing.has_attempted,
            'course_name', v_existing.course_name,
            'class_name', v_existing.class_name,
            'score', v_existing.score,
            'total_marks', v_existing.total_marks,
            'is_new', false
        );
    END IF;

    -- Also check leaderboard in case already submitted
    SELECT * INTO v_existing FROM public.leaderboard
    WHERE date_key = p_date_key
      AND (student_id = p_student_id 
           OR (p_student_name != 'Student' AND p_student_name != '' AND student_name = p_student_name));

    IF FOUND AND v_existing.roll_no IS NOT NULL AND v_existing.roll_no != '' THEN
        -- Ensure participant record exists
        INSERT INTO public.daily_participants (
            student_id, student_name, course_name, class_name, roll_no, date_key, has_attempted, score, total_marks
        ) VALUES (
            p_student_id, p_student_name, p_course_name, p_class_name, v_existing.roll_no, p_date_key, true, v_existing.score, v_existing.total_marks
        ) ON CONFLICT (roll_no, date_key) DO NOTHING;

        RETURN jsonb_build_object(
            'roll_no', v_existing.roll_no,
            'already_attempted', true,
            'course_name', p_course_name,
            'class_name', p_class_name,
            'score', v_existing.score,
            'total_marks', v_existing.total_marks,
            'is_new', false
        );
    END IF;

    -- 2. Extract date components for format "YYMMDDNNNN"
    v_yy := substring(p_date_key from 3 for 2);
    v_mm := substring(p_date_key from 6 for 2);
    v_dd := substring(p_date_key from 9 for 2);

    -- Calculate max sequential number for today across BOTH daily_participants and leaderboard
    SELECT COALESCE(
        GREATEST(
            (SELECT MAX(CASE WHEN roll_no ~ '^[0-9]{10}$' THEN SUBSTRING(roll_no FROM 7)::int ELSE 0 END) 
             FROM public.daily_participants WHERE date_key = p_date_key),
            (SELECT MAX(CASE WHEN roll_no ~ '^[0-9]{10}$' THEN SUBSTRING(roll_no FROM 7)::int ELSE 0 END) 
             FROM public.leaderboard WHERE date_key = p_date_key)
        ), 0
    ) + 1 INTO v_seq;

    -- 3. Loop with unique constraint retry to guarantee 100% collision-free roll numbers
    LOOP
        v_roll := v_yy || v_mm || v_dd || lpad(v_seq::text, 4, '0');

        BEGIN
            INSERT INTO public.daily_participants (
                student_id, student_name, course_name, class_name, roll_no, date_key, has_attempted
            ) VALUES (
                p_student_id, p_student_name, p_course_name, p_class_name, v_roll, p_date_key, false
            );
            EXIT; -- Insert succeeded without conflict
        EXCEPTION WHEN unique_violation THEN
            -- Check if current student was already registered concurrently
            SELECT * INTO v_existing FROM public.daily_participants
            WHERE date_key = p_date_key AND student_id = p_student_id;

            IF FOUND THEN
                RETURN jsonb_build_object(
                    'roll_no', v_existing.roll_no,
                    'already_attempted', v_existing.has_attempted,
                    'course_name', v_existing.course_name,
                    'class_name', v_existing.class_name,
                    'score', v_existing.score,
                    'total_marks', v_existing.total_marks,
                    'is_new', false
                );
            END IF;

            -- Roll number was taken by another participant, advance sequence and retry
            v_seq := v_seq + 1;
        END;
    END LOOP;

    RETURN jsonb_build_object(
        'roll_no', v_roll,
        'already_attempted', false,
        'course_name', p_course_name,
        'class_name', p_class_name,
        'score', 0.0,
        'total_marks', 50.0,
        'is_new', true
    );
END;
$$;

-- 4. Stored Procedure: Reset & Delete Previous-Day Records at Midnight IST
-- Permanently deletes all previous-day temporary participant records, Roll Numbers, scores, and daily leaderboard entries.
-- Preserves permanent courses, questions, tests, and student profiles.
CREATE OR REPLACE FUNCTION public.reset_daily_participants(
    p_current_date_key text
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    -- Delete previous-day temporary participants
    DELETE FROM public.daily_participants WHERE date_key < p_current_date_key;

    -- Delete previous-day daily leaderboard rows
    DELETE FROM public.leaderboard WHERE date_key < p_current_date_key;

    RETURN true;
END;
$$;

-- 5. Row Level Security & Permissions
ALTER TABLE public.daily_participants ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public Read Daily Participants" ON public.daily_participants FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Insert Daily Participants" ON public.daily_participants FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Public Update Daily Participants" ON public.daily_participants FOR UPDATE TO anon, authenticated USING (true);
CREATE POLICY "Public Delete Daily Participants" ON public.daily_participants FOR DELETE TO anon, authenticated USING (true);

GRANT ALL ON public.daily_participants TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.get_or_create_daily_roll_no TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.reset_daily_participants TO anon, authenticated, service_role;

-- 6. Scheduled Server-side Cleanup Cron (12:00 AM IST = 18:30 UTC)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        PERFORM cron.schedule(
            'daily_midnight_cleanup_ist',
            '30 18 * * *',
            'DELETE FROM public.daily_participants WHERE date_key < to_char(now() AT TIME ZONE ''Asia/Kolkata'', ''YYYY-MM-DD''); DELETE FROM public.leaderboard WHERE date_key < to_char(now() AT TIME ZONE ''Asia/Kolkata'', ''YYYY-MM-DD'');'
        );
    END IF;
END $$;

NOTIFY pgrst, 'reload schema';
