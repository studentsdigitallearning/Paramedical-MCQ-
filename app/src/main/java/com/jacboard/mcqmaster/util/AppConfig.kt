package com.jacboard.mcqmaster.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object AppConfig {
    const val APP_NAME = "JAC MCQ Master"
    const val APP_VERSION = "1.0.0"
    const val APP_VERSION_CODE = "1"
    const val APP_TAGLINE = "Learn • Practice • Improve"
    const val EDUCATIONAL_PURPOSE = "Comprehensive MCQ practice, mock tests, and daily challenges for CBSE & State Board students of Class 8 to 12."
    const val APP_DESCRIPTION = "JAC MCQ Master is an intuitive learning platform designed to help school students master multiple-choice questions across Mathematics, Science, Social Studies, English, and Hindi. With detailed analytics, streak rewards, and daily challenges, students achieve academic excellence through regular practice."

    // Configured URLs dynamically from Supabase app_config
    var privacyPolicyUrl: String? = null
    var termsConditionsUrl: String? = null
    var storeUrl: String? = null // Opens Google Play Store dynamically via package name if null
    var supportEmail: String = ""
    var websiteUrl: String = ""
    var telegramUrl: String = ""
    var whatsappUrl: String = ""
    var youtubeUrl: String = ""
    var instagramUrl: String = ""
    var adminPortalUrl: String = ""

    private fun normalizeUrl(raw: String?): String? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    /**
     * Dynamically updates app config and sidebar links from Admin Panel / Remote Config key-value map.
     */
    fun updateFromRemoteMap(configMap: Map<String, String>) {
        val privacyCandidate = configMap["privacy_policy_url"]
            ?: configMap["privacy_policy"]
            ?: configMap["privacy_url"]
            ?: configMap["privacy_link"]
            ?: configMap["privacyPolicyUrl"]
        privacyCandidate?.takeIf { it.isNotBlank() }?.let { privacyPolicyUrl = normalizeUrl(it) }

        val termsCandidate = configMap["terms_conditions_url"]
            ?: configMap["terms_and_conditions_url"]
            ?: configMap["terms_conditions"]
            ?: configMap["terms_and_conditions"]
            ?: configMap["terms_url"]
            ?: configMap["terms_link"]
            ?: configMap["termsConditionsUrl"]
        termsCandidate?.takeIf { it.isNotBlank() }?.let { termsConditionsUrl = normalizeUrl(it) }

        configMap["store_url"]?.takeIf { it.isNotBlank() }?.let { storeUrl = normalizeUrl(it) }
        configMap["support_email"]?.takeIf { it.isNotBlank() }?.let { supportEmail = it.trim() }
        configMap["website_url"]?.takeIf { it.isNotBlank() }?.let { websiteUrl = normalizeUrl(it) ?: "" }
        configMap["telegram_url"]?.takeIf { it.isNotBlank() }?.let { telegramUrl = normalizeUrl(it) ?: "" }
        configMap["whatsapp_url"]?.takeIf { it.isNotBlank() }?.let { whatsappUrl = normalizeUrl(it) ?: "" }
        configMap["youtube_url"]?.takeIf { it.isNotBlank() }?.let { youtubeUrl = normalizeUrl(it) ?: "" }
        configMap["instagram_url"]?.takeIf { it.isNotBlank() }?.let { instagramUrl = normalizeUrl(it) ?: "" }
        configMap["admin_portal_url"]?.takeIf { it.isNotBlank() }?.let { adminPortalUrl = normalizeUrl(it) ?: "" }
    }

    /**
     * Opens the Privacy Policy URL in the device's default external browser.
     * Shows a message if URL has not yet been configured.
     */
    fun openPrivacyPolicy(context: Context) {
        val url = normalizeUrl(privacyPolicyUrl)
        if (url.isNullOrEmpty()) {
            Toast.makeText(context, "Privacy Policy link is not configured yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open browser on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the Terms & Conditions URL in the device's default external browser.
     * Shows a message if URL has not yet been configured.
     */
    fun openTermsConditions(context: Context) {
        val url = normalizeUrl(termsConditionsUrl)
        if (url.isNullOrEmpty()) {
            Toast.makeText(context, "Terms & Conditions link is not configured yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open browser on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the app store rating page using the device's external handler.
     * Gracefully handles unconfigured/unavailable store URLs.
     */
    fun rateApp(context: Context) {
        val customStoreUrl = storeUrl?.trim()
        if (!customStoreUrl.isNullOrEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(customStoreUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // fallback to market package
            }
        }

        val packageName = context.packageName
        val marketUri = Uri.parse("market://details?id=$packageName")
        val marketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            try {
                val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "App store rating is not available right now.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Opens the native Android share sheet with configured app name and store link.
     */
    fun shareApp(context: Context) {
        val link = if (!storeUrl.isNullOrEmpty()) storeUrl else websiteUrl
        val shareMessage = "Download $APP_NAME and start practicing MCQs, tests and daily challenges.\n\n$link"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            putExtra(Intent.EXTRA_SUBJECT, "$APP_NAME — Learn • Practice • Improve")
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Share $APP_NAME via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooserIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open share sheet.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches the default email client with support email.
     */
    fun sendSupportEmail(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$supportEmail")
            putExtra(Intent.EXTRA_SUBJECT, "Support Query — $APP_NAME")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Email support: $supportEmail", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Opens the official website in external browser.
     */
    fun openWebsite(context: Context) {
        val url = websiteUrl.trim()
        if (url.isEmpty()) {
            Toast.makeText(context, "Website URL is not configured yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open browser on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens Telegram channel / group link in browser or Telegram app.
     */
    fun openTelegramChannel(context: Context) {
        val url = telegramUrl.trim()
        if (url.isEmpty()) {
            Toast.makeText(context, "Telegram link is not configured yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Telegram: $url", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Opens WhatsApp support link in browser or WhatsApp app.
     */
    fun openWhatsAppSupport(context: Context) {
        val url = whatsappUrl.trim()
        if (url.isEmpty()) {
            Toast.makeText(context, "WhatsApp support is not configured yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp: $url", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Opens Admin Portal link in external browser.
     */
    fun openAdminPortal(context: Context) {
        val url = adminPortalUrl.trim()
        if (url.isEmpty()) {
            Toast.makeText(context, "Admin Portal link is not configured yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open browser on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openYouTubeChannel(context: Context) {
        val url = youtubeUrl.trim()
        if (url.isEmpty()) {
            Toast.makeText(context, "YouTube link is not configured yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "YouTube: $url", Toast.LENGTH_LONG).show()
        }
    }

    fun openInstagramUpdates(context: Context) {
        val url = instagramUrl.trim()
        if (url.isEmpty()) {
            Toast.makeText(context, "Instagram link is not configured yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Instagram: $url", Toast.LENGTH_LONG).show()
        }
    }
}
