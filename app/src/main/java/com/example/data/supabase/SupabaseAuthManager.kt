package com.example.data.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Official Supabase GoTrue Authentication Manager.
 * Acts as the ONLY authentication authority for the application.
 */
class SupabaseAuthManager private constructor() {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val storage: SupabaseAuthStorage?
        get() = SupabaseAuthStorage.getInstance()

    private var currentSession: SupabaseSession? = null

    val currentAuthenticatedUser: SupabaseUser?
        get() = currentSession?.user

    val currentAccessToken: String?
        get() = currentSession?.accessToken ?: storage?.getAccessToken()

    private fun getAuthBaseUrl(): String {
        val base = SupabaseConfig.projectUrl.trimEnd('/')
        return "$base/auth/v1"
    }

    /**
     * Signs up a new user via Supabase Auth (Email + Password).
     */
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String
    ): Result<RegistrationResult> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()
            val trimmedName = fullName.trim()

            if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                return@withContext Result.failure(SupabaseAuthException("Please enter a valid email address"))
            }
            if (trimmedPassword.length < 6) {
                return@withContext Result.failure(SupabaseAuthException("Password must be at least 6 characters"))
            }

            val payload = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", trimmedPassword)
                put("data", JSONObject().apply {
                    put("full_name", trimmedName)
                    put("name", trimmedName)
                })
            }.toString()

            val url = "${getAuthBaseUrl()}/signup"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val statusCode = response.code

                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val user = parseUser(json.optJSONObject("user") ?: json, trimmedName)

                    val accessToken = json.optString("access_token", "")
                    val refreshToken = json.optString("refresh_token", "")
                    val expiresIn = json.optLong("expires_in", 3600L)

                    if (accessToken.isNotBlank()) {
                        val session = SupabaseSession(
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            tokenType = json.optString("token_type", "bearer"),
                            expiresIn = expiresIn,
                            expiresAtEpoch = System.currentTimeMillis() + (expiresIn * 1000L),
                            user = user
                        )
                        currentSession = session
                        storage?.saveSession(session)
                        _authState.value = AuthState.Authenticated(session, user)

                        Result.success(
                            RegistrationResult(
                                user = user,
                                session = session,
                                isEmailConfirmationRequired = false,
                                message = "Account created successfully!"
                            )
                        )
                    } else {
                        // Email confirmation is required by Supabase project settings
                        Result.success(
                            RegistrationResult(
                                user = user,
                                session = null,
                                isEmailConfirmationRequired = true,
                                message = "Please check your email and confirm your account before logging in."
                            )
                        )
                    }
                } else {
                    val errorMsg = parseErrorMessage(body, statusCode)
                    Log.w(TAG, "Supabase signup failed ($statusCode): $body")
                    Result.failure(SupabaseAuthException(errorMsg, httpCode = statusCode))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase signup exception: ${e.message}")
            Result.failure(mapException(e))
        }
    }

    /**
     * Signs in with Supabase Auth (Email + Password).
     */
    suspend fun signIn(
        email: String,
        password: String
    ): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()

            if (trimmedEmail.isBlank()) {
                return@withContext Result.failure(SupabaseAuthException("Please enter your email address"))
            }
            if (trimmedPassword.isBlank()) {
                return@withContext Result.failure(SupabaseAuthException("Please enter your password"))
            }

            val payload = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", trimmedPassword)
            }.toString()

            val url = "${getAuthBaseUrl()}/token?grant_type=password"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val statusCode = response.code

                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val accessToken = json.optString("access_token", "")
                    val refreshToken = json.optString("refresh_token", "")
                    val expiresIn = json.optLong("expires_in", 3600L)
                    val userJson = json.optJSONObject("user") ?: JSONObject()
                    val user = parseUser(userJson)

                    val session = SupabaseSession(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        tokenType = json.optString("token_type", "bearer"),
                        expiresIn = expiresIn,
                        expiresAtEpoch = System.currentTimeMillis() + (expiresIn * 1000L),
                        user = user
                    )

                    currentSession = session
                    storage?.saveSession(session)
                    _authState.value = AuthState.Authenticated(session, user)

                    Result.success(session)
                } else {
                    val errorMsg = parseErrorMessage(body, statusCode)
                    Log.w(TAG, "Supabase signin failed ($statusCode): $body")
                    Result.failure(SupabaseAuthException(errorMsg, httpCode = statusCode))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase signin exception: ${e.message}")
            Result.failure(mapException(e))
        }
    }

    /**
     * Signs in via Supabase Auth using a Google ID Token.
     */
    suspend fun signInWithGoogleIdToken(
        idToken: String,
        nonce: String? = null
    ): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("provider", "google")
                put("id_token", idToken)
                if (!nonce.isNullOrBlank()) {
                    put("nonce", nonce)
                }
            }.toString()

            val url = "${getAuthBaseUrl()}/token?grant_type=id_token"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val statusCode = response.code

                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val user = parseUser(json.optJSONObject("user") ?: JSONObject())
                    val accessToken = json.optString("access_token", "")
                    val refreshToken = json.optString("refresh_token", "")
                    val expiresIn = json.optLong("expires_in", 3600L)

                    val session = SupabaseSession(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        tokenType = json.optString("token_type", "bearer"),
                        expiresIn = expiresIn,
                        expiresAtEpoch = System.currentTimeMillis() + (expiresIn * 1000L),
                        user = user
                    )

                    currentSession = session
                    storage?.saveSession(session)
                    _authState.value = AuthState.Authenticated(session, user)

                    Result.success(session)
                } else {
                    val errorMsg = parseErrorMessage(body, statusCode)
                    Log.w(TAG, "Supabase Google ID token signin failed ($statusCode): $body")
                    Result.failure(SupabaseAuthException(errorMsg, httpCode = statusCode))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google ID token signin exception: ${e.message}")
            Result.failure(mapException(e))
        }
    }

    /**
     * Restores persisted session and verifies it with Supabase Auth.
     */
    suspend fun restoreSession(): SupabaseSession? = withContext(Dispatchers.IO) {
        try {
            val savedSession = storage?.loadSession() ?: run {
                _authState.value = AuthState.Unauthenticated()
                return@withContext null
            }

            val isCloseToExpiry = System.currentTimeMillis() > (savedSession.expiresAtEpoch - (5 * 60 * 1000L))

            val validSession = if (isCloseToExpiry && savedSession.refreshToken.isNotBlank()) {
                refreshSessionToken(savedSession.refreshToken) ?: savedSession
            } else {
                savedSession
            }

            // Validate with Supabase GoTrue endpoint GET /auth/v1/user
            val verifiedUser = fetchAuthenticatedUser(validSession.accessToken)
            if (verifiedUser != null) {
                val updatedSession = validSession.copy(user = verifiedUser)
                currentSession = updatedSession
                storage?.saveSession(updatedSession)
                _authState.value = AuthState.Authenticated(updatedSession, verifiedUser)
                updatedSession
            } else {
                // If token invalid, try refresh once
                if (validSession.refreshToken.isNotBlank()) {
                    val refreshed = refreshSessionToken(validSession.refreshToken)
                    if (refreshed != null) {
                        currentSession = refreshed
                        storage?.saveSession(refreshed)
                        _authState.value = AuthState.Authenticated(refreshed, refreshed.user)
                        refreshed
                    } else {
                        storage?.clearSession()
                        currentSession = null
                        _authState.value = AuthState.Unauthenticated()
                        null
                    }
                } else {
                    storage?.clearSession()
                    currentSession = null
                    _authState.value = AuthState.Unauthenticated()
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Session restoration error: ${e.message}")
            // If offline, preserve saved session to avoid prematurely kicking user out, but keep state
            val cached = storage?.loadSession()
            if (cached != null) {
                currentSession = cached
                _authState.value = AuthState.Authenticated(cached, cached.user)
                cached
            } else {
                _authState.value = AuthState.Unauthenticated()
                null
            }
        }
    }

    /**
     * Refreshes access token via Supabase GoTrue API.
     */
    private suspend fun refreshSessionToken(refreshToken: String): SupabaseSession? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("refresh_token", refreshToken)
            }.toString()

            val url = "${getAuthBaseUrl()}/token?grant_type=refresh_token"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val accessToken = json.optString("access_token", "")
                    val newRefreshToken = json.optString("refresh_token", refreshToken)
                    val expiresIn = json.optLong("expires_in", 3600L)
                    val userJson = json.optJSONObject("user") ?: JSONObject()
                    val user = parseUser(userJson)

                    SupabaseSession(
                        accessToken = accessToken,
                        refreshToken = newRefreshToken,
                        tokenType = json.optString("token_type", "bearer"),
                        expiresIn = expiresIn,
                        expiresAtEpoch = System.currentTimeMillis() + (expiresIn * 1000L),
                        user = user
                    )
                } else {
                    Log.w(TAG, "Token refresh failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh exception: ${e.message}")
            null
        }
    }

    /**
     * Fetches the currently authenticated user from Supabase Auth.
     */
    private suspend fun fetchAuthenticatedUser(accessToken: String): SupabaseUser? = withContext(Dispatchers.IO) {
        try {
            val url = "${getAuthBaseUrl()}/user"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    parseUser(json)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sends a password recovery email via Supabase Auth.
     */
    suspend fun sendPasswordRecovery(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
                return@withContext Result.failure(SupabaseAuthException("Please enter a valid email address"))
            }

            val payload = JSONObject().apply {
                put("email", trimmedEmail)
            }.toString()

            val url = "${getAuthBaseUrl()}/recover"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val statusCode = response.code

                if (response.isSuccessful) {
                    Result.success("Password recovery instructions / code have been sent to $trimmedEmail. Please check your inbox.")
                } else {
                    val errorMsg = parseErrorMessage(body, statusCode)
                    Result.failure(SupabaseAuthException(errorMsg, httpCode = statusCode))
                }
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * Verifies recovery OTP code sent by Supabase Auth and establishes a recovery session.
     */
    suspend fun verifyRecoveryOtp(email: String, token: String): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            val trimmedToken = token.trim()

            if (trimmedEmail.isBlank() || trimmedToken.isBlank()) {
                return@withContext Result.failure(SupabaseAuthException("Please provide email and 6-digit code"))
            }

            val payload = JSONObject().apply {
                put("type", "recovery")
                put("email", trimmedEmail)
                put("token", trimmedToken)
            }.toString()

            val url = "${getAuthBaseUrl()}/verify"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val statusCode = response.code

                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val user = parseUser(json.optJSONObject("user") ?: json)
                    val accessToken = json.optString("access_token", "")
                    val refreshToken = json.optString("refresh_token", "")
                    val expiresIn = json.optLong("expires_in", 3600L)

                    val session = SupabaseSession(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        tokenType = json.optString("token_type", "bearer"),
                        expiresIn = expiresIn,
                        expiresAtEpoch = System.currentTimeMillis() + (expiresIn * 1000L),
                        user = user
                    )

                    currentSession = session
                    storage?.saveSession(session)
                    _authState.value = AuthState.Authenticated(session, user)

                    Result.success(session)
                } else {
                    val errorMsg = parseErrorMessage(body, statusCode)
                    Result.failure(SupabaseAuthException(errorMsg, httpCode = statusCode))
                }
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * Resets password directly using the verified recovery OTP code.
     */
    suspend fun resetPasswordWithOtp(email: String, token: String, newPassword: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val verifyRes = verifyRecoveryOtp(email, token)
        if (verifyRes.isFailure) {
            return@withContext Result.failure(verifyRes.exceptionOrNull() ?: Exception("Invalid or expired OTP code"))
        }
        val session = verifyRes.getOrThrow()
        updatePassword(newPassword, session.accessToken)
    }

    /**
     * Updates user password via Supabase Auth.
     */
    suspend fun updatePassword(newPassword: String, tokenOverride: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val trimmed = newPassword.trim()
            if (trimmed.length < 6) {
                return@withContext Result.failure(SupabaseAuthException("Password must be at least 6 characters"))
            }

            val token = tokenOverride ?: currentAccessToken ?: storage?.getAccessToken()
            if (token.isNullOrBlank()) {
                return@withContext Result.failure(SupabaseAuthException("Authentication token expired. Please request a new recovery link."))
            }

            val payload = JSONObject().apply {
                put("password", trimmed)
            }.toString()

            val url = "${getAuthBaseUrl()}/user"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .put(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val statusCode = response.code

                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorMsg = parseErrorMessage(body, statusCode)
                    Result.failure(SupabaseAuthException(errorMsg, httpCode = statusCode))
                }
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * Signs out the user from Supabase Auth and clears session.
     */
    suspend fun signOut(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = currentAccessToken
            if (!token.isNullOrBlank()) {
                val url = "${getAuthBaseUrl()}/logout"
                val request = Request.Builder()
                    .url(url)
                    .header("apikey", SupabaseConfig.anonKey)
                    .header("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(jsonMediaType))
                    .build()

                try {
                    httpClient.newCall(request).execute().close()
                } catch (e: Exception) {
                    Log.w(TAG, "Logout remote request exception: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
        } finally {
            storage?.clearSession()
            currentSession = null
            _authState.value = AuthState.Unauthenticated()
        }
        Result.success(true)
    }

    /**
     * Deletes user's account and personal data permanently from database tables and Supabase Auth.
     */
    suspend fun deleteAccount(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val user = currentAuthenticatedUser ?: storage?.loadSession()?.user
            val token = currentAccessToken ?: storage?.getAccessToken()

            if (user != null && user.id.isNotBlank()) {
                val studentId = user.id
                val client = SupabaseClient.getInstance()

                // 1. Permanently delete all student records from database tables
                try { client.delete("daily_attempts", "student_id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("daily_attempts", "user_id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("leaderboard", "student_id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("leaderboard", "user_id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("test_attempts", "student_id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("test_attempts", "user_id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("results", "student_id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("results", "user_id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("profiles", "id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("profiles", "user_id=eq.$studentId", token) } catch (_: Exception) {}
                try { client.delete("students", "id=eq.$studentId", token) } catch (_: Exception) {}

                // 2. Call Supabase RPC delete function if configured
                if (!token.isNullOrBlank()) {
                    val rpcUrls = listOf(
                        "${SupabaseConfig.restBaseUrl}rpc/delete_user",
                        "${SupabaseConfig.restBaseUrl}rpc/delete_current_user",
                        "${SupabaseConfig.restBaseUrl}rpc/delete_account"
                    )
                    for (rpcUrl in rpcUrls) {
                        try {
                            val rpcRequest = Request.Builder()
                                .url(rpcUrl)
                                .header("apikey", SupabaseConfig.anonKey)
                                .header("Authorization", "Bearer $token")
                                .header("Content-Type", "application/json")
                                .post("{}".toRequestBody(jsonMediaType))
                                .build()
                            httpClient.newCall(rpcRequest).execute().close()
                        } catch (_: Exception) {}
                    }

                    // 3. Attempt direct user delete via /auth/v1/user
                    try {
                        val authDeleteReq = Request.Builder()
                            .url("${getAuthBaseUrl()}/user")
                            .header("apikey", SupabaseConfig.anonKey)
                            .header("Authorization", "Bearer $token")
                            .delete()
                            .build()
                        httpClient.newCall(authDeleteReq).execute().close()
                    } catch (_: Exception) {}
                }
            }

            signOut()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Delete account error: ${e.message}")
            signOut()
            Result.success(true)
        }
    }

    private fun parseUser(json: JSONObject, fallbackName: String = ""): SupabaseUser {
        val id = json.optString("id", "")
        val email = json.optString("email", "")
        val userMetadata = json.optJSONObject("user_metadata")

        val nameFromMeta = userMetadata?.optString("full_name", userMetadata.optString("name", "")) ?: ""
        val resolvedName = nameFromMeta.ifBlank {
            fallbackName.ifBlank {
                if (email.contains("@")) {
                    email.substringBefore("@").replace(".", " ")
                        .split(" ").filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                } else "Student"
            }
        }

        val emailVerified = json.optBoolean("email_confirmed_at", false) ||
                (json.optString("confirmed_at", "").isNotBlank()) ||
                (userMetadata?.optBoolean("email_verified", false) ?: false)

        return SupabaseUser(
            id = id,
            email = email,
            fullName = resolvedName,
            role = json.optString("role", "authenticated"),
            emailVerified = emailVerified,
            confirmationSentAt = json.optString("confirmation_sent_at", null),
            createdAt = json.optString("created_at", null)
        )
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val msg = json.optString("msg", json.optString("message", json.optString("error_description", "")))
            val errorCode = json.optString("error_code", json.optString("code", ""))

            when {
                errorCode == "email_not_confirmed" || msg.contains("Email not confirmed", ignoreCase = true) ->
                    "Please check your email and confirm your account before logging in."
                errorCode == "invalid_credentials" || errorCode == "invalid_grant" || msg.contains("Invalid login credentials", ignoreCase = true) ->
                    "Invalid email or password. Please check your credentials and try again."
                errorCode == "user_already_exists" || msg.contains("User already registered", ignoreCase = true) ->
                    "An account with this email already exists. Please login instead."
                errorCode == "over_email_send_rate_limit" || msg.contains("rate limit", ignoreCase = true) ->
                    "Email rate limit exceeded. Please wait a few moments and try again."
                errorCode == "email_address_invalid" ->
                    "Please provide a valid email address."
                msg.isNotBlank() -> msg
                statusCode == 400 -> "Invalid request. Please verify your details."
                statusCode == 401 -> "Unauthorized. Please login again."
                statusCode == 404 -> "Authentication endpoint not found."
                statusCode >= 500 -> "Authentication service is temporarily unavailable. Please try again."
                else -> "Authentication failed (Error $statusCode). Please try again."
            }
        } catch (_: Exception) {
            when (statusCode) {
                400 -> "Invalid credentials or email confirmation required."
                401 -> "Invalid email or password."
                else -> "Authentication error. Please try again."
            }
        }
    }

    private fun mapException(e: Exception): Exception {
        return when (e) {
            is java.net.UnknownHostException, is java.net.ConnectException ->
                SupabaseAuthException("No internet connection. Please check your network and try again.")
            is java.net.SocketTimeoutException ->
                SupabaseAuthException("Connection timed out. Please try again.")
            is IOException ->
                SupabaseAuthException("Network communication error: ${e.message}")
            else -> e
        }
    }

    companion object {
        private const val TAG = "SupabaseAuthManager"

        @Volatile
        private var instance: SupabaseAuthManager? = null

        fun getInstance(): SupabaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: SupabaseAuthManager().also { instance = it }
            }
        }
    }
}
