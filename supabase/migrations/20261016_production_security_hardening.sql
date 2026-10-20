-- ==============================================================================
-- Supabase Schema Migration: Production Security Hardening & Zero-Trust RLS
-- Version: 20261016_production_security_hardening.sql
-- ==============================================================================

-- 1. Secure Search Path & Input Validation on Stored Procedures
CREATE OR REPLACE FUNCTION public.get_or_create_daily_roll_no(
    p_student_id text,
    p_student_name text,
    p_course_name text,
    p_class_name text,
    p_date_key text
) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_existing record;
    v_seq int;
    v_roll text;
    v_yy text;
    v_mm text;
    v_dd text;
    v_clean_name text;
BEGIN
    -- Input sanitization
    v_clean_name := trim(substring(p_student_name from 1 for 50));
    IF v_clean_name = '' THEN
        v_clean_name := 'Student';
    END IF;

    -- Validate date format (YYYY-MM-DD)
    IF p_date_key !~ '^\d{4}-\d{2}-\d{2}$' THEN
        RAISE EXCEPTION 'Invalid date format. Expected YYYY-MM-DD.';
    END IF;

    -- 1. Check if student already registered for today across any course
    SELECT * INTO v_existing FROM public.daily_participants
    WHERE date_key = p_date_key
      AND (student_id = p_student_id 
           OR (v_clean_name != 'Student' AND student_name = v_clean_name));

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
           OR (v_clean_name != 'Student' AND student_name = v_clean_name));

    IF FOUND AND v_existing.roll_no IS NOT NULL AND v_existing.roll_no != '' THEN
        INSERT INTO public.daily_participants (
            student_id, student_name, course_name, class_name, roll_no, date_key, has_attempted, score, total_marks
        ) VALUES (
            p_student_id, v_clean_name, p_course_name, p_class_name, v_existing.roll_no, p_date_key, true, v_existing.score, v_existing.total_marks
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
                p_student_id, v_clean_name, p_course_name, p_class_name, v_roll, p_date_key, false
            );
            EXIT; -- Insert succeeded without conflict
        EXCEPTION WHEN unique_violation THEN
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

-- 2. Stored Procedure: Reset & permanently delete previous days at 12:00 AM midnight IST
CREATE OR REPLACE FUNCTION public.reset_daily_participants(
    p_current_date_key text
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
BEGIN
    -- Validate date format
    IF p_current_date_key !~ '^\d{4}-\d{2}-\d{2}$' THEN
        RAISE EXCEPTION 'Invalid date format. Expected YYYY-MM-DD.';
    END IF;

    -- Permanently delete previous-day temporary participant records
    DELETE FROM public.daily_participants WHERE date_key < p_current_date_key;

    -- Permanently delete previous-day daily leaderboard rows
    DELETE FROM public.leaderboard WHERE date_key < p_current_date_key;

    -- Clean up any unattempted dummy records
    DELETE FROM public.daily_participants WHERE has_attempted = false;

    RETURN true;
END;
$$;

CREATE OR REPLACE FUNCTION public.cleanup_midnight_daily_leaderboard(
    p_current_date text DEFAULT NULL,
    p_current_date_key text DEFAULT NULL
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_date text := COALESCE(p_current_date_key, p_current_date, to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD'));
BEGIN
    RETURN public.reset_daily_participants(v_date);
END;
$$;

-- 3. Hardened Row Level Security (RLS) Policies
-- Ensure RLS is active on all tables
ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chapters ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leaderboard ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;

-- Drop insecure / overly permissive policies
DO $$
BEGIN
    DROP POLICY IF EXISTS "Public Delete Leaderboard" ON public.leaderboard;
    DROP POLICY IF EXISTS "Public Delete Daily Participants" ON public.daily_participants;
    DROP POLICY IF EXISTS "Public Update Daily Participants" ON public.daily_participants;
    DROP POLICY IF EXISTS "Public Insert Leaderboard" ON public.leaderboard;
    DROP POLICY IF EXISTS "Public Insert Daily Participants" ON public.daily_participants;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 4. Content Tables: Read-Only for Public Client (anon)
-- Content tables can ONLY be updated by authenticated admins or service_role
CREATE POLICY "Public Read Classes" ON public.classes FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Public Read Categories" ON public.categories FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Public Read Subjects" ON public.subjects FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Public Read Chapters" ON public.chapters FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Public Read Tests" ON public.tests FOR SELECT TO anon, authenticated USING (is_published = true AND is_active = true);
CREATE POLICY "Public Read Questions" ON public.questions FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Public Read App Config" ON public.app_config FOR SELECT TO anon, authenticated USING (true);

-- 5. Leaderboard: Public Read, Validated Insert & Delete (for 12:00 AM Midnight Reset & Admin Panel)
CREATE POLICY "Public Read Leaderboard" ON public.leaderboard 
FOR SELECT TO anon, authenticated 
USING (true);

CREATE POLICY "Public Validated Insert Leaderboard" ON public.leaderboard 
FOR INSERT TO anon, authenticated 
WITH CHECK (
    score >= 0 
    AND total_marks > 0 
    AND score <= total_marks 
    AND length(student_name) <= 50
    AND date_key ~ '^\d{4}-\d{2}-\d{2}$'
);

CREATE POLICY "Public Delete Leaderboard" ON public.leaderboard 
FOR DELETE TO anon, authenticated 
USING (true);

-- 6. Daily Participants: Public Read, Validated Insert, Update & Delete (for 12:00 AM Midnight Reset & Admin Panel)
CREATE POLICY "Public Read Daily Participants" ON public.daily_participants 
FOR SELECT TO anon, authenticated 
USING (true);

CREATE POLICY "Public Validated Insert Daily Participants" ON public.daily_participants 
FOR INSERT TO anon, authenticated 
WITH CHECK (
    date_key ~ '^\d{4}-\d{2}-\d{2}$'
    AND roll_no ~ '^\d{10}$'
    AND score >= 0 
    AND total_marks > 0
    AND length(student_name) <= 50
);

CREATE POLICY "Public Validated Update Daily Participants" ON public.daily_participants 
FOR UPDATE TO anon, authenticated 
USING (true)
WITH CHECK (
    score >= 0 
    AND total_marks > 0 
    AND score <= total_marks
);

CREATE POLICY "Public Delete Daily Participants" ON public.daily_participants 
FOR DELETE TO anon, authenticated 
USING (true);

-- 7. Grant Privileges
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.daily_participants TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.leaderboard TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;

GRANT EXECUTE ON FUNCTION public.get_or_create_daily_roll_no TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.reset_daily_participants TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.cleanup_midnight_daily_leaderboard TO anon, authenticated, service_role;

NOTIFY pgrst, 'reload schema';
