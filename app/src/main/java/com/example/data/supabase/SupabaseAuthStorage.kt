package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Storage for Supabase Auth session tokens (access_token, refresh_token, user metadata).
 * Supabase Auth remains the sole authority: tokens are verified and refreshed against Supabase GoTrue.
 */
class SupabaseAuthStorage private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveSession(session: SupabaseSession) {
        try {
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, session.accessToken)
                .putString(KEY_REFRESH_TOKEN, session.refreshToken)
                .putString(KEY_TOKEN_TYPE, session.tokenType)
                .putLong(KEY_EXPIRES_IN, session.expiresIn)
                .putLong(KEY_EXPIRES_AT, session.expiresAtEpoch)
                .putString(KEY_USER_ID, session.user.id)
                .putString(KEY_USER_EMAIL, session.user.email)
                .putString(KEY_USER_FULL_NAME, session.user.fullName)
                .putBoolean(KEY_EMAIL_VERIFIED, session.user.emailVerified)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Supabase session: ${e.message}")
        }
    }

    fun loadSession(): SupabaseSession? {
        try {
            val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
            val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: ""
            val tokenType = prefs.getString(KEY_TOKEN_TYPE, "bearer") ?: "bearer"
            val expiresIn = prefs.getLong(KEY_EXPIRES_IN, 3600L)
            val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
            val userId = prefs.getString(KEY_USER_ID, null) ?: return null
            val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
            val fullName = prefs.getString(KEY_USER_FULL_NAME, "") ?: ""
            val emailVerified = prefs.getBoolean(KEY_EMAIL_VERIFIED, false)

            val user = SupabaseUser(
                id = userId,
                email = email,
                fullName = fullName,
                emailVerified = emailVerified
            )

            return SupabaseSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = tokenType,
                expiresIn = expiresIn,
                expiresAtEpoch = expiresAt,
                user = user
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading Supabase session: ${e.message}")
            return null
        }
    }

    fun clearSession() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing Supabase session: ${e.message}")
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    companion object {
        private const val TAG = "SupabaseAuthStorage"
        private const val PREFS_NAME = "supabase_auth_session_prefs"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_EXPIRES_AT = "expires_at_epoch"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_EMAIL_VERIFIED = "email_verified"

        @Volatile
        private var instance: SupabaseAuthStorage? = null

        fun init(context: Context): SupabaseAuthStorage {
            return instance ?: synchronized(this) {
                instance ?: SupabaseAuthStorage(context).also { instance = it }
            }
        }

        fun getInstance(): SupabaseAuthStorage? = instance
    }
}
