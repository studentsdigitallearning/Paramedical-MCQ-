-- ==============================================================================
-- Supabase Migration: Daily Test Top 100 Retention & Midnight Permanent Deletion
-- Version: 20261022_top100_retention_and_midnight_deletion.sql
-- Timezone: Asia/Kolkata (IST, UTC+05:30) | Midnight Cron: 30 18 * * * UTC
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Ensure schema columns for accuracy, UTC submission timestamp, and canonical class filtering exist
ALTER TABLE public.leaderboard ADD COLUMN IF NOT EXISTS accuracy real NOT NULL DEFAULT 0.0;
ALTER TABLE public.leaderboard ADD COLUMN IF NOT EXISTS submitted_at bigint NOT NULL DEFAULT (floor(extract(epoch from now()) * 1000)::bigint);
ALTER TABLE public.leaderboard ADD COLUMN IF NOT EXISTS roll_no text;
ALTER TABLE public.leaderboard ADD COLUMN IF NOT EXISTS course_name text;

ALTER TABLE public.daily_participants ADD COLUMN IF NOT EXISTS accuracy real NOT NULL DEFAULT 0.0;
ALTER TABLE public.daily_participants ADD COLUMN IF NOT EXISTS completion_time_seconds bigint NOT NULL DEFAULT 300;
ALTER TABLE public.daily_participants ADD COLUMN IF NOT EXISTS submitted_at bigint NOT NULL DEFAULT 0;

-- Optional temporary tables for daily attempt & saved answer tracking (automatically purged at midnight IST or when ranked > 100)
CREATE TABLE IF NOT EXISTS public.daily_test_attempts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id text NOT NULL,
    student_name text NOT NULL DEFAULT 'Student',
    roll_no text,
    class_name text NOT NULL,
    date_key text NOT NULL,
    score real NOT NULL DEFAULT 0.0,
    total_marks real NOT NULL DEFAULT 50.0,
    accuracy real NOT NULL DEFAULT 0.0,
    completion_time_seconds bigint NOT NULL DEFAULT 300,
    saved_answers jsonb NOT NULL DEFAULT '{}'::jsonb,
    submitted_at bigint NOT NULL DEFAULT (floor(extract(epoch from now()) * 1000)::bigint),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_daily_test_attempts_date_class
ON public.daily_test_attempts(date_key, class_name);

ALTER TABLE public.daily_test_attempts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public Select Daily Test Attempts" ON public.daily_test_attempts;
CREATE POLICY "Public Select Daily Test Attempts" ON public.daily_test_attempts
FOR SELECT TO anon, authenticated USING (true);

DROP POLICY IF EXISTS "Public Insert Daily Test Attempts" ON public.daily_test_attempts;
CREATE POLICY "Public Insert Daily Test Attempts" ON public.daily_test_attempts
FOR INSERT TO anon, authenticated WITH CHECK (true);

DROP POLICY IF EXISTS "Public Delete Daily Test Attempts" ON public.daily_test_attempts;
CREATE POLICY "Public Delete Daily Test Attempts" ON public.daily_test_attempts
FOR DELETE TO anon, authenticated USING (true);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.daily_test_attempts TO anon, authenticated, service_role;

-- Audit log table to verify midnight IST scheduled deletion executions
CREATE TABLE IF NOT EXISTS public.daily_cleanup_audit_logs (
    id bigserial PRIMARY KEY,
    executed_at_utc timestamptz NOT NULL DEFAULT now(),
    executed_at_ist text NOT NULL DEFAULT to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD HH24:MI:SS'),
    Protected_date_key text NOT NULL,
    deleted_leaderboard_count int NOT NULL DEFAULT 0,
    deleted_participants_count int NOT NULL DEFAULT 0,
    deleted_attempts_count int NOT NULL DEFAULT 0,
    deleted_captcha_count int NOT NULL DEFAULT 0,
    trigger_source text NOT NULL DEFAULT 'scheduled_midnight_ist'
);

ALTER TABLE public.daily_cleanup_audit_logs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public Read Cleanup Logs" ON public.daily_cleanup_audit_logs;
CREATE POLICY "Public Read Cleanup Logs" ON public.daily_cleanup_audit_logs
FOR SELECT TO anon, authenticated USING (true);
GRANT SELECT ON public.daily_cleanup_audit_logs TO anon, authenticated, service_role;
GRANT ALL ON public.daily_cleanup_audit_logs TO service_role;

-- 2. Canonical Single-Class Normalization Function
-- Ensures Class 8, Class 9, Class 10, Class 11, Class 12 are strictly independent
-- and use a single clean class filter regardless of course_name variations.
CREATE OR REPLACE FUNCTION public.normalize_jac_class_name(p_raw_class text)
RETURNS text LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    v_upper text := upper(trim(coalesce(p_raw_class, '')));
BEGIN
    IF v_upper = '' THEN
        RETURN 'Class 10';
    ELSIF v_upper LIKE '%12%' OR v_upper LIKE '%TWELFTH%' THEN
        RETURN 'Class 12';
    ELSIF v_upper LIKE '%11%' OR v_upper LIKE '%ELEVENTH%' THEN
        RETURN 'Class 11';
    ELSIF v_upper LIKE '%10%' OR v_upper LIKE '%TENTH%' THEN
        RETURN 'Class 10';
    ELSIF v_upper LIKE '%9%' OR v_upper LIKE '%NINTH%' THEN
        RETURN 'Class 9';
    ELSIF v_upper LIKE '%8%' OR v_upper LIKE '%EIGHTH%' THEN
        RETURN 'Class 8';
    ELSE
        RETURN trim(p_raw_class);
    END IF;
END;
$$;

-- Normalize existing rows in leaderboard and daily_participants so single class filter matches cleanly
UPDATE public.leaderboard
SET class_name = public.normalize_jac_class_name(class_name)
WHERE class_name IS NOT NULL;

UPDATE public.daily_participants
SET class_name = public.normalize_jac_class_name(class_name)
WHERE class_name IS NOT NULL;

-- Replace per-student date index with per-(student_id, class_name, date_key) unique index
-- so all 5 classes (Class 8, Class 9, Class 10, Class 11, Class 12) are strictly independent
DROP INDEX IF EXISTS public.idx_uq_leaderboard_student_date;
CREATE UNIQUE INDEX IF NOT EXISTS idx_uq_leaderboard_student_class_date
ON public.leaderboard (student_id, class_name, date_key);

ALTER TABLE public.daily_participants DROP CONSTRAINT IF EXISTS uq_daily_participants_student_date;
DO $$
BEGIN
    ALTER TABLE public.daily_participants
    ADD CONSTRAINT uq_daily_participants_student_class_date UNIQUE (student_id, class_name, date_key);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- Composite index for fast single-filter class + date Top 100 queries
DROP INDEX IF EXISTS public.idx_leaderboard_date_score;
CREATE INDEX IF NOT EXISTS idx_leaderboard_class_date_ranking
ON public.leaderboard (
    date_key,
    class_name,
    score DESC,
    accuracy DESC,
    submitted_at ASC,
    completion_time_seconds ASC
);

-- Ensure RLS allows UPDATE on leaderboard (fixes student name update & score improvement sync)
DROP POLICY IF EXISTS "Public Update Leaderboard" ON public.leaderboard;
CREATE POLICY "Public Update Leaderboard" ON public.leaderboard
FOR UPDATE TO anon, authenticated
USING (true)
WITH CHECK (
    score >= 0
    AND total_marks > 0
    AND score <= total_marks
    AND length(student_name) <= 50
    AND date_key ~ '^\d{4}-\d{2}-\d{2}$'
);

-- 3. Concurrency-Safe Top 100 Enforcement & Permanent Deletion Below Rank 100
-- Sorts by:
--   1. Highest score first (score DESC)
--   2. Highest accuracy next (accuracy DESC)
--   3. Earliest valid submission timestamp as final tie-breaker (submitted_at ASC, completion_time_seconds ASC, id ASC)
-- Permanently deletes all records ranked > 100 from public.leaderboard AND cascades permanent deletion
-- to public.daily_participants and public.daily_test_attempts for displaced/non-qualifying students.
CREATE OR REPLACE FUNCTION public.prune_leaderboard_top_100(
    p_date_key text DEFAULT NULL,
    p_class_name text DEFAULT NULL
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_today text := COALESCE(NULLIF(trim(p_date_key), ''), to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD'));
    v_norm_class text := CASE
        WHEN p_class_name IS NULL OR trim(p_class_name) = '' THEN NULL
        ELSE public.normalize_jac_class_name(p_class_name)
    END;
BEGIN
    -- Acquire transaction-level advisory lock per date + class to guarantee concurrency safety
    PERFORM pg_advisory_xact_lock(hashtext('top100_lock:' || v_today || ':' || COALESCE(v_norm_class, 'ALL')));

    -- Normalize any unnormalized class_name values for the target date
    UPDATE public.leaderboard
    SET class_name = public.normalize_jac_class_name(class_name),
        accuracy = CASE
            WHEN accuracy > 0 THEN accuracy
            WHEN total_marks > 0 THEN round(((score / total_marks) * 100.0)::numeric, 2)::real
            ELSE 0.0
        END,
        submitted_at = CASE
            WHEN submitted_at > 0 THEN submitted_at
            ELSE floor(extract(epoch from COALESCE(created_at, now())) * 1000)::bigint
        END
    WHERE date_key = v_today
      AND (v_norm_class IS NULL OR public.normalize_jac_class_name(class_name) = v_norm_class);

    -- Step 1: Deduplicate same-student or same-roll_no rows within the same (date_key, class_name),
    -- keeping only their highest score, highest accuracy, and earliest valid submission timestamp
    DELETE FROM public.leaderboard a USING public.leaderboard b
    WHERE a.date_key = v_today
      AND b.date_key = v_today
      AND a.class_name = b.class_name
      AND a.id <> b.id
      AND (
          a.student_id = b.student_id
          OR (a.roll_no IS NOT NULL AND a.roll_no <> '' AND a.roll_no = b.roll_no)
      )
      AND (
          a.score < b.score
          OR (a.score = b.score AND a.accuracy < b.accuracy)
          OR (a.score = b.score AND a.accuracy = b.accuracy AND a.submitted_at > b.submitted_at)
          OR (a.score = b.score AND a.accuracy = b.accuracy AND a.submitted_at = b.submitted_at AND a.completion_time_seconds > b.completion_time_seconds)
          OR (a.score = b.score AND a.accuracy = b.accuracy AND a.submitted_at = b.submitted_at AND a.completion_time_seconds = b.completion_time_seconds AND a.id > b.id)
      );

    -- Step 2: Identify all records ranked below 100 (rnk > 100) per independent class on v_today,
    -- permanently delete them from leaderboard, AND permanently delete their temporary registration,
    -- roll number identity, and attempt records from daily_participants and daily_test_attempts.
    WITH ranked_leaderboard AS (
        SELECT
            id,
            student_id,
            roll_no,
            class_name,
            date_key,
            ROW_NUMBER() OVER (
                PARTITION BY date_key, class_name
                ORDER BY
                    score DESC,
                    accuracy DESC,
                    submitted_at ASC,
                    completion_time_seconds ASC,
                    id ASC
            ) AS rnk
        FROM public.leaderboard
        WHERE date_key = v_today
          AND (v_norm_class IS NULL OR class_name = v_norm_class)
    ),
    displaced AS (
        DELETE FROM public.leaderboard l
        USING ranked_leaderboard r
        WHERE l.id = r.id
          AND r.rnk > 100
        RETURNING l.student_id, l.roll_no, l.class_name, l.date_key
    ),
    deleted_participants AS (
        DELETE FROM public.daily_participants dp
        USING displaced d
        WHERE dp.date_key = d.date_key
          AND public.normalize_jac_class_name(dp.class_name) = d.class_name
          AND (
              dp.student_id = d.student_id
              OR (d.roll_no IS NOT NULL AND d.roll_no <> '' AND dp.roll_no = d.roll_no)
          )
        RETURNING dp.id
    )
    DELETE FROM public.daily_test_attempts dta
    USING displaced d
    WHERE dta.date_key = d.date_key
      AND public.normalize_jac_class_name(dta.class_name) = d.class_name
      AND (
          dta.student_id = d.student_id
          OR (d.roll_no IS NOT NULL AND d.roll_no <> '' AND dta.roll_no = d.roll_no)
      );

    -- Step 3: Also ensure daily_participants has at most 100 attempted records per (date_key, class_name)
    WITH ranked_participants AS (
        SELECT
            id,
            ROW_NUMBER() OVER (
                PARTITION BY date_key, public.normalize_jac_class_name(class_name)
                ORDER BY
                    score DESC,
                    accuracy DESC,
                    CASE WHEN submitted_at > 0 THEN submitted_at ELSE floor(extract(epoch from COALESCE(registered_at, now())) * 1000)::bigint END ASC,
                    completion_time_seconds ASC,
                    id ASC
            ) AS rnk
        FROM public.daily_participants
        WHERE date_key = v_today
          AND has_attempted = true
          AND (v_norm_class IS NULL OR public.normalize_jac_class_name(class_name) = v_norm_class)
    )
    DELETE FROM public.daily_participants
    WHERE id IN (SELECT id FROM ranked_participants WHERE rnk > 100);

    RETURN true;
END;
$$;

GRANT EXECUTE ON FUNCTION public.prune_leaderboard_top_100 TO anon, authenticated, service_role;

-- 4. Atomic Server-Side Submission & Top 100 Qualification RPC
-- Validates submission, inserts/updates if eligible, recalculates class ranking,
-- keeps strictly Top 100, and permanently deletes all data for non-qualifying or displaced students.
CREATE OR REPLACE FUNCTION public.submit_daily_test_result_top100(
    p_student_id text,
    p_student_name text,
    p_class_name text,
    p_date_key text,
    p_score real,
    p_total_marks real,
    p_accuracy real,
    p_completion_time_seconds bigint,
    p_submitted_at bigint DEFAULT NULL,
    p_roll_no text DEFAULT NULL,
    p_course_name text DEFAULT NULL
) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_ist_today text := to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD');
    v_date_key text := COALESCE(NULLIF(trim(p_date_key), ''), v_ist_today);
    v_class_name text := public.normalize_jac_class_name(p_class_name);
    v_clean_name text := COALESCE(NULLIF(trim(substring(p_student_name from 1 for 50)), ''), 'Student');
    v_accuracy real := COALESCE(
        NULLIF(p_accuracy, 0.0),
        CASE WHEN p_total_marks > 0 THEN round(((p_score / p_total_marks) * 100.0)::numeric, 2)::real ELSE 0.0 END
    );
    v_submitted_at bigint := COALESCE(NULLIF(p_submitted_at, 0), floor(extract(epoch from now()) * 1000)::bigint);
    v_entry_id bigint;
    v_final_rank int;
    v_qualifies boolean := false;
BEGIN
    IF p_score < 0 OR p_total_marks <= 0 OR p_score > p_total_marks THEN
        RAISE EXCEPTION 'Invalid score or total_marks';
    END IF;

    -- Concurrency lock per (date_key, class_name)
    PERFORM pg_advisory_xact_lock(hashtext('top100_lock:' || v_date_key || ':' || v_class_name));

    -- Upsert into leaderboard for (student_id, class_name, date_key)
    INSERT INTO public.leaderboard (
        student_id, student_name, roll_no, course_name, class_name, date_key,
        score, total_marks, accuracy, completion_time_seconds, attempts_count, submitted_at
    ) VALUES (
        p_student_id, v_clean_name, p_roll_no, COALESCE(p_course_name, v_class_name), v_class_name, v_date_key,
        p_score, p_total_marks, v_accuracy, GREATEST(1, p_completion_time_seconds), 1, v_submitted_at
    )
    ON CONFLICT (student_id, class_name, date_key) DO UPDATE SET
        student_name = EXCLUDED.student_name,
        roll_no = COALESCE(NULLIF(EXCLUDED.roll_no, ''), public.leaderboard.roll_no),
        score = CASE
            WHEN EXCLUDED.score > public.leaderboard.score THEN EXCLUDED.score
            ELSE public.leaderboard.score
        END,
        total_marks = EXCLUDED.total_marks,
        accuracy = CASE
            WHEN EXCLUDED.score > public.leaderboard.score THEN EXCLUDED.accuracy
            WHEN EXCLUDED.score = public.leaderboard.score AND EXCLUDED.accuracy > public.leaderboard.accuracy THEN EXCLUDED.accuracy
            ELSE public.leaderboard.accuracy
        END,
        completion_time_seconds = CASE
            WHEN EXCLUDED.score > public.leaderboard.score THEN EXCLUDED.completion_time_seconds
            WHEN EXCLUDED.score = public.leaderboard.score AND EXCLUDED.accuracy > public.leaderboard.accuracy THEN EXCLUDED.completion_time_seconds
            WHEN EXCLUDED.score = public.leaderboard.score AND EXCLUDED.accuracy = public.leaderboard.accuracy AND EXCLUDED.completion_time_seconds < public.leaderboard.completion_time_seconds THEN EXCLUDED.completion_time_seconds
            ELSE public.leaderboard.completion_time_seconds
        END,
        submitted_at = CASE
            WHEN EXCLUDED.score > public.leaderboard.score THEN EXCLUDED.submitted_at
            WHEN EXCLUDED.score = public.leaderboard.score AND EXCLUDED.accuracy > public.leaderboard.accuracy THEN EXCLUDED.submitted_at
            WHEN EXCLUDED.score = public.leaderboard.score AND EXCLUDED.accuracy = public.leaderboard.accuracy AND EXCLUDED.submitted_at < public.leaderboard.submitted_at THEN EXCLUDED.submitted_at
            ELSE public.leaderboard.submitted_at
        END,
        attempts_count = public.leaderboard.attempts_count + 1
    RETURNING id INTO v_entry_id;

    -- Enforce Top 100 and permanently delete all records ranked > 100 (including non-qualifying or displaced students)
    PERFORM public.prune_leaderboard_top_100(v_date_key, v_class_name);

    -- Check if the submitted student qualified in the Top 100 after pruning
    SELECT rnk INTO v_final_rank
    FROM (
        SELECT
            student_id,
            ROW_NUMBER() OVER (
                ORDER BY
                    score DESC,
                    accuracy DESC,
                    submitted_at ASC,
                    completion_time_seconds ASC,
                    id ASC
            ) AS rnk
        FROM public.leaderboard
        WHERE date_key = v_date_key AND class_name = v_class_name
    ) ranked
    WHERE student_id = p_student_id;

    IF v_final_rank IS NOT NULL AND v_final_rank <= 100 THEN
        v_qualifies := true;
        -- Student qualified in Top 100: retain their participant record for today
        INSERT INTO public.daily_participants (
            student_id, student_name, course_name, class_name, roll_no, date_key,
            score, total_marks, accuracy, completion_time_seconds, has_attempted, submitted_at
        ) VALUES (
            p_student_id, v_clean_name, COALESCE(p_course_name, v_class_name), v_class_name,
            COALESCE(NULLIF(p_roll_no, ''), '0000000000'), v_date_key,
            p_score, p_total_marks, v_accuracy, GREATEST(1, p_completion_time_seconds), true, v_submitted_at
        )
        ON CONFLICT (student_id, class_name, date_key) DO UPDATE SET
            student_name = EXCLUDED.student_name,
            roll_no = EXCLUDED.roll_no,
            score = GREATEST(public.daily_participants.score, EXCLUDED.score),
            total_marks = EXCLUDED.total_marks,
            accuracy = GREATEST(public.daily_participants.accuracy, EXCLUDED.accuracy),
            completion_time_seconds = EXCLUDED.completion_time_seconds,
            has_attempted = true,
            submitted_at = EXCLUDED.submitted_at;
    ELSE
        -- Did not qualify for Top 100: permanently delete temporary registration, identity, and attempt data
        v_qualifies := false;
        DELETE FROM public.leaderboard
        WHERE date_key = v_date_key AND class_name = v_class_name AND student_id = p_student_id;

        DELETE FROM public.daily_participants
        WHERE date_key = v_date_key
          AND public.normalize_jac_class_name(class_name) = v_class_name
          AND (student_id = p_student_id OR (p_roll_no IS NOT NULL AND p_roll_no <> '' AND roll_no = p_roll_no));

        DELETE FROM public.daily_test_attempts
        WHERE date_key = v_date_key
          AND public.normalize_jac_class_name(class_name) = v_class_name
          AND (student_id = p_student_id OR (p_roll_no IS NOT NULL AND p_roll_no <> '' AND roll_no = p_roll_no));
    END IF;

    RETURN jsonb_build_object(
        'qualified', v_qualifies,
        'rank', v_final_rank,
        'submission_id', CASE WHEN v_qualifies THEN v_entry_id ELSE NULL END,
        'class_name', v_class_name,
        'date_key', v_date_key
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.submit_daily_test_result_top100 TO anon, authenticated, service_role;

-- 5. Next Day Midnight (12:00 AM Asia/Kolkata) Permanent Deletion Function
-- Safely deletes ONLY previous-day temporary Daily Test data (date_key < v_ist_today).
-- NEVER deletes current-day (date_key >= v_ist_today) records even if a client with a lagging clock calls it!
-- NEVER deletes permanent educational content (classes, subjects, chapters, tests, questions, app_config).
CREATE OR REPLACE FUNCTION public.reset_daily_participants(
    p_current_date_key text DEFAULT NULL
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_server_ist_today text := to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD');
    v_effective_today text;
    v_del_lead int := 0;
    v_del_part int := 0;
    v_del_att int := 0;
    v_del_cap int := 0;
BEGIN
    -- Protect new-day records: use the server's authoritative Asia/Kolkata date unless a valid newer date is explicitly provided for testing
    IF p_current_date_key IS NOT NULL AND trim(p_current_date_key) ~ '^\d{4}-\d{2}-\d{2}$' AND trim(p_current_date_key) > v_server_ist_today THEN
        v_effective_today := trim(p_current_date_key);
    ELSE
        v_effective_today := v_server_ist_today;
    END IF;

    -- 1. Permanently delete all previous-day Top 100 leaderboard entries across all classes
    WITH del AS (
        DELETE FROM public.leaderboard
        WHERE date_key IS NULL OR date_key < v_effective_today
        RETURNING id
    )
    SELECT count(*) INTO v_del_lead FROM del;

    -- 2. Permanently delete all previous-day temporary registrations, roll numbers, and participant records
    -- Also purge stale unattempted registrations older than 3 hours
    WITH del AS (
        DELETE FROM public.daily_participants
        WHERE date_key IS NULL
           OR date_key < v_effective_today
           OR (has_attempted = false AND registered_at < now() - interval '3 hours')
        RETURNING id
    )
    SELECT count(*) INTO v_del_part FROM del;

    -- 3. Permanently delete all previous-day temporary attempt & saved answer records
    WITH del AS (
        DELETE FROM public.daily_test_attempts
        WHERE date_key IS NULL OR date_key < v_effective_today
        RETURNING id
    )
    SELECT count(*) INTO v_del_att FROM del;

    -- 4. Permanently delete expired or used CAPTCHA challenges
    WITH del AS (
        DELETE FROM public.captcha_challenges
        WHERE expires_at < now() OR is_used = true
        RETURNING id
    )
    SELECT count(*) INTO v_del_cap FROM del;

    -- 5. Record execution in audit log if any rows were cleaned or if triggered at midnight
    IF (v_del_lead + v_del_part + v_del_att + v_del_cap) > 0 THEN
        INSERT INTO public.daily_cleanup_audit_logs (
            protected_date_key,
            deleted_leaderboard_count,
            deleted_participants_count,
            deleted_attempts_count,
            deleted_captcha_count,
            trigger_source
        ) VALUES (
            v_effective_today,
            v_del_lead,
            v_del_part,
            v_del_att,
            v_del_cap,
            'reset_daily_participants'
        );
    END IF;

    RETURN true;
END;
$$;

CREATE OR REPLACE FUNCTION public.cleanup_midnight_daily_leaderboard(
    p_current_date text DEFAULT NULL,
    p_current_date_key text DEFAULT NULL
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
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

-- 6. Triggers for Automatic Enforcement on Insert/Update
CREATE OR REPLACE FUNCTION public.trg_fn_purge_previous_days_leaderboard()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_today text := to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD');
BEGIN
    DELETE FROM public.leaderboard
    WHERE date_key IS NULL OR date_key < v_today;

    DELETE FROM public.daily_participants
    WHERE date_key IS NULL OR date_key < v_today;

    DELETE FROM public.daily_test_attempts
    WHERE date_key IS NULL OR date_key < v_today;

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

CREATE OR REPLACE FUNCTION public.trg_fn_enforce_top_100_only()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
BEGIN
    PERFORM public.prune_leaderboard_top_100(NEW.date_key, NEW.class_name);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_keep_only_top_100_leaderboard ON public.leaderboard;
CREATE TRIGGER trg_keep_only_top_100_leaderboard
AFTER INSERT OR UPDATE ON public.leaderboard
FOR EACH ROW
EXECUTE FUNCTION public.trg_fn_enforce_top_100_only();

-- 7. Configure Scheduled Midnight Deletion Job at 12:00 AM IST (18:30 UTC previous UTC date)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        BEGIN
            PERFORM cron.unschedule('daily_midnight_cleanup_ist');
        EXCEPTION WHEN OTHERS THEN NULL;
        END;

        PERFORM cron.schedule(
            'daily_midnight_cleanup_ist',
            '30 18 * * *',
            $cron$SELECT public.reset_daily_participants(to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD'));$cron$
        );
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- Execute immediate cleanup of previous-day data and prune any class exceeding Top 100
SELECT public.reset_daily_participants(to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD'));
SELECT public.prune_leaderboard_top_100(to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD'), NULL);

NOTIFY pgrst, 'reload schema';
