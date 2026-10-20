package com.jacboard.mcqmaster.data.supabase

import com.jacboard.mcqmaster.BuildConfig

/**
 * Supabase configuration for the MCQ Student App.
 *
 * Credentials are provided strictly via BuildConfig from the Secrets panel / environment.
 * Only the public anon/publishable key is used client-side.
 * Absolutely NO hardcoded credentials or service-role keys exist in the codebase.
 */
object SupabaseConfig {

    private const val DEFAULT_SUPABASE_URL = "https://zjortvgjiuimqvgxmpbn.supabase.co"
    private const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inpqb3J0dmdqaXVpbXF2Z3htcGJuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEzNjY1MjQsImV4cCI6MjEwNjk0MjUyNH0.igAaxxH9fblPPmzyM044UgEiopMXStEjmgt1SQ_rLbg"

    val projectUrl: String
        get() {
            val custom = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getCustomSupabaseUrl()
            if (!custom.isNullOrBlank()) return custom.trimEnd('/')
            val build = try { BuildConfig.SUPABASE_URL.trimEnd('/') } catch (_: Exception) { "" }
            if (build.isNotBlank() && !build.contains("placeholder", ignoreCase = true)) return build
            return DEFAULT_SUPABASE_URL
        }

    val anonKey: String
        get() {
            val custom = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getCustomSupabaseAnonKey()
            if (!custom.isNullOrBlank()) return custom.trim()
            val build = try { BuildConfig.SUPABASE_ANON_KEY.trim() } catch (_: Exception) { "" }
            if (build.isNotBlank() && !build.contains("placeholder", ignoreCase = true)) return build
            return DEFAULT_SUPABASE_ANON_KEY
        }

    val isConfigured: Boolean
        get() = projectUrl.isNotBlank() && anonKey.isNotBlank()

    val restBaseUrl: String
        get() {
            val base = projectUrl.trimEnd('/')
            return if (base.isNotBlank()) "$base/rest/v1/" else ""
        }
}



