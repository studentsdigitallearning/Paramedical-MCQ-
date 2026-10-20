-- ==============================================================================
-- Supabase Migration: Permanent 12:00 AM Midnight Daily Leaderboard Deletion
-- Applies to: Both User App & Admin Panel (public.leaderboard & public.daily_participants)
-- Version: 20261020_permanent_midnight_leaderboard_delete.sql
-- ==============================================================================

-- 1. Immediately purge all previous days' records and unattempted rows
DELETE FROM public.leaderboard
WHERE date_key IS NULL
   OR date_key <> to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD');

DELETE FROM public.daily_participants
WHERE date_key IS NULL
   OR date_key <> to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD')
   OR has_attempted = false;

-- 2. Ensure Row Level Security allows DELETE & UPDATE for Admin Panel & Midnight Reset
ALTER TABLE public.leaderboard ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_participants ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public Delete Leaderboard" ON public.leaderboard;
CREATE POLICY "Public Delete Leaderboard" ON public.leaderboard
FOR DELETE TO anon, authenticated
USING (true);

DROP POLICY IF EXISTS "Public Update Leaderboard" ON public.leaderboard;
CREATE POLICY "Public Update Leaderboard" ON public.leaderboard
FOR UPDATE TO anon, authenticated
USING (true);

DROP POLICY IF EXISTS "Public Delete Daily Participants" ON public.daily_participants;
CREATE POLICY "Public Delete Daily Participants" ON public.daily_participants
FOR DELETE TO anon, authenticated
USING (true);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.leaderboard TO anon, authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.daily_participants TO anon, authenticated, service_role;

-- 3. Stored Procedure: Permanently delete all non-today leaderboard & participant records at 12:00 AM midnight IST
CREATE OR REPLACE FUNCTION public.reset_daily_participants(
    p_current_date_key text DEFAULT NULL
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_today text := COALESCE(NULLIF(trim(p_current_date_key), ''), to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD'));
BEGIN
    -- Permanently delete all leaderboard rows not matching today's IST date_key
    DELETE FROM public.leaderboard
    WHERE date_key IS NULL OR date_key <> v_today;

    -- Permanently delete all daily_participants rows not matching today's IST date_key or unattempted
    DELETE FROM public.daily_participants
    WHERE date_key IS NULL OR date_key <> v_today OR has_attempted = false;

    RETURN true;
END;
$$;

CREATE OR REPLACE FUNCTION public.cleanup_midnight_daily_leaderboard(
    p_current_date text DEFAULT NULL,
    p_current_date_key text DEFAULT NULL
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_date text := COALESCE(
        NULLIF(trim(p_current_date_key), ''),
        NULLIF(trim(p_current_date), ''),
        to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD')
    );
BEGIN
    RETURN public.reset_daily_participants(v_date);
END;
$$;

GRANT EXECUTE ON FUNCTION public.reset_daily_participants TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.cleanup_midnight_daily_leaderboard TO anon, authenticated, service_role;

-- 4. Automatic Trigger: Whenever a new daily score or participant is inserted on a new day,
-- automatically delete any leftover rows from previous days in both tables
CREATE OR REPLACE FUNCTION public.trg_fn_purge_previous_days_leaderboard()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_today text := to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD');
BEGIN
    DELETE FROM public.leaderboard
    WHERE date_key IS NULL OR date_key < v_today;

    DELETE FROM public.daily_participants
    WHERE date_key IS NULL OR date_key < v_today OR has_attempted = false;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_auto_purge_old_daily_leaderboard ON public.leaderboard;
CREATE TRIGGER trg_auto_purge_old_daily_leaderboard
AFTER INSERT ON public.leaderboard
FOR EACH STATEMENT
EXECUTE FUNCTION public.trg_fn_purge_previous_days_leaderboard();

DROP TRIGGER IF EXISTS trg_auto_purge_old_daily_participants ON public.daily_participants;
CREATE TRIGGER trg_auto_purge_old_daily_participants
AFTER INSERT ON public.daily_participants
FOR EACH STATEMENT
EXECUTE FUNCTION public.trg_fn_purge_previous_days_leaderboard();

-- 5. Scheduled Server-side Cleanup Cron (12:00 AM Midnight IST = 18:30 UTC every night)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        PERFORM cron.unschedule('daily_midnight_cleanup_ist');
        PERFORM cron.schedule(
            'daily_midnight_cleanup_ist',
            '30 18 * * *',
            'SELECT public.reset_daily_participants(to_char(now() AT TIME ZONE ''Asia/Kolkata'', ''YYYY-MM-DD''));'
        );
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 6. Ensure Realtime is enabled for both leaderboard and daily_participants
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.leaderboard;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.daily_participants;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

NOTIFY pgrst, 'reload schema';
