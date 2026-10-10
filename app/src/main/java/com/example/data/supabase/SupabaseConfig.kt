package com.example.data.supabase

import com.example.BuildConfig

/**
 * Supabase configuration for the MCQ Student App.
 *
 * Credentials are provided strictly via BuildConfig from the Secrets panel / environment.
 * Only the public anon/publishable key is used client-side.
 * Absolutely NO hardcoded credentials or service-role keys exist in the codebase.
 */
object SupabaseConfig {

    val projectUrl: String
        get() = BuildConfig.SUPABASE_URL.trimEnd('/')

    val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY

    val isConfigured: Boolean
        get() = projectUrl.isNotBlank() && anonKey.isNotBlank()

    val restBaseUrl: String
        get() {
            val base = projectUrl.trimEnd('/')
            return if (base.isNotBlank()) "$base/rest/v1/" else ""
        }
}



