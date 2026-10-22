package com.jacboard.mcqmaster.data

import android.util.Log
import com.jacboard.mcqmaster.data.supabase.SupabaseDataSource
import com.jacboard.mcqmaster.model.Attempt
import com.jacboard.mcqmaster.model.AttemptStatus
import com.jacboard.mcqmaster.model.CaptchaChallengeUiState
import com.jacboard.mcqmaster.model.CaptchaVerificationResult
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

    // Independent Top 100 retention per class ("CLASS_8" .. "CLASS_12") keyed by "${dateKey}::${canonicalClassKey}"
    private val _dailyTop100ByClass = ConcurrentHashMap<String, List<DailyLeaderboardEntry>>()

    // Temporary daily registrations / roll number identities keyed by "${dateKey}::${canonicalClassKey}::${studentId}"
    data class TemporaryDailyRegistration(
        val studentId: String,
        val studentName: String,
        val rollNo: String,
        val className: String,
        val dateKey: String,
        val registeredAtEpoch: Long = System.currentTimeMillis()
    )
    private val _dailyTemporaryRegistrations = ConcurrentHashMap<String, TemporaryDailyRegistration>()
    private val top100Lock = Any()

    private val _leaderboardErrorType = MutableStateFlow(com.jacboard.mcqmaster.model.LeaderboardErrorType.NONE)
    private val _leaderboardErrorMessage = MutableStateFlow<String?>(null)

    // Secure Text CAPTCHA state for Daily Test registration form
    private val _captchaState = MutableStateFlow(CaptchaChallengeUiState(isLoading = true))
    val captchaState: StateFlow<CaptchaChallengeUiState> = _captchaState.asStateFlow()

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
        if (savedAttempt != null) {
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

    /**
     * Resolves the canonical class name ("Class 8" .. "Class 12") using a single class filter,
     * falling back safely when `_classes` has not yet finished loading or when profile name updates.
     */
    fun getEffectiveClassName(explicitClassName: String? = null): String {
        val raw = explicitClassName?.takeIf { it.isNotBlank() }
            ?: getSelectedClass()?.name?.takeIf { it.isNotBlank() }
            ?: _student.value.selectedClassId.takeIf { it.isNotBlank() }
            ?: _student.value.courseName
        return com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(raw, _student.value.courseName)
    }

    private fun classBucketKey(dateKey: String, rawClassName: String): String {
        val canonicalKey = com.jacboard.mcqmaster.util.CourseClassUtils.getCanonicalKey(rawClassName)
            .ifBlank { "CLASS_10" }
        return "$dateKey::$canonicalKey"
    }

    private fun updateClassTop100Peers(dateKey: String, rawClassName: String, entries: List<DailyLeaderboardEntry>) {
        val normalizedClass = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(rawClassName)
        val bucketKey = classBucketKey(dateKey, normalizedClass)
        val rankedTop100 = entries
            .filter { com.jacboard.mcqmaster.util.CourseClassUtils.isClassMatch(normalizedClass, it.className) }
            .sortedWith(DailyLeaderboardEntry.RANKING_COMPARATOR)
            .take(100)
            .mapIndexed { idx, entry -> entry.copy(rank = idx + 1, className = normalizedClass) }
        _dailyTop100ByClass[bucketKey] = rankedTop100
        if (com.jacboard.mcqmaster.util.CourseClassUtils.isClassMatch(getEffectiveClassName(), normalizedClass)) {
            _dailyPeerList.value = rankedTop100
        }
    }

    /**
     * Returns the retained Top 100 leaderboard entries for a specific class and date.
     */
    fun getTop100ForClass(className: String, dateKey: String = _dailyDateKey.value): List<DailyLeaderboardEntry> {
        val bucketKey = classBucketKey(dateKey, className)
        return _dailyTop100ByClass[bucketKey].orEmpty()
    }

    /**
     * Returns the temporary Daily Test registration for a student in a specific class and date,
     * or null if the student was permanently deleted (ranked below 100 or after midnight cleanup).
     */
    fun getTemporaryDailyRegistration(
        studentId: String,
        className: String,
        dateKey: String = _dailyDateKey.value
    ): TemporaryDailyRegistration? {
        val canonicalKey = com.jacboard.mcqmaster.util.CourseClassUtils.getCanonicalKey(className).ifBlank { "CLASS_10" }
        return _dailyTemporaryRegistrations["$dateKey::$canonicalKey::$studentId"]
    }

    /**
     * Concurrency-safe server/repository enforcement of Top 100 retention and automatic
     * permanent deletion of all records ranked below 100 for a given class and IST date.
     *
     * Rules enforced:
     * 1. Classes are strictly independent (Class 8, Class 9, Class 10, Class 11, Class 12).
     * 2. Sorts by: (1) Highest score DESC, (2) Highest accuracy DESC, (3) Earliest submission timestamp ASC.
     * 3. Keeps strictly the highest 100 qualifying results.
     * 4. Permanently DELETES any student ranked > 100 (both displaced existing student and non-qualifying new submission),
     *    removing their leaderboard entry, temporary registration/roll number, and attempt/saved answers.
     */
    data class Top100SubmissionOutcome(
        val qualifiedForTop100: Boolean,
        val assignedRank: Int?,
        val retainedCount: Int,
        val permanentlyDeletedStudentIds: List<String>,
        val permanentlyDeletedRollNos: List<String>
    )

    fun submitDailyTestResultToTop100(
        studentId: String,
        studentName: String,
        className: String,
        score: Float,
        totalMarks: Float = 50f,
        accuracy: Float = if (totalMarks > 0f) (score / totalMarks) * 100f else 0f,
        completionTimeSeconds: Long = 300L,
        submittedAtEpoch: Long = System.currentTimeMillis(),
        rollNo: String? = null,
        dateKey: String = _dailyDateKey.value,
        syncToSupabase: Boolean = false
    ): Top100SubmissionOutcome = synchronized(top100Lock) {
        val normalizedClass = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(className)
        val canonicalClassKey = com.jacboard.mcqmaster.util.CourseClassUtils.getCanonicalKey(normalizedClass).ifBlank { "CLASS_10" }
        val bucketKey = "$dateKey::$canonicalClassKey"
        val effectiveRollNo = rollNo?.takeIf { it.isNotBlank() }
            ?: DailyTestTimeUtils.generateTemporaryRollNo(dateKey, (studentId.hashCode() and 0x7fffffff) % 9999 + 1)

        // Register temporary identity before Top 100 evaluation
        val regKey = "$dateKey::$canonicalClassKey::$studentId"
        _dailyTemporaryRegistrations[regKey] = TemporaryDailyRegistration(
            studentId = studentId,
            studentName = studentName.trim(),
            rollNo = effectiveRollNo,
            className = normalizedClass,
            dateKey = dateKey,
            registeredAtEpoch = submittedAtEpoch
        )

        // Record temporary attempt
        val tempAttempt = DailyTestAttempt(
            id = "att_${dateKey}_${canonicalClassKey}_$studentId",
            testId = "daily_${canonicalClassKey.lowercase()}",
            studentId = studentId,
            studentName = studentName.trim(),
            dateKey = dateKey,
            attemptNumber = 1,
            score = score.coerceAtLeast(0f),
            totalMarks = totalMarks.coerceAtLeast(1f),
            correctCount = (score / 2f).toInt(),
            wrongCount = 0,
            notAttemptedCount = 0,
            completionTimeSeconds = completionTimeSeconds,
            submittedAtEpoch = submittedAtEpoch,
            rollNo = effectiveRollNo
        )
        _dailyAttempts.update { list ->
            list.filterNot { it.studentId == studentId && it.dateKey == dateKey && it.testId == tempAttempt.testId } + tempAttempt
        }

        val initials = studentName.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "ST" }

        val candidateEntry = DailyLeaderboardEntry(
            rank = 0,
            studentId = studentId,
            studentName = studentName.trim(),
            className = normalizedClass,
            score = score.coerceAtLeast(0f),
            totalMarks = totalMarks.coerceAtLeast(1f),
            completionTimeSeconds = completionTimeSeconds,
            isCurrentUser = (studentId == _student.value.id),
            attemptsCount = 1,
            avatarInitials = initials,
            rollNo = effectiveRollNo,
            accuracy = accuracy,
            submittedAtEpoch = submittedAtEpoch
        )

        val currentList = _dailyTop100ByClass[bucketKey].orEmpty().toMutableList()
        val existingIdx = currentList.indexOfFirst {
            it.studentId == studentId || (!it.rollNo.isNullOrBlank() && it.rollNo == effectiveRollNo)
        }
        if (existingIdx >= 0) {
            val existing = currentList[existingIdx]
            if (DailyLeaderboardEntry.RANKING_COMPARATOR.compare(candidateEntry, existing) < 0) {
                currentList[existingIdx] = candidateEntry
            }
        } else {
            currentList.add(candidateEntry)
        }

        val sortedAll = currentList.sortedWith(DailyLeaderboardEntry.RANKING_COMPARATOR)
        val retainedTop100 = sortedAll.take(100).mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
        val evictedBelow100 = if (sortedAll.size > 100) sortedAll.drop(100) else emptyList()

        _dailyTop100ByClass[bucketKey] = retainedTop100

        val deletedStudentIds = mutableListOf<String>()
        val deletedRollNos = mutableListOf<String>()

        // Permanently delete all records ranked below 100 (leaderboard, temporary registration, roll no, and attempts)
        for (evicted in evictedBelow100) {
            deletedStudentIds.add(evicted.studentId)
            evicted.rollNo?.takeIf { it.isNotBlank() }?.let { deletedRollNos.add(it) }
            _dailyTemporaryRegistrations.remove("$dateKey::$canonicalClassKey::${evicted.studentId}")
            _dailyAttempts.update { list ->
                list.filterNot {
                    it.dateKey == dateKey &&
                        (it.studentId == evicted.studentId || (!evicted.rollNo.isNullOrBlank() && it.rollNo == evicted.rollNo))
                }
            }
        }

        if (com.jacboard.mcqmaster.util.CourseClassUtils.isClassMatch(getEffectiveClassName(), normalizedClass)) {
            _dailyPeerList.value = retainedTop100
            recalculateDailyLeaderboard()
        }

        if (syncToSupabase) {
            repositoryScope.launch {
                supabaseDataSource.submitLeaderboardEntry(
                    studentId = studentId,
                    studentName = studentName,
                    className = normalizedClass,
                    dateKey = dateKey,
                    score = score,
                    totalMarks = totalMarks,
                    completionTimeSeconds = completionTimeSeconds,
                    attemptsCount = 1,
                    rollNo = effectiveRollNo,
                    courseName = normalizedClass,
                    accuracy = accuracy,
                    submittedAtEpoch = submittedAtEpoch
                )
            }
        }

        val assignedRank = retainedTop100.find { it.studentId == studentId }?.rank
        return Top100SubmissionOutcome(
            qualifiedForTop100 = assignedRank != null,
            assignedRank = assignedRank,
            retainedCount = retainedTop100.size,
            permanentlyDeletedStudentIds = deletedStudentIds,
            permanentlyDeletedRollNos = deletedRollNos
        )
    }

    /**
     * Executes the 12:00 AM Asia/Kolkata (18:30 UTC) permanent deletion of all previous-day
     * temporary Daily Test data across all classes while protecting new-day records and
     * all permanent educational content (classes, subjects, chapters, tests, questions).
     */
    data class MidnightCleanupSummary(
        val newIstDateKey: String,
        val deletedLeaderboardEntries: Int,
        val deletedTemporaryRegistrations: Int,
        val deletedAttempts: Int
    )

    fun executeMidnightPermanentDeletion(
        newIstDateKey: String = DailyTestTimeUtils.getCurrentDateKey(),
        syncToSupabase: Boolean = false
    ): MidnightCleanupSummary = synchronized(top100Lock) {
        var deletedLeaderboardCount = 0
        var deletedRegistrationsCount = 0

        // 1. Permanently delete all previous-day (< newIstDateKey) Top 100 records across all classes
        val keysToRemove = _dailyTop100ByClass.keys.filter { key ->
            val recordDate = key.substringBefore("::")
            recordDate < newIstDateKey
        }
        for (key in keysToRemove) {
            deletedLeaderboardCount += _dailyTop100ByClass.remove(key)?.size ?: 0
        }

        // 2. Permanently delete all previous-day temporary registrations and roll numbers
        val regKeysToRemove = _dailyTemporaryRegistrations.keys.filter { key ->
            val recordDate = key.substringBefore("::")
            recordDate < newIstDateKey
        }
        for (key in regKeysToRemove) {
            if (_dailyTemporaryRegistrations.remove(key) != null) {
                deletedRegistrationsCount++
            }
        }

        // 3. Permanently delete all previous-day attempt records & saved answers
        val beforeAttempts = _dailyAttempts.value.size
        _dailyAttempts.update { list -> list.filter { it.dateKey >= newIstDateKey } }
        val deletedAttemptsCount = (beforeAttempts - _dailyAttempts.value.size).coerceAtLeast(0)

        // 4. Reset active session state if transitioning to the new IST date
        if (_dailyDateKey.value < newIstDateKey) {
            _dailyDateKey.value = newIstDateKey
            _dailyBestResult.value = null
            _dailyEligibility.value = com.jacboard.mcqmaster.model.DailyEligibilityResult(isEligible = true)
            _student.update { it.copy(currentDailyRollNo = null) }
        }

        val currentClass = getEffectiveClassName()
        _dailyPeerList.value = getTop100ForClass(currentClass, _dailyDateKey.value)
        _leaderboardErrorType.value = com.jacboard.mcqmaster.model.LeaderboardErrorType.NONE
        _leaderboardErrorMessage.value = null
        recalculateDailyLeaderboard()

        if (syncToSupabase) {
            repositoryScope.launch {
                try {
                    supabaseDataSource.resetDailyParticipants(newIstDateKey)
                } catch (e: Exception) {
                    Log.w("AppRepository", "executeMidnightPermanentDeletion Supabase error: ${e.message}")
                }
            }
        }

        return MidnightCleanupSummary(
            newIstDateKey = newIstDateKey,
            deletedLeaderboardEntries = deletedLeaderboardCount,
            deletedTemporaryRegistrations = deletedRegistrationsCount,
            deletedAttempts = deletedAttemptsCount
        )
    }

    private fun setupRealtime() {
        try {
            realtimeClient.addLeaderboardListener {
                repositoryScope.launch {
                    try {
                        val todayKey = _dailyDateKey.value
                        val effectiveClassName = getEffectiveClassName()
                        val res = supabaseDataSource.fetchLeaderboard(todayKey, effectiveClassName, effectiveClassName)
                        if (res.isSuccess) {
                            val liveEntries = res.getOrThrow()
                            _leaderboardErrorType.value = com.jacboard.mcqmaster.model.LeaderboardErrorType.NONE
                            _leaderboardErrorMessage.value = null
                            updateClassTop100Peers(todayKey, effectiveClassName, liveEntries)
                            recalculateDailyLeaderboard()
                        } else {
                            val errMsg = res.exceptionOrNull()?.message ?: "Unable to refresh leaderboard."
                            Log.w("AppRepository", "Realtime leaderboard fetch failed: $errMsg")
                            _leaderboardErrorType.value = com.jacboard.mcqmaster.model.LeaderboardErrorType.SERVER_ERROR
                            _leaderboardErrorMessage.value = errMsg
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

                // 5. Load Shared Leaderboard for Current Class from Supabase (single class filter)
                val todayKey = _dailyDateKey.value
                val effectiveClassName = getEffectiveClassName()
                val leaderboardResult = supabaseDataSource.fetchLeaderboard(todayKey, effectiveClassName, effectiveClassName)
                if (leaderboardResult.isSuccess) {
                    val liveEntries = leaderboardResult.getOrThrow()
                    _leaderboardErrorType.value = com.jacboard.mcqmaster.model.LeaderboardErrorType.NONE
                    _leaderboardErrorMessage.value = null
                    updateClassTop100Peers(todayKey, effectiveClassName, liveEntries)
                    recalculateDailyLeaderboard()
                    anySuccess = true
                } else {
                    val errMsg = leaderboardResult.exceptionOrNull()?.message ?: "Failed to load daily leaderboard from server."
                    _leaderboardErrorType.value = com.jacboard.mcqmaster.model.LeaderboardErrorType.SERVER_ERROR
                    _leaderboardErrorMessage.value = errMsg
                    recalculateDailyLeaderboard()
                }

                // 6. Check Daily Test Eligibility from Backend (Rules 5, 6, 16)
                val eligibilityRes = supabaseDataSource.checkDailyTestEligibility(
                    studentId = _student.value.id,
                    classId = _student.value.selectedClassId,
                    className = effectiveClassName,
                    dateKey = todayKey
                )
                if (eligibilityRes.isSuccess) {
                    val elig = eligibilityRes.getOrThrow()
                    val resolvedElig = syncLocalDailyStateWithEligibility(todayKey, elig)
                    _dailyEligibility.value = resolvedElig
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
            val targetClassName = getEffectiveClassName(className ?: courseName)
            val leaderboardResult = supabaseDataSource.fetchLeaderboard(todayKey, targetClassName, targetClassName)
            if (leaderboardResult.isSuccess) {
                val liveEntries = leaderboardResult.getOrThrow()
                _leaderboardErrorType.value = com.jacboard.mcqmaster.model.LeaderboardErrorType.NONE
                _leaderboardErrorMessage.value = null
                updateClassTop100Peers(todayKey, targetClassName, liveEntries)
                recalculateDailyLeaderboard()
            } else {
                // Do NOT wipe existing Top 100 on transient error and do NOT hide database errors
                val errMsg = leaderboardResult.exceptionOrNull()?.message ?: "Unable to fetch daily leaderboard."
                Log.w("AppRepository", "refreshLeaderboard failed for $targetClassName: $errMsg")
                _leaderboardErrorType.value = com.jacboard.mcqmaster.model.LeaderboardErrorType.SERVER_ERROR
                _leaderboardErrorMessage.value = errMsg
                recalculateDailyLeaderboard()
            }
        }
    }

    fun checkDailyEligibility() {
        repositoryScope.launch {
            val todayKey = _dailyDateKey.value
            val currentClass = getSelectedClass()
            val effectiveClassName = getEffectiveClassName()
            val eligibilityRes = supabaseDataSource.checkDailyTestEligibility(
                studentId = _student.value.id,
                classId = currentClass?.id.orEmpty(),
                className = effectiveClassName,
                dateKey = todayKey
            )
            if (eligibilityRes.isSuccess) {
                val elig = eligibilityRes.getOrThrow()
                val resolvedElig = syncLocalDailyStateWithEligibility(todayKey, elig)
                _dailyEligibility.value = resolvedElig
                recalculateDailyLeaderboard()
            }
        }
    }

    /**
     * Keeps local daily attempt state and Supabase server state in sync without ever losing
     * a student's completed attempt for today.
     * - If the student has a completed local attempt for today (`_dailyBestResult.value?.dateKey == todayKey`),
     *   preserve it and re-sync it to Supabase if the server has not yet recorded it.
     * - If the server reports the student already completed today's test (`!eligibility.isEligible`)
     *   and local state is missing, hydrate `_dailyBestResult` from the server eligibility record.
     */
    private fun syncLocalDailyStateWithEligibility(
        todayKey: String,
        eligibility: com.jacboard.mcqmaster.model.DailyEligibilityResult
    ): com.jacboard.mcqmaster.model.DailyEligibilityResult {
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        val localBest = _dailyBestResult.value
        val savedDetails = prefs?.getDailyAttemptDetails(todayKey)

        if (localBest != null && localBest.dateKey == todayKey) {
            if (eligibility.isEligible) {
                // Server is missing our completed attempt for today — re-push to Supabase in background
                val studentId = _student.value.id
                val studentName = _student.value.name.ifBlank { localBest.studentName.ifBlank { "Student" } }
                val className = getEffectiveClassName(localBest.studentClass)
                repositoryScope.launch {
                    supabaseDataSource.submitLeaderboardEntry(
                        studentId = studentId,
                        studentName = studentName,
                        className = className,
                        dateKey = todayKey,
                        score = localBest.bestScore,
                        totalMarks = localBest.totalMarks,
                        completionTimeSeconds = localBest.bestCompletionTimeSeconds,
                        attemptsCount = maxOf(1, localBest.attemptsCount),
                        rollNo = localBest.rollNo,
                        courseName = className
                    )
                }
                return com.jacboard.mcqmaster.model.DailyEligibilityResult(
                    isEligible = false,
                    reason = "You have already completed today's Daily Test. Students can attempt only once per day across all courses.",
                    attemptedClassId = _student.value.selectedClassId,
                    attemptedClassName = className,
                    attemptsCount = maxOf(1, localBest.attemptsCount),
                    previousAttemptRollNo = localBest.rollNo,
                    previousAttemptScore = localBest.bestScore,
                    previousAttemptTotalMarks = localBest.totalMarks
                )
            }
            return eligibility
        }

        if (!eligibility.isEligible && localBest == null) {
            val resolvedScore = eligibility.previousAttemptScore ?: savedDetails?.score ?: 0f
            val resolvedTotal = eligibility.previousAttemptTotalMarks ?: savedDetails?.totalMarks ?: 50f
            val resolvedRoll = eligibility.previousAttemptRollNo ?: savedDetails?.rollNo ?: _student.value.currentDailyRollNo
            val resolvedClass = getEffectiveClassName(eligibility.attemptedClassName ?: eligibility.attemptedClassId)
            _dailyBestResult.value = DailyBestResult(
                studentId = _student.value.id,
                studentName = _student.value.name.ifBlank { "Student" },
                studentClass = resolvedClass,
                dateKey = todayKey,
                bestScore = resolvedScore,
                totalMarks = resolvedTotal,
                bestCompletionTimeSeconds = savedDetails?.timeTakenSeconds?.takeIf { it > 0L } ?: 180L,
                rank = 1,
                totalParticipants = maxOf(1, _dailyPeerList.value.size),
                attemptsCount = 1,
                correctCount = savedDetails?.correctCount ?: 0,
                wrongCount = savedDetails?.wrongCount ?: 0,
                notAttemptedCount = savedDetails?.notAttemptedCount ?: 0,
                totalQuestions = savedDetails?.totalQuestions ?: 25,
                rollNo = resolvedRoll,
                updatedAtEpoch = System.currentTimeMillis()
            )
        }
        return eligibility
    }

    private fun createInitialDailyState(): DailyLeaderboardState {
        val todayKey = DailyTestTimeUtils.getCurrentDateKey()
        val effectiveClass = getEffectiveClassName()

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
            currentClassName = effectiveClass,
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
            val effectiveClassName = getEffectiveClassName()
            executeMidnightPermanentDeletion(todayKey, syncToSupabase = false)
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
                val res = supabaseDataSource.fetchLeaderboard(todayKey, effectiveClassName, effectiveClassName)
                if (res.isSuccess) {
                    val liveEntries = res.getOrThrow()
                    _leaderboardErrorType.value = com.jacboard.mcqmaster.model.LeaderboardErrorType.NONE
                    _leaderboardErrorMessage.value = null
                    updateClassTop100Peers(todayKey, effectiveClassName, liveEntries)
                    recalculateDailyLeaderboard()
                } else {
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
        executeMidnightPermanentDeletion(targetKey, syncToSupabase = false)
        _dailyDateKey.value = targetKey
        _dailyAttempts.value = emptyList()
        _dailyBestResult.value = null
        _dailyPeerList.value = getTop100ForClass(getEffectiveClassName(), targetKey)
        recalculateDailyLeaderboard()
        repositoryScope.launch {
            val effectiveClassName = getEffectiveClassName()
            val res = supabaseDataSource.fetchLeaderboard(targetKey, effectiveClassName, effectiveClassName)
            if (res.isSuccess) {
                updateClassTop100Peers(targetKey, effectiveClassName, res.getOrThrow())
                recalculateDailyLeaderboard()
            }
        }
    }

    fun recalculateDailyLeaderboard() {
        val todayKey = _dailyDateKey.value
        val targetClassName = getEffectiveClassName()
        val bucketPeers = _dailyTop100ByClass[classBucketKey(todayKey, targetClassName)]
        val peers = (bucketPeers ?: _dailyPeerList.value).take(100)
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

            val isMatch = com.jacboard.mcqmaster.util.CourseClassUtils.isClassMatch(
                targetClass = targetClassName,
                itemClass = peer.className
            )

            if (!isUserPeer && isMatch) {
                allEntries.add(peer)
            } else if (isUserPeer && best == null && isMatch) {
                // If user exists in remote leaderboard but _dailyBestResult wasn't hydrated yet, include them as currentUser
                val displayName = currentUserName.ifBlank { peer.studentName.replace(Regex("(?i)\\s*\\(you\\)+"), "").trim() }
                val initials = displayName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { peer.avatarInitials }
                allEntries.add(
                    peer.copy(
                        studentName = "$displayName (You)",
                        avatarInitials = initials,
                        isCurrentUser = true
                    )
                )
            }
        }

        val isBestMatchingCurrentSelection = best != null && com.jacboard.mcqmaster.util.CourseClassUtils.isClassMatch(
            targetClass = targetClassName,
            itemClass = best.studentClass
        )

        if (best != null && isBestMatchingCurrentSelection) {
            val effectiveName = currentUserName.ifBlank { best.studentName.ifBlank { "Student" } }
            val userInitials = effectiveName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
            val accuracyVal = if (best.totalQuestions > 0) {
                (best.correctCount.toFloat() / best.totalQuestions.toFloat()) * 100f
            } else if (best.totalMarks > 0f) {
                (best.bestScore / best.totalMarks) * 100f
            } else 0f

            allEntries.add(
                DailyLeaderboardEntry(
                    rank = 0,
                    studentId = best.studentId,
                    studentName = "$effectiveName (You)",
                    className = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(best.studentClass, targetClassName),
                    score = best.bestScore,
                    totalMarks = best.totalMarks,
                    completionTimeSeconds = best.bestCompletionTimeSeconds,
                    isCurrentUser = true,
                    attemptsCount = best.attemptsCount,
                    avatarInitials = if (userInitials.isNotEmpty()) userInitials else "ST",
                    rollNo = userRollNo,
                    accuracy = accuracyVal,
                    submittedAtEpoch = best.updatedAtEpoch
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
                } else if (!existing.isCurrentUser && DailyLeaderboardEntry.RANKING_COMPARATOR.compare(item, existing) < 0) {
                    deduplicated[existingIndex] = item
                }
            }
        }

        // Ranking Rules (Requirement 3):
        // 1. Highest score first (score DESC)
        // 2. Highest accuracy next (accuracy DESC)
        // 3. Earliest valid submission timestamp (submittedAtEpoch ASC, completionTimeSeconds ASC, studentId ASC)
        val sorted = deduplicated.sortedWith(DailyLeaderboardEntry.RANKING_COMPARATOR)

        // Strictly keep ONLY Top 100 best toppers — remove all below 100
        val top100Only = sorted.take(100).mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
        _dailyTop100ByClass[classBucketKey(todayKey, targetClassName)] = top100Only

        val userEntry = top100Only.find { it.isCurrentUser }
        val userRank = userEntry?.rank
        val totalParticipants = top100Only.size

        if (best != null && isBestMatchingCurrentSelection) {
            val effectiveName = currentUserName.ifBlank { best.studentName.ifBlank { "Student" } }
            _dailyBestResult.update {
                it?.copy(
                    studentName = effectiveName,
                    rank = userRank ?: (sorted.indexOfFirst { e -> e.isCurrentUser }.let { idx -> if (idx >= 0) idx + 1 else it.rank }),
                    totalParticipants = maxOf(1, totalParticipants)
                )
            }
        }

        val effectiveUserRank = userRank ?: _dailyBestResult.value?.rank
        val ahead = if (effectiveUserRank != null && effectiveUserRank > 0) effectiveUserRank - 1 else 0
        val behind = if (effectiveUserRank != null && effectiveUserRank in 1..totalParticipants) maxOf(0, totalParticipants - effectiveUserRank) else 0

        _dailyLeaderboardState.value = DailyLeaderboardState(
            dateKey = todayKey,
            displayDate = DailyTestTimeUtils.getFormattedDisplayDate(todayKey),
            countdownToMidnight = DailyTestTimeUtils.formatCountdown(DailyTestTimeUtils.getMillisUntilMidnightIST()),
            totalParticipants = totalParticipants,
            top100 = top100Only,
            currentUserBest = if (isBestMatchingCurrentSelection) _dailyBestResult.value else null,
            currentUserRank = effectiveUserRank,
            studentsAhead = ahead,
            studentsBehind = behind,
            attemptsToday = _dailyAttempts.value,
            currentClassName = targetClassName,
            isEligible = _dailyEligibility.value.isEligible,
            ineligibilityReason = _dailyEligibility.value.reason,
            errorType = _leaderboardErrorType.value,
            errorMessage = _leaderboardErrorMessage.value
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
        val normalizedClass = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(
            foundClass?.name ?: classId,
            _student.value.courseName
        )
        val courseName = foundClass?.name ?: normalizedClass
        val className = foundClass?.name ?: normalizedClass

        _student.update { it.copy(selectedClassId = classId, courseName = courseName) }
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.updateClass(courseName, classId, className)

        // Hydrate cached subjects, tests, and banners for newly selected class immediately (< 5ms)
        val cachedSubjects = supabaseDataSource.loadCachedSubjects(classId)
        if (cachedSubjects.isNotEmpty()) _subjects.value = cachedSubjects
        val cachedTests = supabaseDataSource.loadCachedTests(classId)
        if (cachedTests.isNotEmpty()) _tests.value = cachedTests
        val cachedBanners = supabaseDataSource.loadCachedPromotionalBanners(classId)
        if (cachedBanners.isNotEmpty()) _promotionalBanners.value = cachedBanners

        // Immediately display retained Top 100 for this class if already loaded today
        _dailyPeerList.value = getTop100ForClass(normalizedClass, _dailyDateKey.value)
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

            // Refresh leaderboard strictly for the new Class using single class filter
            refreshLeaderboard(normalizedClass, normalizedClass)

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
        _dailyBestResult.update { it?.copy(studentName = trimmed) }
        _dailyAttempts.update { list -> list.map { it.copy(studentName = trimmed) } }
        val currentUserId = _student.value.id
        val todayKey = _dailyDateKey.value
        val currentRoll = _dailyBestResult.value?.rollNo
            ?: _student.value.currentDailyRollNo
            ?: prefs?.getTodayRollNo(todayKey)
        val initials = trimmed.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")

        // Update student name in-place across all retained Top 100 class lists so topper list never disappears on name change
        for ((bucketKey, entries) in _dailyTop100ByClass.entries) {
            if (bucketKey.startsWith("$todayKey::")) {
                _dailyTop100ByClass[bucketKey] = entries.map { peer ->
                    if (peer.studentId == currentUserId || (!currentRoll.isNullOrBlank() && peer.rollNo == currentRoll)) {
                        peer.copy(studentName = trimmed, avatarInitials = initials.ifEmpty { peer.avatarInitials })
                    } else {
                        peer
                    }
                }
            }
        }
        _dailyPeerList.update { list ->
            list.map { peer ->
                if (peer.studentId == currentUserId || (!currentRoll.isNullOrBlank() && peer.rollNo == currentRoll)) {
                    peer.copy(studentName = trimmed, avatarInitials = initials.ifEmpty { peer.avatarInitials })
                } else {
                    peer
                }
            }
        }
        recalculateDailyLeaderboard()

        val effectiveClassName = getEffectiveClassName()
        repositoryScope.launch {
            supabaseDataSource.updateDailyStudentName(
                studentId = currentUserId,
                rollNo = currentRoll,
                newName = trimmed,
                dateKey = todayKey
            )
            refreshLeaderboard(effectiveClassName, effectiveClassName)
        }
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

    /**
     * Generates or refreshes a 6-character alphanumeric CAPTCHA challenge from the server.
     * Each CAPTCHA expires after 2 minutes and allows up to 5 attempts.
     */
    suspend fun refreshCaptchaChallenge(): CaptchaChallengeUiState {
        _captchaState.update { it.copy(isLoading = true, errorMessage = null) }
        val res = supabaseDataSource.generateCaptchaChallenge(_student.value.id)
        val newState = res.getOrElse {
            CaptchaChallengeUiState(
                isLoading = false,
                errorMessage = "Unable to generate CAPTCHA. Please tap refresh."
            )
        }
        _captchaState.value = newState
        return newState
    }

    /**
     * Updates the live 2-minute countdown timer for the active CAPTCHA challenge.
     * Automatically marks the CAPTCHA as expired when 2 minutes elapse.
     */
    fun tickCaptchaTimer() {
        val current = _captchaState.value
        if (current.challengeId.isBlank() || current.isVerified || current.isLoading) return
        val remainingMs = current.expiresAtEpochMs - System.currentTimeMillis()
        if (remainingMs <= 0L) {
            if (!current.isExpired) {
                _captchaState.update {
                    it.copy(
                        remainingSeconds = 0,
                        isExpired = true,
                        errorMessage = "CAPTCHA expired (2 minutes). Please refresh to get a new code."
                    )
                }
            }
        } else {
            val secs = ((remainingMs + 999L) / 1000L).toInt()
            if (secs != current.remainingSeconds) {
                _captchaState.update { it.copy(remainingSeconds = secs) }
            }
        }
    }

    /**
     * Verifies the student's CAPTCHA input server-side and generates the temporary daily roll number
     * ONLY after successful CAPTCHA verification.
     */
    suspend fun verifyCaptchaAndGenerateRollNo(
        userAnswer: String,
        customStudentName: String? = null
    ): CaptchaVerificationResult {
        val currentChallenge = _captchaState.value
        val todayKey = _dailyDateKey.value
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()

        if (!customStudentName.isNullOrBlank()) {
            val trimmed = customStudentName.trim()
            _student.update { it.copy(name = trimmed) }
            prefs?.saveStudentName(trimmed)
        }

        val studentName = _student.value.name.ifBlank { prefs?.getStudentName() ?: "Student" }
        val courseName = _student.value.courseName
        val className = getSelectedClass()?.name.orEmpty()
        val studentId = _student.value.id

        val verification = supabaseDataSource.verifyCaptchaAndIssueRollNo(
            challengeId = currentChallenge.challengeId,
            userAnswer = userAnswer,
            studentId = studentId,
            studentName = studentName,
            courseName = courseName,
            className = className,
            dateKey = todayKey
        )

        if (verification.verified && !verification.rollNo.isNullOrBlank()) {
            val issuedRoll = verification.rollNo
            prefs?.saveTodayRollNo(todayKey, issuedRoll)
            _student.update { it.copy(currentDailyRollNo = issuedRoll) }
            // Invalidate the CAPTCHA immediately after successful verification
            _captchaState.update {
                it.copy(
                    isVerified = true,
                    isExpired = false,
                    glyphs = emptyList(),
                    errorMessage = null
                )
            }
        } else {
            val attemptsUsedNow = currentChallenge.maxAttempts - verification.attemptsRemaining
            _captchaState.update {
                it.copy(
                    attemptsUsed = attemptsUsedNow.coerceIn(0, currentChallenge.maxAttempts),
                    errorMessage = verification.errorMessage
                )
            }
            if (verification.shouldRefresh) {
                val refreshed = supabaseDataSource.generateCaptchaChallenge(studentId).getOrNull()
                if (refreshed != null) {
                    _captchaState.value = refreshed.copy(errorMessage = verification.errorMessage)
                }
            }
        }

        return verification
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
            val studentClass = getEffectiveClassName()
            val submittedAtEpoch = dailyAttempt.submittedAtEpoch
            val accuracyVal = result.accuracyPercent.toFloat()
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
                    courseName = studentClass,
                    accuracy = accuracyVal,
                    submittedAtEpoch = submittedAtEpoch
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
