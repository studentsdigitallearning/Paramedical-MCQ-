package com.example.data.supabase

/**
 * Data models for Supabase GoTrue Authentication.
 */
data class SupabaseUser(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String = "authenticated",
    val emailVerified: Boolean = false,
    val confirmationSentAt: String? = null,
    val createdAt: String? = null,
    val userMetadata: Map<String, Any> = emptyMap()
)

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "bearer",
    val expiresIn: Long = 3600,
    val expiresAtEpoch: Long = System.currentTimeMillis() + 3600 * 1000L,
    val user: SupabaseUser
)

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val session: SupabaseSession, val user: SupabaseUser) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
}

data class RegistrationResult(
    val user: SupabaseUser?,
    val session: SupabaseSession?,
    val isEmailConfirmationRequired: Boolean,
    val message: String
)

class SupabaseAuthException(
    override val message: String,
    val errorCode: String? = null,
    val httpCode: Int = 400
) : Exception(message)
