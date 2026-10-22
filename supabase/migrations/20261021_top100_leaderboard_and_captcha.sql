-- ==============================================================================
-- Supabase Migration: Top 100 Only Leaderboard Pruning & Server-Side Text CAPTCHA
-- Version: 20261021_top100_leaderboard_and_captcha.sql
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Secure Server-Only CAPTCHA Challenges Table
-- Never exposed through public database queries (RLS enabled with zero anon/authenticated policies)
CREATE TABLE IF NOT EXISTS public.captcha_challenges (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id text NOT NULL,
    answer_hash text NOT NULL,
    attempts_count int NOT NULL DEFAULT 0,
    max_attempts int NOT NULL DEFAULT 5,
    is_used boolean NOT NULL DEFAULT false,
    expires_at timestamptz NOT NULL,
    verified_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_captcha_challenges_student ON public.captcha_challenges(student_id, is_used);
CREATE INDEX IF NOT EXISTS idx_captcha_challenges_expires ON public.captcha_challenges(expires_at);

ALTER TABLE public.captcha_challenges ENABLE ROW LEVEL SECURITY;

-- Explicitly revoke direct table access from anon and authenticated so answers/hashes
-- can NEVER be queried through public PostgREST table endpoints
REVOKE ALL ON TABLE public.captcha_challenges FROM anon, authenticated;
GRANT ALL ON TABLE public.captcha_challenges TO service_role;

-- 2. Server-Side CAPTCHA Challenge Generator Function (SECURITY DEFINER)
-- Returns only the challenge UUID, expiration time, and visual glyph coordinates (never plain answer field)
CREATE OR REPLACE FUNCTION public.create_captcha_challenge(
    p_student_id text
) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_alphabet constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    v_code text := '';
    v_i int;
    v_idx int;
    v_ch text;
    v_challenge_id uuid := gen_random_uuid();
    v_expires_at timestamptz := now() + interval '2 minutes';
    v_salt constant text := 'jac_mcq_master_server_captcha_salt_2026';
    v_hash text;
    v_glyphs jsonb := '[]'::jsonb;
BEGIN
    IF p_student_id IS NULL OR length(trim(p_student_id)) < 2 THEN
        RAISE EXCEPTION 'Invalid student_id';
    END IF;

    -- Purge expired or used challenges and invalidate previous active challenges for this student
    DELETE FROM public.captcha_challenges
    WHERE expires_at < now() OR is_used = true OR student_id = p_student_id;

    -- Generate random 6-character alphanumeric code
    FOR v_i IN 1..6 LOOP
        v_idx := floor(random() * length(v_alphabet))::int + 1;
        v_ch := substr(v_alphabet, v_idx, 1);
        v_code := v_code || v_ch;
        v_glyphs := v_glyphs || jsonb_build_array(
            jsonb_build_object(
                'code_point', ascii(v_ch),
                'rotation_deg', floor(random() * 31 - 15)::int,
                'offset_y', floor(random() * 11 - 5)::int,
                'scale', round((0.92 + random() * 0.16)::numeric, 2)
            )
        );
    END LOOP;

    v_hash := encode(digest(v_challenge_id::text || ':' || upper(v_code) || ':' || v_salt, 'sha256'), 'hex');

    INSERT INTO public.captcha_challenges (
        id, student_id, answer_hash, attempts_count, max_attempts, is_used, expires_at
    ) VALUES (
        v_challenge_id, p_student_id, v_hash, 0, 5, false, v_expires_at
    );

    RETURN jsonb_build_object(
        'success', true,
        'challenge_id', v_challenge_id::text,
        'glyphs', v_glyphs,
        'issued_at_ms', floor(extract(epoch from now()) * 1000)::bigint,
        'expires_at_ms', floor(extract(epoch from v_expires_at) * 1000)::bigint,
        'ttl_seconds', 120,
        'max_attempts', 5,
        'attempts_used', 0
    );
END;
$$;

-- 3. Server-Side CAPTCHA Verification & Roll Number Issuance Function (SECURITY DEFINER)
-- Enforces 2-minute expiry, 5-attempt limit, single-use invalidation, and generates roll number ONLY on success
CREATE OR REPLACE FUNCTION public.verify_captcha_and_issue_roll_no(
    p_challenge_id uuid,
    p_user_input text,
    p_student_id text,
    p_student_name text,
    p_course_name text,
    p_class_name text,
    p_date_key text
) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_challenge record;
    v_salt constant text := 'jac_mcq_master_server_captcha_salt_2026';
    v_candidate_hash text;
    v_new_attempts int;
    v_remaining int;
    v_roll_result jsonb;
BEGIN
    SELECT * INTO v_challenge
    FROM public.captcha_challenges
    WHERE id = p_challenge_id AND student_id = p_student_id
    FOR UPDATE;

    IF NOT FOUND OR v_challenge.is_used THEN
        RETURN jsonb_build_object(
            'verified', false,
            'expired', true,
            'attempts_remaining', 0,
            'error', 'CAPTCHA expired or already used. Please refresh CAPTCHA.'
        );
    END IF;

    IF now() > v_challenge.expires_at THEN
        UPDATE public.captcha_challenges SET is_used = true WHERE id = p_challenge_id;
        DELETE FROM public.captcha_challenges WHERE id = p_challenge_id;
        RETURN jsonb_build_object(
            'verified', false,
            'expired', true,
            'attempts_remaining', 0,
            'error', 'CAPTCHA expired after 2 minutes. Please enter the new CAPTCHA.'
        );
    END IF;

    IF v_challenge.attempts_count >= v_challenge.max_attempts THEN
        UPDATE public.captcha_challenges SET is_used = true WHERE id = p_challenge_id;
        DELETE FROM public.captcha_challenges WHERE id = p_challenge_id;
        RETURN jsonb_build_object(
            'verified', false,
            'max_attempts_reached', true,
            'attempts_remaining', 0,
            'error', 'Maximum 5 incorrect attempts reached. Generating a new CAPTCHA.'
        );
    END IF;

    v_candidate_hash := encode(
        digest(p_challenge_id::text || ':' || upper(trim(coalesce(p_user_input, ''))) || ':' || v_salt, 'sha256'),
        'hex'
    );

    IF v_candidate_hash <> v_challenge.answer_hash THEN
        v_new_attempts := v_challenge.attempts_count + 1;
        v_remaining := greatest(0, v_challenge.max_attempts - v_new_attempts);

        IF v_new_attempts >= v_challenge.max_attempts THEN
            DELETE FROM public.captcha_challenges WHERE id = p_challenge_id;
            RETURN jsonb_build_object(
                'verified', false,
                'max_attempts_reached', true,
                'attempts_remaining', 0,
                'error', 'Maximum 5 incorrect attempts reached. Generating a new CAPTCHA.'
            );
        ELSE
            UPDATE public.captcha_challenges
            SET attempts_count = v_new_attempts
            WHERE id = p_challenge_id;

            RETURN jsonb_build_object(
                'verified', false,
                'max_attempts_reached', false,
                'attempts_remaining', v_remaining,
                'error', format('Incorrect CAPTCHA code. %s attempt(s) remaining.', v_remaining)
            );
        END IF;
    END IF;

    -- CAPTCHA Verified! Invalidate & delete challenge immediately so it can never be reused
    DELETE FROM public.captcha_challenges WHERE id = p_challenge_id;

    -- Generate temporary student roll number ONLY after successful CAPTCHA verification
    v_roll_result := public.get_or_create_daily_roll_no(
        p_student_id,
        p_student_name,
        p_course_name,
        p_class_name,
        p_date_key
    );

    RETURN jsonb_build_object(
        'verified', true,
        'roll_no', v_roll_result->>'roll_no',
        'already_attempted', COALESCE((v_roll_result->>'already_attempted')::boolean, false),
        'attempts_remaining', v_challenge.max_attempts - v_challenge.attempts_count
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_captcha_challenge TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.verify_captcha_and_issue_roll_no TO anon, authenticated, service_role;

-- 4. Keep ONLY Best 100 Topper List in Database (Remove all > 100 permanently)
CREATE OR REPLACE FUNCTION public.prune_leaderboard_top_100(
    p_date_key text DEFAULT NULL,
    p_class_name text DEFAULT NULL
) RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_today text := COALESCE(NULLIF(trim(p_date_key), ''), to_char(now() AT TIME ZONE 'Asia/Kolkata', 'YYYY-MM-DD'));
BEGIN
    -- 1. Deduplicate any same-student rows keeping only their best score and fastest time
    DELETE FROM public.leaderboard a USING public.leaderboard b
    WHERE a.date_key = b.date_key
      AND a.student_id = b.student_id
      AND a.id <> b.id
      AND (
          a.score < b.score
          OR (a.score = b.score AND a.completion_time_seconds > b.completion_time_seconds)
          OR (a.score = b.score AND a.completion_time_seconds = b.completion_time_seconds AND a.id > b.id)
      );

    -- 2. Permanently delete any leaderboard entries ranked > 100 per (date_key, class_name)
    WITH ranked_leaderboard AS (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY date_key, COALESCE(class_name, '')
                   ORDER BY score DESC, completion_time_seconds ASC, id ASC
               ) AS rnk
        FROM public.leaderboard
        WHERE date_key = v_today
          AND (p_class_name IS NULL OR trim(p_class_name) = '' OR class_name = p_class_name)
    )
    DELETE FROM public.leaderboard
    WHERE id IN (SELECT id FROM ranked_leaderboard WHERE rnk > 100);

    -- 3. Permanently delete any daily_participants entries ranked > 100 per (date_key, class_name)
    WITH ranked_participants AS (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY date_key, COALESCE(class_name, '')
                   ORDER BY score DESC, id ASC
               ) AS rnk
        FROM public.daily_participants
        WHERE date_key = v_today
          AND has_attempted = true
          AND (p_class_name IS NULL OR trim(p_class_name) = '' OR class_name = p_class_name)
    )
    DELETE FROM public.daily_participants
    WHERE id IN (SELECT id FROM ranked_participants WHERE rnk > 100);

    RETURN true;
END;
$$;

GRANT EXECUTE ON FUNCTION public.prune_leaderboard_top_100 TO anon, authenticated, service_role;

-- Trigger to automatically remove any entries below Top 100 on every insert or update
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

-- Immediately prune any existing rows beyond Top 100
SELECT public.prune_leaderboard_top_100();

NOTIFY pgrst, 'reload schema';
