-- ==============================================================================
-- Supabase Migration: Clean Up Duplicates, Enforce 1-Participant Count & Midnight Reset
-- Version: 20261011_cleanup_duplicates_and_midnight_reset.sql
-- ==============================================================================

-- 1. Permanently delete all unattempted dummy rows (Not yet 0/50)
DELETE FROM public.daily_participants 
WHERE has_attempted = false OR has_attempted IS NULL;

-- 2. Deduplicate daily_participants: Keep strictly the highest score per student per day
DELETE FROM public.daily_participants a USING public.daily_participants b
WHERE a.id < b.id
  AND a.date_key = b.date_key
  AND (a.student_id = b.student_id OR (a.roll_no IS NOT NULL AND a.roll_no != '' AND a.roll_no = b.roll_no));

-- 3. Deduplicate leaderboard: Keep strictly the highest score & fastest time per student per day
DELETE FROM public.leaderboard a USING public.leaderboard b
WHERE a.id < b.id
  AND a.date_key = b.date_key
  AND (a.student_id = b.student_id OR (a.roll_no IS NOT NULL AND a.roll_no != '' AND a.roll_no = b.roll_no));

-- 4. Unique Constraints to permanently block any duplicate inserts
DO $$
BEGIN
    ALTER TABLE public.daily_participants DROP CONSTRAINT IF EXISTS uq_daily_participants_student_date;
    ALTER TABLE public.daily_participants ADD CONSTRAINT uq_daily_participants_student_date UNIQUE (student_id, date_key);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 5. Row Level Security: Allow DELETE so 12:00 AM midnight reset can delete previous days
ALTER TABLE public.leaderboard ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_participants ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public Delete Leaderboard" ON public.leaderboard;
CREATE POLICY "Public Delete Leaderboard" ON public.leaderboard FOR DELETE TO anon, authenticated USING (true);

DROP POLICY IF EXISTS "Public Delete Daily Participants" ON public.daily_participants;
CREATE POLICY "Public Delete Daily Participants" ON public.daily_participants FOR DELETE TO anon, authenticated USING (true);

-- 6. Stored Procedure: Reset & permanently delete previous days at 12:00 AM midnight IST
CREATE OR REPLACE FUNCTION public.reset_daily_participants(
    p_current_date_key text
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    -- Delete previous days' participant records (date_key < today)
    DELETE FROM public.daily_participants WHERE date_key < p_current_date_key;

    -- Delete previous days' daily leaderboard rows (date_key < today)
    DELETE FROM public.leaderboard WHERE date_key < p_current_date_key;

    -- Also clean up any unattempted dummy records
    DELETE FROM public.daily_participants WHERE has_attempted = false;

    RETURN true;
END;
$$;

-- Alias for cleanup_midnight_daily_leaderboard
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

-- 7. Grant execution privileges to all PostgREST roles
GRANT EXECUTE ON FUNCTION public.reset_daily_participants TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.cleanup_midnight_daily_leaderboard TO anon, authenticated, service_role;
GRANT ALL ON public.daily_participants TO anon, authenticated, service_role;
GRANT ALL ON public.leaderboard TO anon, authenticated, service_role;

-- 8. Scheduled Server-side Cleanup Cron (12:00 AM IST = 18:30 UTC every night)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        PERFORM cron.unschedule('daily_midnight_cleanup_ist');
        PERFORM cron.schedule(
            'daily_midnight_cleanup_ist',
            '30 18 * * *',
            'DELETE FROM public.daily_participants WHERE date_key < to_char(now() AT TIME ZONE ''Asia/Kolkata'', ''YYYY-MM-DD'') OR has_attempted = false; DELETE FROM public.leaderboard WHERE date_key < to_char(now() AT TIME ZONE ''Asia/Kolkata'', ''YYYY-MM-DD'');'
        );
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

NOTIFY pgrst, 'reload schema';
