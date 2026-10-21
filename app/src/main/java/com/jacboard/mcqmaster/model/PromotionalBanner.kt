package com.jacboard.mcqmaster.model

enum class BannerActionType {
    OPEN_DAILY_TEST,
    OPEN_TEST,
    OPEN_INTERNAL_PAGE,
    OPEN_WEBSITE,
    OPEN_VIDEO,
    IMAGE_BANNER,
    INTERNAL_ROUTE,
    EXTERNAL_URL,
    NONE
}

data class PromotionalBanner(
    val id: String,
    val isDailyChallenge: Boolean = false,
    val badgeText: String = "",
    val metaText: String = "",
    val title: String = "",
    val description: String = "",
    val buttonText: String? = null,
    val imageUrl: String? = null,
    val gradientIndex: Int = 0,
    val gradientColors: List<Long> = listOf(0xFF5038ED, 0xFF7E57FF),
    val actionType: BannerActionType = BannerActionType.NONE,
    val targetId: String? = null,
    val url: String? = null
) {
    val hasImage: Boolean
        get() = !imageUrl.isNullOrBlank()

    val isImageOnly: Boolean
        get() = hasImage && (title.isBlank() || title.equals("IMAGE", ignoreCase = true)) && description.isBlank()

    companion object {
        val defaultBanners: List<PromotionalBanner> = emptyList()
    }
}


