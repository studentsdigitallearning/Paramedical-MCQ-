package com.jacboard.mcqmaster.data

import android.util.Log
import com.jacboard.mcqmaster.data.supabase.SupabaseDataSource
import com.jacboard.mcqmaster.model.Attempt
import com.jacboard.mcqmaster.model.AttemptStatus
import com.jacboard.mcqmaster.model.Chapter
import com.jacboard.mcqmaster.model.ClassData
import com.jacboard.mcqmaster.model.CategoryType
import com.jacboard.mcqmaster.model.Difficulty
import com.jacboard.mcqmaster.model.DailyBestResult
import com.jacboard.mcqmaster.model.DailyLeaderboardEntry
import com.jacboard.mcqmaster.model.DailyLeaderboardState
import com.jacboard.mcqmaster.model.DailyTestAttempt
import com.jacboard.mcqmaster.model.MonthlyStreakDayStatus
import com.jacboard.mcqmaster.model.MonthlyStreakState
import com.jacboard.mcqmaster.model.NotificationItem
import com.jacboard.mcqmaster.model.PromotionalBanner
import com.jacboard.mcqmaster.model.Question
import com.jacboard.mcqmaster.model.QuestionReview
import com.jacboard.mcqmaster.model.Student
import com.jacboard.mcqmaster.model.Subject
import com.jacboard.mcqmaster.model.Test
import com.jacboard.mcqmaster.model.TestCategory
import com.jacboard.mcqmaster.model.TestMode
import com.jacboard.mcqmaster.model.TestResult
import com.jacboard.mcqmaster.model.TestStatus
import com.jacboard.mcqmaster.util.DailyTestTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AppRepository private constructor() {

    private val supabaseDataSource: SupabaseDataSource = SupabaseDataSource()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun loadInitialStudent(): Student {
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        return if (prefs != null) {
            prefs.loadStudent()
        } else {
            Student(
                id = "std_user",
                name = "",
                email = "",
                selectedClassId = "",
                courseName = "",
                xp = 0,
                level = 1,
                streakDays = 0
            )
        }
    }

    private val _student = MutableStateFlow(loadInitialStudent())
    val student: StateFlow<Student> = _student.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val emptyFallbackTest = Test(
        id = "empty_test",
        classId = "class_10",
        categoryId = "cat_mock",
        title = "Test",
        subtitle = "Practice Test",
        questionCount = 0,
        durationMinutes = 15,
        totalMarks = 0f
    )

    // Database-driven collections loaded exclusively from Supabase database
    private val _classes = MutableStateFlow<List<ClassData>>(emptyList())
    val classes: StateFlow<List<ClassData>> = _classes.asStateFlow()

    private val _categories = MutableStateFlow<List<TestCategory>>(emptyList())
    val categories: StateFlow<List<TestCategory>> = _categories.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _tests = MutableStateFlow<List<Test>>(emptyList())
    val tests: StateFlow<List<Test>> = _tests.asStateFlow()

    private val _promotionalBanners = MutableStateFlow<List<PromotionalBanner>>(PromotionalBanner.defaultBanners)
    val promotionalBanners: StateFlow<List<PromotionalBanner>> = _promotionalBanners.asStateFlow()

    private val _chaptersCache = ConcurrentHashMap<String, List<Chapter>>()
    private val _chaptersState = MutableStateFlow<Map<String, List<Chapter>>>(emptyMap())
    val chaptersState: StateFlow<Map<String, List<Chapter>>> = _chaptersState.asStateFlow()
    private val _questionsCache = ConcurrentHashMap<String, List<Question>>()

    private val realtimeClient = com.jacboard.mcqmaster.data.supabase.SupabaseRealtimeClient.getInstance()

    // Loading & Error states for Supabase connectivity (Requirements 12 & 13)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _activeAttempt = MutableStateFlow<Attempt?>(null)
    val activeAttempt: StateFlow<Attempt?> = _activeAttempt.asStateFlow()

    private val _latestResult = MutableStateFlow<TestResult?>(null)
    val latestResult: StateFlow<TestResult?> = _latestResult.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Voice Reading System Architecture
    private val _voiceReadingLanguage = MutableStateFlow("EN") // "EN" or "HI"
    val voiceReadingLanguage: StateFlow<String> = _voiceReadingLanguage.asStateFlow()

    private val _voiceGender = MutableStateFlow("FEMALE") // "FEMALE" or "MALE"
    val voiceGender: StateFlow<String> = _voiceGender.asStateFlow()

    private val _voiceSpeechRate = MutableStateFlow(0.95f)
    val voiceSpeechRate: StateFlow<Float> = _voiceSpeechRate.asStateFlow()

    fun setVoiceReadingLanguage(lang: String) {
        _voiceReadingLanguage.value = if (lang.equals("HI", ignoreCase = true)) "HI" else "EN"
    }

    fun setVoiceGender(gender: String) {
        _voiceGender.value = if (gender.equals("MALE", ignoreCase = true)) "MALE" else "FEMALE"
    }

    fun setVoiceSpeechRate(rate: Float) {
        _voiceSpeechRate.value = rate.coerceIn(0.80f, 1.20f)
    }

    // Daily Test and Leaderboard Architecture
    private val _dailyDateKey = MutableStateFlow(DailyTestTimeUtils.getCurrentDateKey())
    val dailyDateKey: StateFlow<String> = _dailyDateKey.asStateFlow()

    private val _dailyAttempts = MutableStateFlow<List<DailyTestAttempt>>(emptyList())
    val dailyAttempts: StateFlow<List<DailyTestAttempt>> = _dailyAttempts.asStateFlow()

    private val _dailyBestResult = MutableStateFlow<DailyBestResult?>(null)
    val dailyBestResult: StateFlow<DailyBestResult?> = _dailyBestResult.asStateFlow()

    private val _dailyEligibility = MutableStateFlow(com.jacboard.mcqmaster.model.DailyEligibilityResult(isEligible = true))
    val dailyEligibility: StateFlow<com.jacboard.mcqmaster.model.DailyEligibilityResult> = _dailyEligibility.asStateFlow()

    private val _dailyLeaderboardState = MutableStateFlow<DailyLeaderboardState>(createInitialDailyState())
    val dailyLeaderboardState: StateFlow<DailyLeaderboardState> = _dailyLeaderboardState.asStateFlow()

    private val _dailyPeerList = MutableStateFlow<List<DailyLeaderboardEntry>>(emptyList())

    @Volatile
    private var lastLocalSubmissionEpoch: Long = 0L

    // Monthly Streak Architecture (Purely local device state)
    private val _currentMonthKey = MutableStateFlow(DailyTestTimeUtils.getCurrentMonthKey())
    private val _completedDaysInMonth = MutableStateFlow<Set<Int>>(emptySet())

    private val _monthlyStreakState = MutableStateFlow<MonthlyStreakState>(createInitialMonthlyStreakState())
    val monthlyStreakState: StateFlow<MonthlyStreakState> = _monthlyStreakState.asStateFlow()

    init {
        val todayKey = DailyTestTimeUtils.getCurrentDateKey()
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        prefs?.resetDailyStateIfNewDay(todayKey)

        // Restore saved daily attempt on app launch / process recreation so user result is never lost
        val savedAttempt = prefs?.getDailyAttemptDetails(todayKey)
        if (savedAttempt != null && savedAttempt.score > 0f) {
            val userInitials = _student.value.name.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
            _dailyBestResult.value = DailyBestResult(
                studentId = _student.value.id,
                studentName = _student.value.name,
                studentClass = savedAttempt.className.ifBlank { getSelectedClass()?.name.orEmpty() },
                dateKey = todayKey,
                bestScore = savedAttempt.score,
                totalMarks = savedAttempt.totalMarks,
                bestCompletionTimeSeconds = if (savedAttempt.timeTakenSeconds > 0L) savedAttempt.timeTakenSeconds else 300L,
                rank = 1,
                totalParticipants = 1,
                attemptsCount = 1,
                correctCount = savedAttempt.correctCount,
                wrongCount = savedAttempt.wrongCount,
                notAttemptedCount = savedAttempt.notAttemptedCount,
                totalQuestions = if (savedAttempt.totalQuestions > 0) savedAttempt.totalQuestions else (savedAttempt.correctCount + savedAttempt.wrongCount + savedAttempt.notAttemptedCount),
                rollNo = savedAttempt.rollNo,
                updatedAtEpoch = System.currentTimeMillis()
            )
            _dailyEligibility.value = com.jacboard.mcqmaster.model.DailyEligibilityResult(
                isEligible = false,
                reason = "You have already completed today's Daily Test. Students can attempt only once per day across all courses.",
                attemptedClassId = _student.value.selectedClassId,
                attemptedClassName = savedAttempt.className,
                attemptsCount = 1,
                previousAttemptRollNo = savedAttempt.rollNo,
                previousAttemptScore = savedAttempt.score,
                previousAttemptTotalMarks = savedAttempt.totalMarks
            )
        }

        // Sync initial streak calculation (Device-local)
        recalculateMonthlyStreakState()

        // Hydrate academic & UI content from local cache immediately (< 10ms) for instant startup
        // Note: Daily Leaderboard is strictly NEVER cached and always fetched live in real-time
        hydrateFromLocalCache()

        // Clean up previous-day records and unattempted dummy rows in Supabase at startup
        repositoryScope.launch {
            try {
                supabaseDataSource.resetDailyParticipants(todayKey)
            } catch (e: Exception) {
                Log.w("AppRepository", "Initial resetDailyParticipants error: ${e.message}")
            }
        }

        // Automatically sync fresh dynamic content from Supabase
        syncFromSupabase()

        // Check daily eligibility immediately for today's date
        checkDailyEligibility()

        // Initial live leaderboard load from network
        refreshLeaderboard()

        // Connect Supabase Realtime for live Leaderboard, Tests, Banners & Admin Cache Clear
        setupRealtime()
    }

    /**
     * Hydrates static/academic content from local disk cache for instant UI rendering (< 10ms),
     * while background network sync fetches any updates. Daily Leaderboard is strictly excluded.
     */
    private fun hydrateFromLocalCache() {
        try {
            val cachedClasses = supabaseDataSource.loadCachedClasses()
            if (cachedClasses.isNotEmpty()) {
                _classes.value = cachedClasses
            }
            val cachedCategories = supabaseDataSource.loadCachedCategories()
            if (cachedCategories.isNotEmpty()) {
                _categories.value = cachedCategories
            }
            val currentClassId = _student.value.selectedClassId
            if (currentClassId.isNotBlank()) {
                val cachedSubjects = supabaseDataSource.loadCachedSubjects(currentClassId)
                if (cachedSubjects.isNotEmpty()) {
                    _subjects.value = cachedSubjects
                }
                val cachedTests = supabaseDataSource.loadCachedTests(currentClassId)
                if (cachedTests.isNotEmpty()) {
                    _tests.value = cachedTests
                }
                val cachedBanners = supabaseDataSource.loadCachedPromotionalBanners(currentClassId)
                if (cachedBanners.isNotEmpty()) {
                    _promotionalBanners.value = cachedBanners
                }
            }
            val cachedNotifications = supabaseDataSource.loadCachedNotifications()
            if (cachedNotifications.isNotEmpty()) {
                _notifications.value = cachedNotifications
            }
        } catch (e: Exception) {
            Log.w("AppRepository", "hydrateFromLocalCache error: ${e.message}")
        }
    }

    /**
     * Clears all local content and in-memory caches (excluding student profile) and reloads fresh from Supabase.
     * Can be triggered from the Admin Panel ("Clear All Cache") or locally.
     */
    fun clearAllCachesAndReload() {
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.clearAllContentCaches()
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.clearAllLeaderboardCaches(_dailyDateKey.value)
        _chaptersCache.clear()
        _questionsCache.clear()
        _chaptersState.value = emptyMap()
        syncFromSupabase()
    }

    private fun checkRemoteCacheClear(configMap: Map<String, String>) {
        val remoteCacheVersion = (
            configMap["cache_version"]
                ?: configMap["clear_cache_timestamp"]
                ?: configMap["force_clear_cache"]
                ?: configMap["cacheVersion"]
            )?.trim().orEmpty()

        if (remoteCacheVersion.isNotBlank()) {
            val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
            val savedVersion = prefs?.getCacheVersion().orEmpty()
            if (savedVersion != remoteCacheVersion) {
                prefs?.saveCacheVersion(remoteCacheVersion)
                if (savedVersion.isNotBlank()) {
                    Log.d("AppRepository", "Remote Admin Clear All Cache detected ($remoteCacheVersion). Purging local caches...")
                    clearAllCachesAndReload()
                }
            }
        }
    }

    private fun setupRealtime() {
        try {
            realtimeClient.addLeaderboardListener {
                repositoryScope.launch {
                    try {
                        val todayKey = _dailyDateKey.value
                        val selectedClass = getSelectedClass()
                        val courseName = _student.value.courseName
                        val res = supabaseDataSource.fetchLeaderboard(todayKey, selectedClass?.name.orEmpty(), courseName)
                        if (res.isSuccess) {
                            _dailyPeerList.value = res.getOrThrow()
                            recalculateDailyLeaderboard()
                        }
                        checkDailyEligibility()
                    } catch (e: Exception) {
                        Log.w("AppRepository", "Leaderboard realtime update error: ${e.message}")
                    }
                }
            }
            realtimeClient.addTestListener {
                repositoryScope.launch {
                    try {
                        _chaptersCache.clear()
                        _questionsCache.clear()
                        _chaptersState.value = emptyMap()
                        val classesRes = supabaseDataSource.fetchClasses()
                        if (classesRes.isSuccess) _classes.value = classesRes.getOrThrow()
                        val categoriesRes = supabaseDataSource.fetchCategories()
                        if (categoriesRes.isSuccess) _categories.value = categoriesRes.getOrThrow()

                        val currentClassId = _student.value.selectedClassId
                        if (currentClassId.isNotBlank()) {
                            val subRes = supabaseDataSource.fetchSubjects(currentClassId)
                            if (subRes.isSuccess) _subjects.value = subRes.getOrThrow()
                            val res = supabaseDataSource.fetchTests(currentClassId)
                            if (res.isSuccess) {
                                _tests.value = res.getOrThrow()
                            }
                        }
                        refreshPromotionalBanners(currentClassId)
                    } catch (e: Exception) {
                        Log.w("AppRepository", "Test/Content realtime update error: ${e.message}")
                    }
                }
            }
            realtimeClient.addBannerListener {
                repositoryScope.launch {
                    try {
                        val configRes = supabaseDataSource.fetchAppConfig()
                        if (configRes.isSuccess) {
                            val map = configRes.getOrThrow()
                            com.jacboard.mcqmaster.util.AppConfig.updateFromRemoteMap(map)
                            checkRemoteCacheClear(map)
                        }
                        val notifRes = supabaseDataSource.fetchNotifications()
                        if (notifRes.isSuccess) {
                            _notifications.value = notifRes.getOrThrow()
                        }
                        refreshPromotionalBanners(_student.value.selectedClassId)
                    } catch (e: Exception) {
                        Log.w("AppRepository", "Banner/Config realtime update error: ${e.message}")
                    }
                }
            }
            repositoryScope.launch {
                try {
                    realtimeClient.connect()
                } catch (e: Exception) {
                    Log.w("AppRepository", "Realtime connection error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("AppRepository", "setupRealtime error: ${e.message}")
        }
    }

    /**
     * Synchronizes dynamic classes, categories, subjects, tests, and leaderboard from Supabase.
     * Implements proper loading and error states with fallback to local cached/default data.
     */
    fun syncFromSupabase(onComplete: ((Boolean) -> Unit)? = null) {
        repositoryScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            var anySuccess = false

            try {
                // 1. Load Classes / Courses from Supabase (Requirement 1 & 3)
                val classesResult = supabaseDataSource.fetchClasses()
                if (classesResult.isSuccess) {
                    val liveClasses = classesResult.getOrThrow()
                    _classes.value = liveClasses
                    if (liveClasses.isNotEmpty()) {
                        anySuccess = true
                        val currentClassId = _student.value.selectedClassId
                        if (currentClassId.isBlank() || liveClasses.none { it.id.equals(currentClassId, ignoreCase = true) }) {
                            val first = liveClasses.first()
                            _student.update { it.copy(selectedClassId = first.id, courseName = first.name) }
                            com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.updateClass(first.name, first.id, first.name)
                        }
                    }
                }

                // 2. Load Categories from Supabase (Requirement 1)
                val categoriesResult = supabaseDataSource.fetchCategories()
                if (categoriesResult.isSuccess) {
                    val liveCategories = categoriesResult.getOrThrow()
                    _categories.value = liveCategories
                    if (liveCategories.isNotEmpty()) {
                        anySuccess = true
                    }
                }

                // 3. Load Subjects for Current Class from Supabase (Requirement 1 & 4)
                val activeClassId = _student.value.selectedClassId
                if (activeClassId.isNotBlank()) {
                    val subjectsResult = supabaseDataSource.fetchSubjects(activeClassId)
                    if (subjectsResult.isSuccess) {
                        _subjects.value = subjectsResult.getOrThrow()
                        anySuccess = true
                    }

                    // 4. Load Tests for Current Class from Supabase (Requirement 1 & 5)
                    val testsResult = supabaseDataSource.fetchTests(activeClassId)
                    if (testsResult.isSuccess) {
                        _tests.value = testsResult.getOrThrow()
                        anySuccess = true
                    }
                }

                // 5. Load Shared Leaderboard for Current Course + Class from Supabase (Rules 1, 7, 8)
                val todayKey = _dailyDateKey.value
                val selectedClass = getSelectedClass()
                val courseName = _student.value.courseName
                val leaderboardResult = supabaseDataSource.fetchLeaderboard(todayKey, selectedClass?.name.orEmpty(), courseName)
                if (leaderboardResult.isSuccess) {
                    val liveEntries = leaderboardResult.getOrThrow()
                    _dailyPeerList.value = liveEntries
                    recalculateDailyLeaderboard()
                    anySuccess = true
                }

                // 6. Check Daily Test Eligibility from Backend (Rules 5, 6, 16)
                val eligibilityRes = supabaseDataSource.checkDailyTestEligibility(
                    studentId = _student.value.id,
                    classId = _student.value.selectedClassId,
                    className = selectedClass?.name.orEmpty(),
                    dateKey = todayKey
                )
                if (eligibilityRes.isSuccess) {
                    val elig = eligibilityRes.getOrThrow()
                    syncLocalDailyStateWithEligibility(todayKey, elig)
                    _dailyEligibility.value = elig
                    recalculateDailyLeaderboard()
                }

                // 7. Load Notifications from Supabase
                try {
                    val notifRes = supabaseDataSource.fetchNotifications()
                    if (notifRes.isSuccess) {
                        _notifications.value = notifRes.getOrThrow()
                    }
                } catch (_: Exception) {}

                // 8. Load Dynamic App Config (maintenance mode, custom urls, remote cache clear, etc.)
                try {
                    val configRes = supabaseDataSource.fetchAppConfig()
                    if (configRes.isSuccess) {
                        val map = configRes.getOrThrow()
                        com.jacboard.mcqmaster.util.AppConfig.updateFromRemoteMap(map)
                        checkRemoteCacheClear(map)
                    }
                } catch (_: Exception) {}

                // 9. Load Dynamic Hero Promotional Banners from Supabase
                refreshPromotionalBanners(activeClassId)
            } catch (e: Exception) {
                Log.e("AppRepository", "Supabase sync error: ${e.message}")
                _syncError.value = "Unable to load content. Please check your internet connection."
            } finally {
                _isSyncing.value = false
                if (onComplete != null) {
                    withContext(Dispatchers.Main) {
                        onComplete.invoke(anySuccess)
                    }
                }
            }
        }
    }

    fun refreshLeaderboard(className: String? = null, courseName: String? = null) {
        repositoryScope.launch {
            val todayKey = _dailyDateKey.value
            val targetClassName = className ?: getSelectedClass()?.name.orEmpty()
            val targetCourseName = courseName ?: _student.value.courseName
            val leaderboardResult = supabaseDataSource.fetchLeaderboard(todayKey, targetClassName, targetCourseName)
            if (leaderboardResult.isSuccess) {
                val liveEntries = leaderboardResult.getOrThrow()
                _dailyPeerList.value = liveEntries
                recalculateDailyLeaderboard()
            }
        }
    }

    fun checkDailyEligibility() {
        repositoryScope.launch {
            val todayKey = _dailyDateKey.value
            val currentClass = getSelectedClass()
            val eligibilityRes = supabaseDataSource.checkDailyTestEligibility(
                studentId = _student.value.id,
                classId = currentClass?.id.orEmpty(),
                className = currentClass?.name.orEmpty(),
                dateKey = todayKey
            )
            if (eligibilityRes.isSuccess) {
                val elig = eligibilityRes.getOrThrow()
                syncLocalDailyStateWithEligibility(todayKey, elig)
                _dailyEligibility.value = elig
                recalculateDailyLeaderboard()
            }
        }
    }

    /**
     * If the Admin Panel or midnight cleanup deleted the student's daily attempt from Supabase,
     * also delete any local cached attempt and leaderboard cache so the User App and Admin Panel stay 100% synced.
     */
    private fun syncLocalDailyStateWithEligibility(
        todayKey: String,
        eligibility: com.jacboard.mcqmaster.model.DailyEligibilityResult
    ) {
        if (eligibility.isEligible && (System.currentTimeMillis() - lastLocalSubmissionEpoch > 15_000L)) {
            val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
            val hadLocalAttempt = _dailyBestResult.value != null ||
                _dailyAttempts.value.isNotEmpty() ||
                (prefs?.hasAttemptedDailyTestToday(todayKey) == true)
            if (hadLocalAttempt) {
                prefs?.resetDailyStateIfNewDay(todayKey, force = true)
                prefs?.clearAllLeaderboardCaches(todayKey)
                _dailyBestResult.value = null
                _dailyAttempts.value = emptyList()
                _student.update { it.copy(currentDailyRollNo = null) }
            }
        }
    }

    private fun createInitialDailyState(): DailyLeaderboardState {
        val todayKey = DailyTestTimeUtils.getCurrentDateKey()
        val currentClass = getSelectedClass()

        return DailyLeaderboardState(
            dateKey = todayKey,
            displayDate = DailyTestTimeUtils.getFormattedDisplayDate(todayKey),
            countdownToMidnight = DailyTestTimeUtils.formatCountdown(DailyTestTimeUtils.getMillisUntilMidnightIST()),
            totalParticipants = 0,
            top100 = emptyList(),
            currentUserBest = null,
            currentUserRank = null,
            studentsAhead = 0,
            studentsBehind = 0,
            attemptsToday = emptyList(),
            currentClassName = currentClass?.name.orEmpty(),
            isEligible = true,
            ineligibilityReason = null
        )
    }

    private fun createInitialMonthlyStreakState(): MonthlyStreakState {
        val monthKey = DailyTestTimeUtils.getCurrentMonthKey()
        val monthDisplayName = DailyTestTimeUtils.getCurrentMonthDisplayName()
        val totalDays = DailyTestTimeUtils.getDaysInCurrentMonth()
        val startOffset = DailyTestTimeUtils.getFirstDayOfWeekOffsetInMonth()
        val todayDay = DailyTestTimeUtils.getCurrentDayOfMonth()

        val completed = _completedDaysInMonth.value

        var currentContinuousStreak = 0
        val daysList = mutableListOf<MonthlyStreakDayStatus>()

        for (day in 1..totalDays) {
            val isCompleted = completed.contains(day)
            val isToday = (day == todayDay)
            val isFuture = (day > todayDay)
            var is7DayMilestone = false

            if (isCompleted) {
                currentContinuousStreak += 1
                if (currentContinuousStreak > 0 && currentContinuousStreak % 7 == 0) {
                    is7DayMilestone = true
                }
            } else if (!isFuture && !isToday) {
                // Missed past day -> resets continuous streak
                currentContinuousStreak = 0
            }

            daysList.add(
                MonthlyStreakDayStatus(
                    dayOfMonth = day,
                    isCompleted = isCompleted,
                    isToday = isToday,
                    isFuture = isFuture,
                    is7DayMilestone = is7DayMilestone
                )
            )
        }

        val daysToNextBonus = if (currentContinuousStreak > 0 && currentContinuousStreak % 7 == 0) 7 else 7 - (currentContinuousStreak % 7)

        return MonthlyStreakState(
            monthKey = monthKey,
            monthDisplayName = monthDisplayName,
            totalDaysInMonth = totalDays,
            startDayOfWeekOffset = startOffset,
            currentStreakDays = currentContinuousStreak,
            totalDaysCompleted = completed.size,
            nextMilestoneDaysRemaining = daysToNextBonus,
            days = daysList
        )
    }

    fun recalculateMonthlyStreakState() {
        val newState = createInitialMonthlyStreakState()
        _monthlyStreakState.value = newState
        _student.update { current ->
            val updatedStreak = maxOf(current.streakDays, newState.currentStreakDays)
            com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveStudentProgress(
                xp = current.xp,
                level = maxOf(1, current.level),
                streakDays = updatedStreak
            )
            current.copy(streakDays = updatedStreak)
        }
    }

    fun checkMonthlyReset(): Boolean {
        val currentMonth = DailyTestTimeUtils.getCurrentMonthKey()
        if (_currentMonthKey.value != currentMonth) {
            _currentMonthKey.value = currentMonth
            _completedDaysInMonth.value = emptySet()
            recalculateMonthlyStreakState()
            return true
        }
        return false
    }

    fun recordDailyTestCompletion(score: Float, totalMarks: Float, rank: Int) {
        checkMonthlyReset()
        val todayDay = DailyTestTimeUtils.getCurrentDayOfMonth()
        _completedDaysInMonth.update { it + todayDay }
        recalculateMonthlyStreakState()
    }

    fun checkDailyMidnightReset(force: Boolean = false): Boolean {
        val todayKey = DailyTestTimeUtils.getCurrentDateKey()
        val isNewDay = _dailyDateKey.value != todayKey
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        prefs?.resetDailyStateIfNewDay(todayKey, force = force || isNewDay)
        prefs?.clearAllLeaderboardCaches(todayKey)

        if (force || isNewDay) {
            val selectedClass = getSelectedClass()
            val courseName = _student.value.courseName
            _dailyDateKey.value = todayKey
            _dailyAttempts.value = emptyList()
            _dailyBestResult.value = null
            _dailyPeerList.value = emptyList()
            _dailyLeaderboardState.value = createInitialDailyState()
            _dailyEligibility.value = com.jacboard.mcqmaster.model.DailyEligibilityResult(isEligible = true)
            _student.update { it.copy(currentDailyRollNo = null) }
            repositoryScope.launch {
                try {
                    supabaseDataSource.resetDailyParticipants(todayKey)
                } catch (e: Exception) {
                    Log.w("AppRepository", "resetDailyParticipants error: ${e.message}")
                }
                val res = supabaseDataSource.fetchLeaderboard(todayKey, selectedClass?.name.orEmpty(), courseName)
                if (res.isSuccess) {
                    _dailyPeerList.value = res.getOrThrow()
                    recalculateDailyLeaderboard()
                } else {
                    _dailyPeerList.value = emptyList()
                    recalculateDailyLeaderboard()
                }
                checkDailyEligibility()
            }
            return true
        }
        return false
    }

    /**
     * Explicit trigger to reset and delete daily test leaderboard at 12 o'clock midnight IST.
     */
    fun resetDailyLeaderboardAtMidnight() {
        checkDailyMidnightReset(force = true)
    }

    fun resetDailySessionForTesting(forceNewDateKey: String? = null) {
        val targetKey = forceNewDateKey ?: DailyTestTimeUtils.getCurrentDateKey()
        _dailyDateKey.value = targetKey
        _dailyAttempts.value = emptyList()
        _dailyBestResult.value = null
        repositoryScope.launch {
            val selectedClass = getSelectedClass()
            val courseName = _student.value.courseName
            val res = supabaseDataSource.fetchLeaderboard(targetKey, selectedClass?.name.orEmpty(), courseName)
            if (res.isSuccess) {
                _dailyPeerList.value = res.getOrThrow()
                recalculateDailyLeaderboard()
            }
        }
    }

    fun recalculateDailyLeaderboard() {
        val todayKey = _dailyDateKey.value
        val targetClass = getSelectedClass()
        val targetCourse = _student.value.courseName
        val dailyTest = getDailyTest()

        if (dailyTest == null || dailyTest.questionCount <= 0) {
            _dailyLeaderboardState.value = DailyLeaderboardState(
                dateKey = todayKey,
                displayDate = DailyTestTimeUtils.getFormattedDisplayDate(todayKey),
                countdownToMidnight = DailyTestTimeUtils.formatCountdown(DailyTestTimeUtils.getMillisUntilMidnightIST()),
                totalParticipants = 0,
                top100 = emptyList(),
                currentUserBest = null,
                currentUserRank = null,
                studentsAhead = 0,
                studentsBehind = 0,
                attemptsToday = emptyList(),
                currentClassName = targetClass?.name.orEmpty(),
                isEligible = _dailyEligibility.value.isEligible,
                ineligibilityReason = _dailyEligibility.value.reason
            )
            return
        }

        val peers = _dailyPeerList.value
        val best = _dailyBestResult.value

        val allEntries = mutableListOf<DailyLeaderboardEntry>()
        val userRollNo = best?.rollNo
            ?: _student.value.currentDailyRollNo
            ?: com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getTodayRollNo(todayKey)
        val currentUserId = _student.value.id
        val currentUserName = _student.value.name.trim()

        peers.forEach { peer ->
            val isUserPeer = (peer.studentId == currentUserId) ||
                (!userRollNo.isNullOrBlank() && !peer.rollNo.isNullOrBlank() && peer.rollNo == userRollNo)

            val isMatch = com.jacboard.mcqmaster.util.CourseClassUtils.isMatch(
                targetClass = targetClass?.name.orEmpty(),
                targetCourse = targetCourse,
                itemClass = peer.className,
                itemCourse = targetCourse
            )

            if (!isUserPeer && isMatch) {
                allEntries.add(peer)
            }
        }

        val isBestMatchingCurrentSelection = best != null && com.jacboard.mcqmaster.util.CourseClassUtils.isMatch(
            targetClass = targetClass?.name.orEmpty(),
            targetCourse = targetCourse,
            itemClass = best.studentClass,
            itemCourse = targetCourse
        )

        if (best != null && isBestMatchingCurrentSelection) {
            val userInitials = _student.value.name.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")

            allEntries.add(
                DailyLeaderboardEntry(
                    rank = 0,
                    studentId = best.studentId,
                    studentName = "${best.studentName} (You)",
                    className = best.studentClass,
                    score = best.bestScore,
                    totalMarks = best.totalMarks,
                    completionTimeSeconds = best.bestCompletionTimeSeconds,
                    isCurrentUser = true,
                    attemptsCount = best.attemptsCount,
                    avatarInitials = if (userInitials.isNotEmpty()) userInitials else "RS",
                    rollNo = userRollNo
                )
            )
        }

        // Deduplicate allEntries: Guarantee that NO duplicate roll numbers or student IDs exist
        val deduplicated = mutableListOf<DailyLeaderboardEntry>()
        for (item in allEntries) {
            val existingIndex = deduplicated.indexOfFirst { existing ->
                (!item.rollNo.isNullOrBlank() && !existing.rollNo.isNullOrBlank() && item.rollNo == existing.rollNo) ||
                (item.studentId.isNotBlank() && item.studentId == existing.studentId)
            }
            if (existingIndex < 0) {
                deduplicated.add(item)
            } else {
                val existing = deduplicated[existingIndex]
                if (item.isCurrentUser) {
                    deduplicated[existingIndex] = item
                } else if (!existing.isCurrentUser && (item.score > existing.score || (item.score == existing.score && item.completionTimeSeconds < existing.completionTimeSeconds))) {
                    deduplicated[existingIndex] = item
                }
            }
        }

        // Priority 1: Highest Score (descending)
        // Priority 2: Fastest Completion Time (ascending)
        // Priority 3: Stable Tie-breaker (studentId ascending)
        val sorted = deduplicated.sortedWith(
            compareByDescending<DailyLeaderboardEntry> { it.score }
                .thenBy { it.completionTimeSeconds }
                .thenBy { it.studentId }
        )

        val ranked = sorted.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }

        val userEntry = ranked.find { it.isCurrentUser }
        val userRank = userEntry?.rank
        val totalParticipants = ranked.size

        if (best != null && userRank != null && isBestMatchingCurrentSelection) {
            _dailyBestResult.update {
                it?.copy(rank = userRank, totalParticipants = totalParticipants)
            }
        }

        val ahead = if (userRank != null) userRank - 1 else 0
        val behind = if (userRank != null) maxOf(0, totalParticipants - userRank) else 0

        _dailyLeaderboardState.value = DailyLeaderboardState(
            dateKey = todayKey,
            displayDate = DailyTestTimeUtils.getFormattedDisplayDate(todayKey),
            countdownToMidnight = DailyTestTimeUtils.formatCountdown(DailyTestTimeUtils.getMillisUntilMidnightIST()),
            totalParticipants = totalParticipants,
            top100 = ranked.take(9),
            currentUserBest = if (isBestMatchingCurrentSelection) _dailyBestResult.value else null,
            currentUserRank = userRank,
            studentsAhead = ahead,
            studentsBehind = behind,
            attemptsToday = _dailyAttempts.value,
            currentClassName = targetClass?.name.orEmpty(),
            isEligible = _dailyEligibility.value.isEligible,
            ineligibilityReason = _dailyEligibility.value.reason
        )
    }

    /**
     * Checks if there is an active session. Always returns false as registration/authentication is disabled.
     */
    suspend fun checkSession(): Boolean {
        return false
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
    }

    fun getClasses(): List<ClassData> = _classes.value

    fun getSelectedClass(): ClassData? {
        val currentId = _student.value.selectedClassId
        val list = getClasses()
        return list.find { it.id.equals(currentId, ignoreCase = true) }
            ?: list.firstOrNull()
    }

    fun switchClass(classId: String) {
        val foundClass = getClasses().find { it.id.equals(classId, ignoreCase = true) }
        val courseName = foundClass?.name ?: _student.value.courseName
        val className = foundClass?.name ?: _student.value.courseName.ifBlank { "Class" }

        _student.update { it.copy(selectedClassId = classId, courseName = courseName) }
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.updateClass(courseName, classId, className)

        // Hydrate cached subjects, tests, and banners for newly selected class immediately (< 5ms)
        val cachedSubjects = supabaseDataSource.loadCachedSubjects(classId)
        if (cachedSubjects.isNotEmpty()) _subjects.value = cachedSubjects
        val cachedTests = supabaseDataSource.loadCachedTests(classId)
        if (cachedTests.isNotEmpty()) _tests.value = cachedTests
        val cachedBanners = supabaseDataSource.loadCachedPromotionalBanners(classId)
        if (cachedBanners.isNotEmpty()) _promotionalBanners.value = cachedBanners

        _dailyPeerList.value = emptyList()
        recalculateDailyLeaderboard()

        // Dynamically fetch subjects and tests from Supabase for the newly selected class/course
        repositoryScope.launch {
            val subjectsResult = supabaseDataSource.fetchSubjects(classId)
            if (subjectsResult.isSuccess) {
                _subjects.value = subjectsResult.getOrThrow()
            }

            val testsResult = supabaseDataSource.fetchTests(classId)
            if (testsResult.isSuccess) {
                _tests.value = testsResult.getOrThrow()
            }

            // Check eligibility for the newly switched Course+Class
            checkDailyEligibility()

            // Refresh leaderboard strictly for the new Course+Class
            refreshLeaderboard(className, courseName)

            // Refresh dynamic Hero Banners for the new Course+Class
            refreshPromotionalBanners(classId)
        }
    }

    private suspend fun refreshPromotionalBanners(classId: String?) {
        try {
            val bannersRes = supabaseDataSource.fetchPromotionalBanners(classId)
            if (bannersRes.isSuccess) {
                val remoteBanners = bannersRes.getOrThrow()
                val dailyTest = getDailyTest()
                _promotionalBanners.value = remoteBanners.map { banner ->
                    if (banner.isDailyChallenge && dailyTest != null && dailyTest.questionCount > 0 && banner.metaText.isBlank()) {
                        banner.copy(
                            metaText = "${dailyTest.questionCount} Questions • ${dailyTest.durationMinutes} Minutes"
                        )
                    } else {
                        banner
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AppRepository", "refreshPromotionalBanners error: ${e.message}")
        }
    }

    fun selectCourseAndClass(courseName: String, classId: String, className: String) {
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        prefs?.saveCourseAndClass(courseName, classId, className)
        val studentId = prefs?.getStudentId() ?: "std_${System.currentTimeMillis()}"
        _student.update {
            it.copy(
                id = studentId,
                courseName = courseName.trim(),
                selectedClassId = classId.trim()
            )
        }
        switchClass(classId)
    }

    fun saveInitialProfile(name: String, courseName: String, classId: String, className: String) {
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        prefs?.saveProfile(name, courseName, classId, className)
        val studentId = prefs?.getStudentId() ?: "std_${System.currentTimeMillis()}"
        _student.update {
            it.copy(
                id = studentId,
                name = name.trim(),
                courseName = courseName.trim(),
                selectedClassId = classId.trim()
            )
        }
        switchClass(classId)
    }

    fun updateProfileName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        prefs?.saveStudentName(trimmed)
        _student.update { it.copy(name = trimmed) }
        refreshLeaderboard()
    }

    fun deleteProfile() {
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        prefs?.clearProfile()
        _student.update {
            Student(
                id = prefs?.getStudentId() ?: "std_user",
                name = "",
                email = "",
                selectedClassId = "",
                courseName = "",
                xp = 0,
                level = 1,
                streakDays = 0
            )
        }
    }

    suspend fun getOrGenerateDailyRollNo(customStudentName: String? = null): String {
        val todayKey = _dailyDateKey.value
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        val savedRoll = prefs?.getTodayRollNo(todayKey)
        if (!savedRoll.isNullOrBlank()) {
            _student.update { it.copy(currentDailyRollNo = savedRoll) }
            return savedRoll
        }

        if (!customStudentName.isNullOrBlank()) {
            val trimmed = customStudentName.trim()
            _student.update { it.copy(name = trimmed) }
            prefs?.saveStudentName(trimmed)
        }

        val studentName = _student.value.name.ifBlank { prefs?.getStudentName() ?: "Student" }
        val courseName = _student.value.courseName
        val className = getSelectedClass()?.name.orEmpty()
        val studentId = _student.value.id

        val rpcRes = supabaseDataSource.getOrCreateDailyRollNo(
            studentId = studentId,
            studentName = studentName,
            courseName = courseName,
            className = className,
            dateKey = todayKey
        )

        val rollNo = if (rpcRes.isSuccess) {
            rpcRes.getOrThrow().rollNo
        } else {
            com.jacboard.mcqmaster.util.DailyTestTimeUtils.generateTemporaryRollNo(todayKey, 1)
        }

        prefs?.saveTodayRollNo(todayKey, rollNo)
        _student.update { it.copy(currentDailyRollNo = rollNo) }
        return rollNo
    }

    fun getSubjects(): List<Subject> = _subjects.value

    fun getSubject(subjectId: String): Subject? {
        return getSubjects().find { it.id == subjectId }
    }

    suspend fun fetchChaptersForSubject(subjectId: String): List<Chapter> {
        val stateList = _chaptersState.value[subjectId]
        if (!stateList.isNullOrEmpty()) return stateList

        val cached = _chaptersCache[subjectId] ?: supabaseDataSource.loadCachedChapters(subjectId).takeIf { it.isNotEmpty() }
        if (cached != null && cached.isNotEmpty()) {
            _chaptersCache[subjectId] = cached
            _chaptersState.update { it + (subjectId to cached) }
        }

        val result = supabaseDataSource.fetchChapters(subjectId)
        if (result.isSuccess) {
            val list = result.getOrThrow()
            _chaptersCache[subjectId] = list
            _chaptersState.update { it + (subjectId to list) }
            return list
        }

        return cached ?: emptyList()
    }

    fun getChapters(subjectId: String): List<Chapter> {
        val stateList = _chaptersState.value[subjectId]
        if (!stateList.isNullOrEmpty()) return stateList

        val cached = _chaptersCache[subjectId] ?: supabaseDataSource.loadCachedChapters(subjectId).takeIf { it.isNotEmpty() }
        if (cached != null && cached.isNotEmpty()) {
            _chaptersCache[subjectId] = cached
            _chaptersState.update { it + (subjectId to cached) }
            return cached
        }

        // Trigger asynchronous background fetch from Supabase
        repositoryScope.launch {
            val result = supabaseDataSource.fetchChapters(subjectId)
            if (result.isSuccess) {
                val list = result.getOrThrow()
                _chaptersCache[subjectId] = list
                _chaptersState.update { it + (subjectId to list) }
            }
        }

        return emptyList()
    }

    fun getCategories(): List<TestCategory> = _categories.value

    /**
     * Gets today's daily test for the student's selected Course + Class (Rules 1, 2, 3).
     * Strictly belongs to student's Course + Class from database. Returns null if none exists.
     */
    fun getDailyTest(): Test? {
        val currentClassId = _student.value.selectedClassId
        val currentClass = getSelectedClass()
        val foundInState = _tests.value.firstOrNull { 
            (it.classId.equals(currentClassId, ignoreCase = true)) && (it.isDaily || it.categoryId == "cat_daily") 
        }
        if (foundInState != null) {
            return foundInState.copy(
                classId = currentClassId,
                title = if (foundInState.title.contains("Daily", ignoreCase = true) || foundInState.title.contains("MCQ", ignoreCase = true)) "${currentClass?.name ?: "Course"} Daily Challenge" else foundInState.title,
                subtitle = "${currentClass?.name ?: "Course"} • Today's Special Practice Test"
            )
        }
        return null
    }

    fun getTests(
        categoryId: String? = null,
        subjectId: String? = null,
        chapterId: String? = null
    ): List<Test> {
        val currentClassId = _student.value.selectedClassId
        return _tests.value.filter { test ->
            val matchesClass = test.classId.equals(currentClassId, ignoreCase = true) || test.classId.isBlank()
            val isDailyTest = test.isDaily || test.categoryId == "cat_daily"

            if (categoryId == "cat_daily") {
                matchesClass && isDailyTest
            } else {
                matchesClass && !isDailyTest &&
                    (categoryId == null || test.categoryId == categoryId) &&
                    (subjectId == null || test.subjectId == subjectId) &&
                    (chapterId == null || test.chapterId == chapterId)
            }
        }
    }

    fun getTestById(testId: String): Test? {
        if (testId.contains("daily", ignoreCase = true)) {
            return getDailyTest()
        }
        return _tests.value.find { it.id == testId }
    }

    private fun getFallbackQuestions(testId: String): List<Question> {
        return emptyList()
    }

    fun getQuestionsForTest(testId: String): List<Question> {
        val cached = _questionsCache[testId] ?: supabaseDataSource.loadCachedQuestions(testId).takeIf { it.isNotEmpty() }
        if (cached != null && cached.isNotEmpty()) {
            _questionsCache[testId] = cached
            return cached
        }

        // Trigger asynchronous fetch from Supabase
        repositoryScope.launch {
            val result = supabaseDataSource.fetchQuestions(testId)
            if (result.isSuccess && result.getOrThrow().isNotEmpty()) {
                val questions = result.getOrThrow()
                _questionsCache[testId] = questions
                _activeAttempt.update { current ->
                    if (current != null && current.testId == testId && current.shuffledQuestions.isEmpty()) {
                        val ordered = questions
                        val optionOrders = ordered.mapIndexed { idx, q ->
                            idx to (0 until q.options.size).toList()
                        }.toMap()
                        current.copy(shuffledQuestions = ordered, optionOrders = optionOrders)
                    } else current
                }
            }
        }

        return getFallbackQuestions(testId)
    }

    suspend fun prefetchQuestionsForTest(testId: String): List<Question> {
        val cached = _questionsCache[testId] ?: supabaseDataSource.loadCachedQuestions(testId).takeIf { it.isNotEmpty() }
        if (cached != null && cached.isNotEmpty()) {
            _questionsCache[testId] = cached
            _activeAttempt.update { current ->
                if (current != null && current.testId == testId && current.shuffledQuestions.isEmpty()) {
                    val ordered = cached
                    val optionOrders = ordered.mapIndexed { idx, q ->
                        idx to (0 until q.options.size).toList()
                    }.toMap()
                    current.copy(shuffledQuestions = ordered, optionOrders = optionOrders)
                } else current
            }
            return cached
        }

        val result = supabaseDataSource.fetchQuestions(testId)
        if (result.isSuccess && result.getOrThrow().isNotEmpty()) {
            val questions = result.getOrThrow()
            _questionsCache[testId] = questions
            _activeAttempt.update { current ->
                if (current != null && current.testId == testId && current.shuffledQuestions.isEmpty()) {
                    val ordered = questions
                    val optionOrders = ordered.mapIndexed { idx, q ->
                        idx to (0 until q.options.size).toList()
                    }.toMap()
                    current.copy(shuffledQuestions = ordered, optionOrders = optionOrders)
                } else current
            }
            return questions
        }

        val fallback = getFallbackQuestions(testId)
        if (fallback.isNotEmpty()) {
            _questionsCache[testId] = fallback
        }
        return fallback
    }

    fun startOrResumeAttempt(testId: String, mode: TestMode = TestMode.EXAM): Attempt {
        val isDailyReq = testId.contains("daily", ignoreCase = true)
        val test = if (isDailyReq) {
            getDailyTest()
        } else {
            getTestById(testId) ?: _tests.value.firstOrNull { !it.isDaily } ?: emptyFallbackTest
        }

        if (test == null && isDailyReq) {
            val emptyDailyAttempt = Attempt(
                id = "attempt_${System.currentTimeMillis()}",
                testId = testId,
                studentId = _student.value.id,
                status = AttemptStatus.IN_PROGRESS,
                mode = TestMode.EXAM,
                selectedLanguage = "HI",
                startedAtEpoch = System.currentTimeMillis(),
                endsAtEpoch = System.currentTimeMillis() + 600000L,
                totalDurationSeconds = 600L,
                answers = emptyMap(),
                markedForReview = emptySet(),
                currentQuestionIndex = 0,
                optionOrders = emptyMap(),
                shuffledQuestions = emptyList()
            )
            _activeAttempt.value = emptyDailyAttempt
            return emptyDailyAttempt
        }

        val targetTest = test ?: emptyFallbackTest
        val effectiveMode = if (targetTest.isDaily) TestMode.EXAM else mode
        val rawQuestions = if (isDailyReq && getDailyTest() == null) emptyList() else (_questionsCache[targetTest.id] ?: getQuestionsForTest(targetTest.id))
        val shuffledQuestions = rawQuestions
        val durationSeconds = targetTest.durationMinutes * 60L
        val now = System.currentTimeMillis()

        // Preserve authored option order A, B, C, D for each question
        val optionOrders = shuffledQuestions.mapIndexed { idx, q ->
            idx to (0 until q.options.size).toList()
        }.toMap()

        val newAttempt = Attempt(
            id = "attempt_${System.currentTimeMillis()}",
            testId = testId,
            studentId = _student.value.id,
            status = AttemptStatus.IN_PROGRESS,
            mode = effectiveMode,
            startedAtEpoch = now,
            endsAtEpoch = now + durationSeconds * 1000L,
            totalDurationSeconds = durationSeconds,
            answers = emptyMap(),
            markedForReview = emptySet(),
            currentQuestionIndex = 0,
            optionOrders = optionOrders,
            shuffledQuestions = shuffledQuestions
        )

        _activeAttempt.value = newAttempt

        return newAttempt
    }

    fun clearActiveAttempt() {
        _activeAttempt.value = null
    }

    fun restartAttempt(testId: String, mode: TestMode = TestMode.EXAM): Attempt {
        _activeAttempt.value = null
        return startOrResumeAttempt(testId, mode)
    }

    fun recordAnswer(questionIndex: Int, optionIndex: Int) {
        _activeAttempt.update { current ->
            current?.let {
                val updatedAnswers = it.answers.toMutableMap()
                updatedAnswers[questionIndex] = optionIndex
                it.copy(answers = updatedAnswers)
            }
        }
    }

    fun clearAnswer(questionIndex: Int) {
        _activeAttempt.update { current ->
            current?.let {
                val updatedAnswers = it.answers.toMutableMap()
                updatedAnswers.remove(questionIndex)
                it.copy(answers = updatedAnswers)
            }
        }
    }

    fun toggleMarkForReview(questionIndex: Int) {
        _activeAttempt.update { current ->
            current?.let {
                val updated = it.markedForReview.toMutableSet()
                if (updated.contains(questionIndex)) {
                    updated.remove(questionIndex)
                } else {
                    updated.add(questionIndex)
                }
                it.copy(markedForReview = updated)
            }
        }
    }

    fun setCurrentQuestionIndex(index: Int) {
        _activeAttempt.update { current ->
            current?.copy(currentQuestionIndex = index)
        }
    }

    fun submitAttempt(attemptId: String): TestResult {
        val attempt = _activeAttempt.value
        val test = getTestById(attempt?.testId.orEmpty()) ?: _tests.value.firstOrNull() ?: emptyFallbackTest
        val questions = attempt?.shuffledQuestions?.ifEmpty { getQuestionsForTest(test.id) } ?: getQuestionsForTest(test.id)

        val answers = attempt?.answers ?: emptyMap()
        val marked = attempt?.markedForReview ?: emptySet()

        var correctCount = 0
        var wrongCount = 0
        var notAttemptedCount = 0
        var totalScore = 0f

        val reviews = questions.mapIndexed { idx, q ->
            val userAns = answers[idx]
            val isCorrect = userAns != null && userAns == q.correctIndex
            if (userAns == null) {
                notAttemptedCount++
            } else if (isCorrect) {
                correctCount++
                totalScore += q.marks
            } else {
                wrongCount++
                totalScore -= test.negativeMarking
            }

            val optionOrder = attempt?.optionOrders?.get(idx) ?: (0 until q.options.size).toList()

            QuestionReview(
                question = q,
                userAnswerIndex = userAns,
                isCorrect = isCorrect,
                isMarked = marked.contains(idx),
                timeSpentSeconds = 45L,
                optionOrder = optionOrder
            )
        }

        val safeScore = maxOf(0f, totalScore)
        val effectiveTotalMarks = if (test.totalMarks > 0f) {
            test.totalMarks
        } else {
            questions.sumOf { it.marks.toDouble() }.toFloat().coerceAtLeast(1f)
        }
        val percentage = ((safeScore / effectiveTotalMarks) * 100).toInt().coerceIn(0, 100)

        val timeTaken = if (attempt != null) {
            val elapsed = (System.currentTimeMillis() - attempt.startedAtEpoch) / 1000L
            minOf(elapsed, attempt.totalDurationSeconds)
        } else {
            1725L // 28:45 in seconds
        }

        val result = TestResult(
            attemptId = attemptId,
            testId = test.id,
            testTitle = test.title,
            score = safeScore,
            totalMarks = effectiveTotalMarks,
            percentage = percentage,
            correctCount = correctCount,
            wrongCount = wrongCount,
            notAttemptedCount = notAttemptedCount,
            timeTakenSeconds = timeTaken,
            totalTimeSeconds = test.durationMinutes * 60L,
            xpEarned = 45,
            streakDays = _student.value.streakDays,
            newBadgeUnlocked = if (percentage >= 90) "Centurion" else null,
            questionsReview = reviews,
            accuracyPercent = if (correctCount + wrongCount > 0) ((correctCount.toFloat() / (correctCount + wrongCount)) * 100).toInt() else 0
        )

        _latestResult.value = result
        _activeAttempt.value = null

        // Update test status to COMPLETED
        _tests.update { list ->
            list.map {
                if (it.id == test.id) {
                    it.copy(
                        status = TestStatus.COMPLETED,
                        scoreObtained = safeScore,
                        scorePercent = percentage
                    )
                } else it
            }
        }

        // Update student XP, level, streakDays, testsAttempted, questionsSolved locally
        _student.update { current ->
            val earnedXp = 45
            val newXp = current.xp + earnedXp
            val newLevel = maxOf(1, (newXp / 500) + 1)
            val currentStreak = _monthlyStreakState.value.currentStreakDays
            val newStreak = maxOf(current.streakDays, maxOf(1, currentStreak))

            com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveStudentProgress(
                xp = newXp,
                level = newLevel,
                streakDays = newStreak
            )

            current.copy(
                xp = newXp,
                level = newLevel,
                streakDays = newStreak,
                testsAttempted = current.testsAttempted + 1,
                questionsSolved = current.questionsSolved + correctCount + wrongCount
            )
        }

        // Dedicated Daily Test Recording & Best Performance Re-Ranking
        if (test.isDaily || test.id.contains("daily", ignoreCase = true) || test.categoryId == "cat_daily") {
            lastLocalSubmissionEpoch = System.currentTimeMillis()
            checkDailyMidnightReset()
            val todayKey = _dailyDateKey.value
            val todayAttempts = _dailyAttempts.value.filter { it.dateKey == todayKey }
            val attemptNumber = todayAttempts.size + 1

            val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
            val currentRollNo = _student.value.currentDailyRollNo
                ?: prefs?.getTodayRollNo(todayKey)
                ?: com.jacboard.mcqmaster.util.DailyTestTimeUtils.generateTemporaryRollNo(todayKey, 1)

            prefs?.markDailyTestAttempted(
                dateKey = todayKey,
                rollNo = currentRollNo,
                attemptedCourse = _student.value.courseName,
                attemptedClass = getSelectedClass()?.name.orEmpty(),
                score = safeScore,
                totalMarks = test.totalMarks,
                correctCount = correctCount,
                wrongCount = wrongCount,
                notAttemptedCount = notAttemptedCount,
                totalQuestions = questions.size,
                timeTakenSeconds = timeTaken
            )

            _dailyEligibility.value = com.jacboard.mcqmaster.model.DailyEligibilityResult(
                isEligible = false,
                reason = "You have already completed today's Daily Test (Roll No: $currentRollNo). Students can attempt only once per day across all courses.",
                attemptedClassId = getSelectedClass()?.id.orEmpty(),
                attemptedClassName = if (getSelectedClass() != null) "${_student.value.courseName} • ${getSelectedClass()?.name}" else _student.value.courseName,
                attemptsCount = 1,
                previousAttemptRollNo = currentRollNo,
                previousAttemptScore = safeScore,
                previousAttemptTotalMarks = test.totalMarks
            )

            val dailyAttempt = DailyTestAttempt(
                id = "daily_att_${System.currentTimeMillis()}",
                testId = test.id,
                studentId = _student.value.id,
                studentName = _student.value.name,
                dateKey = todayKey,
                attemptNumber = attemptNumber,
                score = safeScore,
                totalMarks = test.totalMarks,
                correctCount = correctCount,
                wrongCount = wrongCount,
                notAttemptedCount = notAttemptedCount,
                completionTimeSeconds = timeTaken,
                submittedAtEpoch = System.currentTimeMillis(),
                rollNo = currentRollNo
            )
            _dailyAttempts.update { it + dailyAttempt }

            val currentBest = _dailyBestResult.value
            val isBetter = when {
                currentBest == null -> true
                safeScore > currentBest.bestScore -> true
                safeScore == currentBest.bestScore && timeTaken < currentBest.bestCompletionTimeSeconds -> true
                else -> false
            }

            if (isBetter) {
                _dailyBestResult.value = DailyBestResult(
                    studentId = _student.value.id,
                    studentName = _student.value.name,
                    studentClass = getSelectedClass()?.name.orEmpty(),
                    dateKey = todayKey,
                    bestScore = safeScore,
                    totalMarks = test.totalMarks,
                    bestCompletionTimeSeconds = timeTaken,
                    rank = currentBest?.rank ?: 1,
                    totalParticipants = maxOf(1, _dailyLeaderboardState.value.totalParticipants),
                    attemptsCount = attemptNumber,
                    correctCount = correctCount,
                    wrongCount = wrongCount,
                    notAttemptedCount = notAttemptedCount,
                    totalQuestions = questions.size,
                    rollNo = currentRollNo,
                    updatedAtEpoch = System.currentTimeMillis()
                )
            } else {
                currentBest?.let {
                    _dailyBestResult.value = it.copy(attemptsCount = attemptNumber, rollNo = currentRollNo)
                }
            }

            recalculateDailyLeaderboard()
            val finalRank = _dailyBestResult.value?.rank ?: 1
            recordDailyTestCompletion(safeScore, test.totalMarks, finalRank)

            // Submit result to Supabase shared leaderboard asynchronously
            val studentId = _student.value.id
            val studentName = _student.value.name
            val studentClass = getSelectedClass()?.name.orEmpty()
            val courseName = _student.value.courseName
            repositoryScope.launch {
                supabaseDataSource.submitLeaderboardEntry(
                    studentId = studentId,
                    studentName = studentName,
                    className = studentClass,
                    dateKey = todayKey,
                    score = safeScore,
                    totalMarks = test.totalMarks,
                    completionTimeSeconds = timeTaken,
                    attemptsCount = attemptNumber,
                    rollNo = currentRollNo,
                    courseName = courseName
                )
                refreshLeaderboard(studentClass)
                checkDailyEligibility()
            }

            val finalResultWithRoll = result.copy(rollNo = currentRollNo)
            _latestResult.value = finalResultWithRoll
            return finalResultWithRoll
        }

        return result
    }

    suspend fun saveOrUpdateTest(test: Test): Boolean {
        _tests.update { current ->
            val index = current.indexOfFirst { it.id == test.id }
            if (index >= 0) {
                current.toMutableList().apply { set(index, test) }
            } else {
                current + test
            }
        }
        val result = supabaseDataSource.saveOrUpdateTest(test)
        return result.isSuccess
    }

    suspend fun deleteTest(testId: String): Boolean {
        _tests.update { current -> current.filterNot { it.id == testId } }
        _questionsCache.remove(testId)
        val result = supabaseDataSource.deleteTest(testId)
        return result.isSuccess
    }

    suspend fun saveOrUpdateQuestion(question: Question): Boolean {
        val testId = question.testId
        val existingQuestions = (_questionsCache[testId] ?: getQuestionsForTest(testId)).toMutableList()
        val index = existingQuestions.indexOfFirst { it.id == question.id }
        if (index >= 0) {
            existingQuestions[index] = question
        } else {
            existingQuestions.add(question)
        }
        _questionsCache[testId] = existingQuestions

        val test = getTestById(testId)
        if (test != null) {
            val newTotalMarks = existingQuestions.sumOf { it.marks.toDouble() }.toFloat().coerceAtLeast(1f)
            val updatedTest = test.copy(
                questionCount = existingQuestions.size,
                totalMarks = newTotalMarks
            )
            _tests.update { current ->
                val tIdx = current.indexOfFirst { it.id == testId }
                if (tIdx >= 0) current.toMutableList().apply { set(tIdx, updatedTest) } else current
            }
            supabaseDataSource.saveOrUpdateTest(updatedTest)
        }

        val result = supabaseDataSource.saveOrUpdateQuestion(question)
        return result.isSuccess
    }

    suspend fun deleteQuestion(questionId: String, testId: String): Boolean {
        val existingQuestions = (_questionsCache[testId] ?: getQuestionsForTest(testId)).filterNot { it.id == questionId }
        _questionsCache[testId] = existingQuestions

        val test = getTestById(testId)
        if (test != null) {
            val newTotalMarks = existingQuestions.sumOf { it.marks.toDouble() }.toFloat().coerceAtLeast(1f)
            val updatedTest = test.copy(
                questionCount = existingQuestions.size,
                totalMarks = newTotalMarks
            )
            _tests.update { current ->
                val tIdx = current.indexOfFirst { it.id == testId }
                if (tIdx >= 0) current.toMutableList().apply { set(tIdx, updatedTest) } else current
            }
            supabaseDataSource.saveOrUpdateTest(updatedTest)
        }

        val result = supabaseDataSource.deleteQuestion(questionId)
        return result.isSuccess
    }

    suspend fun saveOrUpdateBanner(banner: PromotionalBanner): Boolean {
        _promotionalBanners.update { current ->
            val index = current.indexOfFirst { it.id == banner.id }
            if (index >= 0) {
                current.toMutableList().apply { set(index, banner) }
            } else {
                current + banner
            }
        }
        val result = supabaseDataSource.saveOrUpdateBanner(banner, _student.value.selectedClassId)
        return result.isSuccess
    }

    suspend fun deleteBanner(bannerId: String): Boolean {
        _promotionalBanners.update { current -> current.filterNot { it.id == bannerId } }
        val result = supabaseDataSource.deleteBanner(bannerId)
        return result.isSuccess
    }

    fun markNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
    }

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun getInstance(): AppRepository {
            return instance ?: synchronized(this) {
                instance ?: AppRepository().also { instance = it }
            }
        }
    }
}
