package com.example.model

enum class BannerActionType {
    OPEN_DAILY_TEST,
    OPEN_TEST,
    OPEN_INTERNAL_PAGE,
    OPEN_WEBSITE,
    OPEN_VIDEO,
    NONE
}

data class PromotionalBanner(
    val id: String,
    val isDailyChallenge: Boolean = false,
    val badgeText: String,
    val metaText: String,
    val title: String,
    val description: String,
    val buttonText: String? = null,
    val actionType: BannerActionType = BannerActionType.NONE,
    val targetId: String? = null,
    val url: String? = null
)
