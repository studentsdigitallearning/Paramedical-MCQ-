package com.jacboard.mcqmaster.data.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Lightweight HTTP client for Supabase PostgREST API.
 * Handles authentication headers, timeouts, and asynchronous execution.
 */
class SupabaseClient private constructor() {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Executes a GET request against the Supabase PostgREST endpoint.
     */
    suspend fun get(tableOrEndpoint: String, queryParams: String = ""): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY in Secrets or .env"))
        }
        try {
            val url = buildUrl(tableOrEndpoint, queryParams)
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Log.w(TAG, "GET $tableOrEndpoint failed with HTTP ${response.code}: $body")
                    Result.failure(IOException("Supabase HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "GET $tableOrEndpoint exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Executes a POST request against the Supabase PostgREST endpoint (e.g. insert rows).
     */
    suspend fun post(
        tableOrEndpoint: String,
        jsonPayload: String,
        prefer: String = "return=representation"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY in Secrets or .env"))
        }
        try {
            val url = buildUrl(tableOrEndpoint, "")
            val requestBody = jsonPayload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Prefer", prefer)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Log.w(TAG, "POST $tableOrEndpoint failed with HTTP ${response.code}: $body")
                    Result.failure(IOException("Supabase HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "POST $tableOrEndpoint exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Executes a PATCH request against the Supabase PostgREST endpoint (e.g. update rows).
     */
    suspend fun patch(
        tableOrEndpoint: String,
        queryParams: String,
        jsonPayload: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY in Secrets or .env"))
        }
        try {
            val url = buildUrl(tableOrEndpoint, queryParams)
            val requestBody = jsonPayload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .patch(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Log.w(TAG, "PATCH $tableOrEndpoint failed with HTTP ${response.code}: $body")
                    Result.failure(IOException("Supabase HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PATCH $tableOrEndpoint exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Executes a DELETE request against the Supabase PostgREST endpoint (e.g. remove user data rows).
     */
    suspend fun delete(
        tableOrEndpoint: String,
        queryParams: String,
        userToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY in Secrets or .env"))
        }

        try {
            val url = buildUrl(tableOrEndpoint, queryParams)
            val authHeader = if (!userToken.isNullOrBlank()) "Bearer $userToken" else "Bearer ${SupabaseConfig.anonKey}"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Log.w(TAG, "DELETE $tableOrEndpoint failed with HTTP ${response.code}: $body")
                    Result.failure(IOException("Supabase HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DELETE $tableOrEndpoint exception: ${e.message}")
            Result.failure(e)
        }
    }

    private fun buildUrl(tableOrEndpoint: String, queryParams: String): String {
        val base = SupabaseConfig.restBaseUrl
        val cleanEndpoint = tableOrEndpoint.trimStart('/')
        val encodedParams = if (queryParams.isBlank()) "" else queryParams.replace(" ", "%20")
        return if (encodedParams.isBlank()) {
            "$base$cleanEndpoint"
        } else {
            val separator = if (cleanEndpoint.contains("?")) "&" else "?"
            "$base$cleanEndpoint$separator$encodedParams"
        }
    }

    companion object {
        private const val TAG = "SupabaseClient"

        @Volatile
        private var instance: SupabaseClient? = null

        fun getInstance(): SupabaseClient {
            return instance ?: synchronized(this) {
                instance ?: SupabaseClient().also { instance = it }
            }
        }
    }
}
