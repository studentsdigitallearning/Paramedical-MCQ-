package com.example.util

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

    // Configured URLs dynamically
    var privacyPolicyUrl: String? = null
    var termsConditionsUrl: String? = null
    var storeUrl: String? = null // Opens Google Play Store dynamically via package name if null
    var supportEmail: String = "support@example.com"
    var websiteUrl: String = ""
    var helplineNumber: String = ""
    var telegramUrl: String = ""
    var whatsappUrl: String = ""
    var adminPortalUrl: String = ""

    /**
     * Dynamically updates app config and sidebar links from Admin Panel / Remote Config key-value map.
     */
    fun updateFromRemoteMap(configMap: Map<String, String>) {
        configMap["privacy_policy_url"]?.takeIf { it.isNotBlank() }?.let { privacyPolicyUrl = it }
        configMap["terms_conditions_url"]?.takeIf { it.isNotBlank() }?.let { termsConditionsUrl = it }
        configMap["store_url"]?.takeIf { it.isNotBlank() }?.let { storeUrl = it }
        configMap["support_email"]?.takeIf { it.isNotBlank() }?.let { supportEmail = it }
        configMap["website_url"]?.takeIf { it.isNotBlank() }?.let { websiteUrl = it }
        configMap["helpline_number"]?.takeIf { it.isNotBlank() }?.let { helplineNumber = it }
        configMap["telegram_url"]?.takeIf { it.isNotBlank() }?.let { telegramUrl = it }
        configMap["whatsapp_url"]?.takeIf { it.isNotBlank() }?.let { whatsappUrl = it }
        configMap["admin_portal_url"]?.takeIf { it.isNotBlank() }?.let { adminPortalUrl = it }
    }

    /**
     * Opens the Privacy Policy URL in the device's default external browser.
     * Shows a message if URL has not yet been configured.
     */
    fun openPrivacyPolicy(context: Context) {
        val url = privacyPolicyUrl?.trim()
        if (url.isNullOrEmpty()) {
            Toast.makeText(context, "Privacy Policy URL is not configured yet.", Toast.LENGTH_SHORT).show()
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
        val url = termsConditionsUrl?.trim()
        if (url.isNullOrEmpty()) {
            Toast.makeText(context, "Terms & Conditions URL is not configured yet.", Toast.LENGTH_SHORT).show()
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
}
