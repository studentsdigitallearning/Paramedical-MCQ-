// Supabase Edge Function: Secure 6-Character Alphanumeric Text CAPTCHA & Roll Number Generator
// Endpoint: /functions/v1/captcha
// Protects JAC Board MCQ Master Daily Test registration from bots and automated duplicate requests.

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const CAPTCHA_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const CAPTCHA_LENGTH = 6;
const CAPTCHA_TTL_MS = 2 * 60 * 1000; // 2 minutes
const MAX_ATTEMPTS = 5;

async function sha256Hex(input: string): Promise<string> {
  const data = new TextEncoder().encode(input);
  const hashBuffer = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

function generateSecureCode(length = CAPTCHA_LENGTH): string {
  const randomBytes = new Uint8Array(length);
  crypto.getRandomValues(randomBytes);
  let code = "";
  for (let i = 0; i < length; i++) {
    code += CAPTCHA_ALPHABET[randomBytes[i] % CAPTCHA_ALPHABET.length];
  }
  return code;
}

function buildVisualGlyphs(code: string) {
  const rand = new Uint8Array(code.length * 3);
  crypto.getRandomValues(rand);
  return Array.from(code).map((ch, idx) => ({
    code_point: ch.charCodeAt(0),
    rotation_deg: ((rand[idx * 3] % 33) - 16),
    offset_y: ((rand[idx * 3 + 1] % 13) - 6),
    scale: 0.92 + (rand[idx * 3 + 2] % 18) / 100.0,
  }));
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const captchaSecret = Deno.env.get("CAPTCHA_SECRET_SALT") ?? serviceRoleKey;

    const supabase = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false },
    });

    const body = await req.json();
    const action = (body.action ?? "generate").toString().toLowerCase();
    const studentId = (body.student_id ?? "").toString().replace(/[^a-zA-Z0-9_\-]/g, "");

    if (!studentId) {
      return new Response(
        JSON.stringify({ success: false, error: "Invalid student identifier." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (action === "generate") {
      // Rate limit: invalidate any previous active challenges for this student
      await supabase
        .from("captcha_challenges")
        .update({ is_used: true })
        .eq("student_id", studentId)
        .eq("is_used", false);

      const code = generateSecureCode(CAPTCHA_LENGTH);
      const challengeId = crypto.randomUUID();
      const nowMs = Date.now();
      const expiresAtMs = nowMs + CAPTCHA_TTL_MS;
      const answerHash = await sha256Hex(`${challengeId}:${code.toUpperCase()}:${captchaSecret}`);

      const { error: insertErr } = await supabase.from("captcha_challenges").insert({
        id: challengeId,
        student_id: studentId,
        answer_hash: answerHash,
        attempts_count: 0,
        max_attempts: MAX_ATTEMPTS,
        is_used: false,
        expires_at: new Date(expiresAtMs).toISOString(),
      });

      if (insertErr) {
        throw insertErr;
      }

      return new Response(
        JSON.stringify({
          success: true,
          challenge_id: challengeId,
          glyphs: buildVisualGlyphs(code),
          issued_at_ms: nowMs,
          expires_at_ms: expiresAtMs,
          ttl_seconds: 120,
          max_attempts: MAX_ATTEMPTS,
          attempts_used: 0,
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (action === "verify") {
      const challengeId = (body.challenge_id ?? "").toString().trim();
      const userInput = (body.user_input ?? "").toString().trim().toUpperCase();
      const studentName = (body.student_name ?? "Student").toString().trim().slice(0, 50);
      const courseName = (body.course_name ?? "").toString().trim().slice(0, 50);
      const className = (body.class_name ?? "Class 10").toString().trim().slice(0, 50);
      const dateKey = (body.date_key ?? "").toString().trim();

      if (!challengeId || !userInput) {
        return new Response(
          JSON.stringify({
            verified: false,
            error: "Please enter the 6-character CAPTCHA code.",
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const { data: challenge, error: fetchErr } = await supabase
        .from("captcha_challenges")
        .select("*")
        .eq("id", challengeId)
        .eq("student_id", studentId)
        .single();

      if (fetchErr || !challenge) {
        return new Response(
          JSON.stringify({
            verified: false,
            expired: true,
            error: "CAPTCHA session not found or already invalidated. Please refresh.",
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      if (challenge.is_used) {
        return new Response(
          JSON.stringify({
            verified: false,
            expired: true,
            error: "This CAPTCHA has already been used or invalidated. Please refresh.",
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const expiresAtMs = new Date(challenge.expires_at).getTime();
      if (Date.now() > expiresAtMs) {
        await supabase
          .from("captcha_challenges")
          .update({ is_used: true })
          .eq("id", challengeId);

        return new Response(
          JSON.stringify({
            verified: false,
            expired: true,
            attempts_remaining: 0,
            error: "CAPTCHA expired (2-minute limit). Please try the new CAPTCHA.",
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      if (challenge.attempts_count >= challenge.max_attempts) {
        await supabase
          .from("captcha_challenges")
          .update({ is_used: true })
          .eq("id", challengeId);

        return new Response(
          JSON.stringify({
            verified: false,
            max_attempts_reached: true,
            attempts_remaining: 0,
            error: "Maximum 5 incorrect attempts reached. Generating a new CAPTCHA.",
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const candidateHash = await sha256Hex(`${challengeId}:${userInput}:${captchaSecret}`);
      if (candidateHash !== challenge.answer_hash) {
        const newAttempts = (challenge.attempts_count ?? 0) + 1;
        const limitReached = newAttempts >= MAX_ATTEMPTS;

        await supabase
          .from("captcha_challenges")
          .update({
            attempts_count: newAttempts,
            is_used: limitReached,
          })
          .eq("id", challengeId);

        const remaining = Math.max(0, MAX_ATTEMPTS - newAttempts);
        return new Response(
          JSON.stringify({
            verified: false,
            max_attempts_reached: limitReached,
            attempts_remaining: remaining,
            error: limitReached
              ? "Maximum 5 incorrect attempts reached. A new CAPTCHA has been loaded."
              : `Incorrect CAPTCHA code. ${remaining} attempt${remaining === 1 ? "" : "s"} remaining.`,
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      // CAPTCHA matched! Immediately invalidate challenge so it cannot be reused
      await supabase
        .from("captcha_challenges")
        .update({ is_used: true, verified_at: new Date().toISOString() })
        .eq("id", challengeId);

      // Generate temporary student roll number ONLY after successful CAPTCHA verification
      const { data: rollData, error: rollErr } = await supabase.rpc(
        "get_or_create_daily_roll_no",
        {
          p_student_id: studentId,
          p_student_name: studentName,
          p_course_name: courseName,
          p_class_name: className,
          p_date_key: dateKey,
        }
      );

      if (rollErr) {
        throw rollErr;
      }

      const issuedRollNo = rollData?.roll_no ?? "";
      return new Response(
        JSON.stringify({
          verified: true,
          roll_no: issuedRollNo,
          already_attempted: rollData?.already_attempted ?? false,
          attempts_remaining: MAX_ATTEMPTS - (challenge.attempts_count ?? 0),
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({ success: false, error: "Unsupported action." }),
      { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, error: err instanceof Error ? err.message : "Server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
