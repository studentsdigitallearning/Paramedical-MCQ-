package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

/**
 * Handles Google Sign-In via Android Jetpack Credential Manager.
 */
object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"

    // Default Web Client ID (Can be configured or injected via BuildConfig)
    private const val DEFAULT_WEB_CLIENT_ID = "552398506161-0qgr2vj6s2499dpsjqu0p9h4j21m76a0.apps.googleusercontent.com"

    suspend fun initiateGoogleSignIn(
        context: Context,
        serverClientId: String? = null
    ): Result<GoogleSignInAccountData> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val clientId = serverClientId?.takeIf { it.isNotBlank() } ?: DEFAULT_WEB_CLIENT_ID

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val accountData = GoogleSignInAccountData(
                    idToken = googleIdTokenCredential.idToken,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.givenName ?: "Student",
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                    nonce = hashedNonce
                )
                Result.success(accountData)
            } else {
                Result.failure(Exception("Unsupported credential type: ${credential::class.java.simpleName}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google Sign-In was cancelled"))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In Credential Manager error: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Google Sign-In failed"))
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

data class GoogleSignInAccountData(
    val idToken: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val nonce: String?
)
