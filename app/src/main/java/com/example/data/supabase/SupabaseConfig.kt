package com.example.data.supabase

import com.example.BuildConfig

/**
 * Supabase configuration for the MCQ Student App.
 *
 * Credentials are provided via BuildConfig from .env / Secrets panel.
 * Only the public anon/publishable key is used client-side (safe for Android APK).
 * No service-role or admin secrets are bundled in the application.
 */
object SupabaseConfig {

    val projectUrl: String
        get() = BuildConfig.SUPABASE_URL.takeIf { it.isNotBlank() }?.trimEnd('/')
            ?: "https://zjortvgjiuimqvgxmpbn.supabase.co"

    val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.takeIf { it.isNotBlank() }
            ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inpqb3J0dmdqaXVpbXF2Z3htcGJuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEzNjY1MjQsImV4cCI6MjEwNjk0MjUyNH0.igAaxxH9fblPPmzyM044UgEiopMXStEjmgt1SQ_rLbg"

    val isConfigured: Boolean
        get() = projectUrl.isNotBlank() && anonKey.isNotBlank()

    val restBaseUrl: String
        get() {
            val base = projectUrl.trimEnd('/')
            return if (base.isNotBlank()) "$base/rest/v1/" else ""
        }
}


