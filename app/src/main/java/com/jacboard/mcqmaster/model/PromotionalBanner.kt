package com.jacboard.mcqmaster.model

enum class BannerActionType {
    OPEN_DAILY_TEST,
    OPEN_TEST,
    OPEN_INTERNAL_PAGE,
    OPEN_WEBSITE,
    OPEN_VIDEO,
    INTERNAL_ROUTE,
    EXTERNAL_URL,
    NONE
}

data class PromotionalBanner(
    val id: String,
    val isDailyChallenge: Boolean = false,
    val badgeText: String = "FEATURED",
    val metaText: String = "",
    val title: String,
    val description: String = "",
    val buttonText: String? = null,
    val gradientIndex: Int = 0,
    val gradientColors: List<Long> = listOf(0xFF5038ED, 0xFF7E57FF),
    val actionType: BannerActionType = BannerActionType.NONE,
    val targetId: String? = null,
    val url: String? = null
) {
    companion object {
        fun createDefaultDailyChallengeBanner(): PromotionalBanner {
            return PromotionalBanner(
                id = "banner_daily_mcq",
                isDailyChallenge = true,
                badgeText = "TODAY'S CHALLENGE",
                metaText = "20 Questions • 30 Minutes",
                title = "Daily MCQ Challenge",
                description = "Compete with all-India JAC board students in today's live test",
                buttonText = "Start Test",
                actionType = BannerActionType.OPEN_DAILY_TEST,
                gradientIndex = 0
            )
        }

        fun createMockTestBanner(): PromotionalBanner {
            return PromotionalBanner(
                id = "banner_mock_tests",
                isDailyChallenge = false,
                badgeText = "BOARD EXAM PREP",
                metaText = "Full Syllabus • Timed Exam",
                title = "All-India Mock Test Series",
                description = "Practice high-scoring timed mock exams designed for JAC Board pattern",
                buttonText = "Explore Mock Tests",
                actionType = BannerActionType.OPEN_INTERNAL_PAGE,
                targetId = "mock_tests",
                gradientIndex = 1
            )
        }

        fun createSubjectWiseBanner(): PromotionalBanner {
            return PromotionalBanner(
                id = "banner_subject_wise",
                isDailyChallenge = false,
                badgeText = "CHAPTER PRACTICE",
                metaText = "All Subjects • Voice Support",
                title = "Subject-Wise Chapter Mastery",
                description = "Master Science, Math & more with detailed explanations and voice reading",
                buttonText = "Start Practice",
                actionType = BannerActionType.OPEN_INTERNAL_PAGE,
                targetId = "subject_wise",
                gradientIndex = 2
            )
        }

        val defaultBanners: List<PromotionalBanner>
            get() = listOf(
                createDefaultDailyChallengeBanner(),
                createMockTestBanner(),
                createSubjectWiseBanner()
            )
    }
}

