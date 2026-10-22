package com.jacboard.mcqmaster.model

enum class Difficulty {
    EASY, MEDIUM, HARD
}

enum class TestStatus {
    NOT_ATTEMPTED,
    IN_PROGRESS,
    COMPLETED,
    LOCKED,
    EXPIRED
}

enum class CategoryType {
    DAILY,
    SUBJECT,
    MOCK,
    PRACTICE,
    PREVIOUS,
    CUSTOM,
    RECOMMENDED
}

enum class TestMode {
    EXAM,
    PRACTICE
}

enum class VoiceGender {
    FEMALE,
    MALE
}

enum class VoiceLanguage {
    AUTO,
    EN_IN,
    HI_IN
}

enum class VoiceSpeed(val rate: Float, val label: String) {
    SLOW(0.8f, "Slow (0.8x)"),
    NORMAL(1.0f, "Normal (1.0x)"),
    FAST(1.15f, "Fast (1.15x)")
}

data class VoiceSettings(
    val gender: VoiceGender = VoiceGender.FEMALE,
    val language: VoiceLanguage = VoiceLanguage.AUTO,
    val speed: VoiceSpeed = VoiceSpeed.NORMAL,
    val readOptions: Boolean = true
)

enum class AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    AUTO_SUBMITTED,
    ABANDONED
}

data class Student(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val selectedClassId: String = "",
    val courseName: String = "",
    val currentDailyRollNo: String? = null,
    val xp: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 0,
    val badgeCount: Int = 0,
    val testsAttempted: Int = 0,
    val averageScorePercent: Int = 0,
    val questionsSolved: Int = 0,
    val currentRank: Int = 0,
    val weeklyGoal: Int = 5,
    val weeklyCompleted: Int = 0
)

data class CourseData(
    val id: String,
    val name: String,
    val description: String,
    val hasClasses: Boolean,
    val targetClassId: String? = null,
    val iconKey: String = "school"
)


data class ClassData(
    val id: String,
    val name: String,
    val subjectCount: Int = 5,
    val displayOrder: Int
)

data class Subject(
    val id: String,
    val classId: String,
    val name: String,
    val iconKey: String,
    val chapterCount: Int,
    val progressPercent: Int
)

data class Chapter(
    val id: String,
    val subjectId: String,
    val chapterNumber: Int,
    val name: String,
    val testCount: Int,
    val progressPercent: Int
)

data class TestCategory(
    val id: String,
    val name: String,
    val type: CategoryType,
    val iconKey: String,
    val subtitle: String = "",
    val testCount: Int = 0
)

data class Test(
    val id: String,
    val classId: String,
    val categoryId: String,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val chapterId: String? = null,
    val chapterName: String? = null,
    val title: String,
    val subtitle: String = "",
    val questionCount: Int,
    val durationMinutes: Int,
    val totalMarks: Float,
    val negativeMarking: Float = 0f,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val status: TestStatus = TestStatus.NOT_ATTEMPTED,
    val scoreObtained: Float? = null,
    val scorePercent: Int? = null,
    val isDaily: Boolean = false,
    val expiryNote: String? = null,
    val randomizeQuestions: Boolean = true,
    val randomizeOptions: Boolean = true
)

data class Question(
    val id: String,
    val testId: String,
    val questionIndex: Int,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val chapterName: String = "",
    val marks: Float = 1.0f,
    val textHindi: String? = null,
    val optionsHindi: List<String>? = null,
    val explanationHindi: String? = null,
    val imageUrl: String? = null,
    val optionIds: List<String> = listOf("opt_a", "opt_b", "opt_c", "opt_d")
) {
    fun hasHindi(): Boolean {
        return !textHindi.isNullOrBlank() || !optionsHindi.isNullOrEmpty()
    }

    fun getEnglishText(): String {
        return text
    }

    fun getHindiText(): String {
        if (!textHindi.isNullOrBlank()) return textHindi
        return text
    }

    fun getEnglishOptions(): List<String> {
        return options
    }

    fun getHindiOptions(): List<String> {
        if (!optionsHindi.isNullOrEmpty() && optionsHindi.size == options.size) {
            return optionsHindi
        }
        return options
    }

    fun getDisplayText(isHindi: Boolean): String {
        return if (isHindi && !textHindi.isNullOrBlank()) textHindi else text
    }

    fun getDisplayOptions(isHindi: Boolean): List<String> {
        return if (isHindi && !optionsHindi.isNullOrEmpty() && optionsHindi.size == options.size) {
            optionsHindi
        } else {
            options
        }
    }

    fun getDisplayExplanation(isHindi: Boolean): String {
        return if (isHindi && !explanationHindi.isNullOrBlank()) explanationHindi else explanation
    }
}

data class Attempt(
    val id: String,
    val testId: String,
    val studentId: String,
    val status: AttemptStatus = AttemptStatus.IN_PROGRESS,
    val mode: TestMode = TestMode.EXAM,
    val selectedLanguage: String = "HI",
    val startedAtEpoch: Long,
    val endsAtEpoch: Long,
    val totalDurationSeconds: Long,
    val rollNo: String? = null,
    val answers: Map<Int, Int> = emptyMap(), // questionIndex -> optionIndex
    val markedForReview: Set<Int> = emptySet(), // question indices
    val currentQuestionIndex: Int = 0,
    val timeSpentPerQuestion: Map<Int, Long> = emptyMap(),
    val optionOrders: Map<Int, List<Int>> = emptyMap(), // questionIndex -> randomized option order indices [0, 1, 2, 3]
    val shuffledQuestions: List<Question> = emptyList()
)

data class QuestionReview(
    val question: Question,
    val userAnswerIndex: Int?,
    val isCorrect: Boolean,
    val isMarked: Boolean,
    val timeSpentSeconds: Long,
    val optionOrder: List<Int> = listOf(0, 1, 2, 3)
)

data class TestResult(
    val attemptId: String,
    val testId: String,
    val testTitle: String,
    val score: Float,
    val totalMarks: Float,
    val percentage: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val notAttemptedCount: Int,
    val timeTakenSeconds: Long,
    val totalTimeSeconds: Long,
    val xpEarned: Int,
    val streakDays: Int,
    val newBadgeUnlocked: String? = null,
    val questionsReview: List<QuestionReview>,
    val accuracyPercent: Int,
    val rollNo: String? = null,
    val submissionId: Long? = null,
    val strongestChapter: String = "",
    val weakestChapter: String = ""
)

data class LeaderboardEntry(
    val id: String,
    val rank: Int,
    val name: String,
    val className: String,
    val score: Int,
    val testsCompleted: Int,
    val isCurrentUser: Boolean = false,
    val streak: Int = 5
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val type: String, // DAILY, NEW_TEST, RESULT, ANNOUNCEMENT
    val isRead: Boolean = false
)

data class BadgeItem(
    val id: String,
    val title: String,
    val description: String,
    val iconKey: String,
    val isUnlocked: Boolean = true,
    val unlockedAt: String? = null
)

data class FAQItem(
    val question: String,
    val answer: String
)

data class DailyTestAttempt(
    val id: String,
    val testId: String,
    val studentId: String,
    val studentName: String,
    val dateKey: String,
    val attemptNumber: Int,
    val score: Float,
    val totalMarks: Float,
    val correctCount: Int,
    val wrongCount: Int,
    val notAttemptedCount: Int,
    val completionTimeSeconds: Long,
    val submittedAtEpoch: Long,
    val rollNo: String? = null
)

data class DailyBestResult(
    val studentId: String = "",
    val studentName: String = "",
    val studentClass: String = "",
    val dateKey: String,
    val bestScore: Float,
    val totalMarks: Float,
    val bestCompletionTimeSeconds: Long,
    val rank: Int,
    val totalParticipants: Int,
    val attemptsCount: Int = 1,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val notAttemptedCount: Int = 0,
    val totalQuestions: Int = 0,
    val rollNo: String? = null,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

data class DailyLeaderboardEntry(
    val rank: Int,
    val studentId: String,
    val studentName: String,
    val className: String,
    val score: Float,
    val totalMarks: Float,
    val completionTimeSeconds: Long,
    val isCurrentUser: Boolean = false,
    val attemptsCount: Int = 1,
    val avatarInitials: String = "",
    val rollNo: String? = null,
    val accuracy: Float = if (totalMarks > 0f) (score / totalMarks) * 100f else 0f,
    val submittedAtEpoch: Long = 0L
) {
    companion object {
        /**
         * Official Daily Test Ranking Rules:
         * 1. Highest score first (score DESC)
         * 2. Highest accuracy next (accuracy DESC)
         * 3. Earliest valid submission timestamp as final tie-breaker (submittedAtEpoch ASC when present, then completionTimeSeconds ASC, then studentId ASC)
         */
        val RANKING_COMPARATOR: Comparator<DailyLeaderboardEntry> = Comparator { a, b ->
            val scoreCmp = b.score.compareTo(a.score)
            if (scoreCmp != 0) return@Comparator scoreCmp

            val accCmp = b.accuracy.compareTo(a.accuracy)
            if (accCmp != 0) return@Comparator accCmp

            if (a.submittedAtEpoch > 0L && b.submittedAtEpoch > 0L && a.submittedAtEpoch != b.submittedAtEpoch) {
                return@Comparator a.submittedAtEpoch.compareTo(b.submittedAtEpoch)
            }

            val timeCmp = a.completionTimeSeconds.compareTo(b.completionTimeSeconds)
            if (timeCmp != 0) return@Comparator timeCmp

            a.studentId.compareTo(b.studentId)
        }
    }
}


data class DailyEligibilityResult(
    val isEligible: Boolean = true,
    val reason: String? = null,
    val attemptedClassId: String? = null,
    val attemptedClassName: String? = null,
    val attemptsCount: Int = 0,
    val previousAttemptRollNo: String? = null,
    val previousAttemptScore: Float? = null,
    val previousAttemptTotalMarks: Float? = null
)

data class DailyRollNoResult(
    val rollNo: String,
    val alreadyAttempted: Boolean,
    val courseName: String,
    val className: String,
    val score: Float,
    val totalMarks: Float,
    val isNew: Boolean
)

enum class LeaderboardErrorType {
    NONE,
    NO_RESULTS,
    NO_INTERNET,
    SERVER_ERROR,
    PERMISSION_DENIED,
    LOADING
}

data class DailyLeaderboardState(
    val dateKey: String,
    val displayDate: String,
    val countdownToMidnight: String,
    val totalParticipants: Int,
    val top100: List<DailyLeaderboardEntry>,
    val currentUserBest: DailyBestResult?,
    val currentUserRank: Int?,
    val studentsAhead: Int,
    val studentsBehind: Int,
    val attemptsToday: List<DailyTestAttempt> = emptyList(),
    val currentClassName: String = "",
    val isEligible: Boolean = true,
    val ineligibilityReason: String? = null,
    val isLoading: Boolean = false,
    val errorType: LeaderboardErrorType = LeaderboardErrorType.NONE,
    val errorMessage: String? = null
) {
    /**
     * The student application displays only the Top 10, while Supabase retains the Top 100.
     */
    val top10: List<DailyLeaderboardEntry>
        get() = top100.take(10)
}

data class MonthlyStreakDayStatus(
    val dayOfMonth: Int,
    val isCompleted: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
    val is7DayMilestone: Boolean = false
)

data class MonthlyStreakState(
    val monthKey: String, // e.g. "2026-10"
    val monthDisplayName: String, // e.g. "October 2026"
    val totalDaysInMonth: Int, // 31
    val startDayOfWeekOffset: Int, // 0 for Sun, 1 for Mon...
    val currentStreakDays: Int,
    val totalDaysCompleted: Int,
    val nextMilestoneDaysRemaining: Int,
    val days: List<MonthlyStreakDayStatus>
)

data class CaptchaGlyph(
    val char: Char,
    val rotationDeg: Float,
    val verticalOffsetRatio: Float,
    val scaleFactor: Float = 1.0f
) {
    val rotationDegrees: Float
        get() = rotationDeg
    val verticalOffsetDp: Float
        get() = verticalOffsetRatio * 24f
    val scale: Float
        get() = scaleFactor
}

data class CaptchaChallengeUiState(
    val challengeId: String = "",
    val glyphs: List<CaptchaGlyph> = emptyList(),
    val issuedAtEpochMs: Long = 0L,
    val expiresAtEpochMs: Long = 0L,
    val remainingSeconds: Int = 120,
    val maxAttempts: Int = 5,
    val attemptsUsed: Int = 0,
    val isLoading: Boolean = false,
    val isVerifying: Boolean = false,
    val isVerified: Boolean = false,
    val isExpired: Boolean = false,
    val errorMessage: String? = null
) {
    val attemptsRemaining: Int
        get() = (maxAttempts - attemptsUsed).coerceAtLeast(0)
}

data class CaptchaVerificationResult(
    val verified: Boolean,
    val rollNo: String? = null,
    val attemptsRemaining: Int = 5,
    val expired: Boolean = false,
    val maxAttemptsReached: Boolean = false,
    val shouldRefresh: Boolean = false,
    val errorMessage: String? = null
)



