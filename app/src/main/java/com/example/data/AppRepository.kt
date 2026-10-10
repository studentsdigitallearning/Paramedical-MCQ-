package com.example.data

import android.util.Log
import com.example.data.supabase.SupabaseDataSource
import com.example.model.Attempt
import com.example.model.AttemptStatus
import com.example.model.Chapter
import com.example.model.ClassData
import com.example.model.DailyBestResult
import com.example.model.DailyLeaderboardEntry
import com.example.model.DailyLeaderboardState
import com.example.model.DailyTestAttempt
import com.example.model.MonthlyStreakDayStatus
import com.example.model.MonthlyStreakState
import com.example.model.NotificationItem
import com.example.model.Question
import com.example.model.QuestionReview
import com.example.model.Student
import com.example.model.Subject
import com.example.model.Test
import com.example.model.TestCategory
import com.example.model.TestMode
import com.example.model.TestResult
import com.example.model.TestStatus
import com.example.util.DailyTestTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AppRepository private constructor() {

    private val supabaseDataSource: SupabaseDataSource = SupabaseDataSource()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun loadInitialStudent(): Student {
        val prefs = com.example.util.UserPreferences.getInstanceOrNull()
        return if (prefs != null && prefs.isProfileCompleted()) {
            prefs.loadStudent()
        } else {
            Student(
                id = prefs?.getStudentId() ?: "std_user",
                name = prefs?.getStudentName()?.takeIf { it.isNotBlank() && it != "Student" } ?: "",
                email = "",
                selectedClassId = prefs?.getSelectedClassId() ?: "class_10",
                courseName = prefs?.getCourseName() ?: "Class 10"
            )
        }
    }

    private val _student = MutableStateFlow(loadInitialStudent())
    val student: StateFlow<Student> = _student.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Database-driven collections backed by Supabase PostgREST
    private val _classes = MutableStateFlow<List<ClassData>>(MockDataSource.classes)
    val classes: StateFlow<List<ClassData>> = _classes.asStateFlow()

    private val _categories = MutableStateFlow<List<TestCategory>>(MockDataSource.categories)
    val categories: StateFlow<List<TestCategory>> = _categories.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(MockDataSource.subjectsByClass["class_10"] ?: emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _tests = MutableStateFlow<List<Test>>(MockDataSource.testsList)
    val tests: StateFlow<List<Test>> = _tests.asStateFlow()

    private val _chaptersCache = ConcurrentHashMap<String, List<Chapter>>()
    private val _chaptersState = MutableStateFlow<Map<String, List<Chapter>>>(emptyMap())
    val chaptersState: StateFlow<Map<String, List<Chapter>>> = _chaptersState.asStateFlow()
    private val _questionsCache = ConcurrentHashMap<String, List<Question>>().apply {
        put("test_daily_10", MockDataSource.questionsDaily)
        put("test_mock_1", MockDataSource.questionsForMockTest1)
        put("test_ch1_1", MockDataSource.questionsMathCh1)
        put("test_sci_ch1_1", MockDataSource.questionsScienceCh1)
        put("test_sci_ch2_1", MockDataSource.questionsScienceCh2)
    }

    private val realtimeClient = com.example.data.supabase.SupabaseRealtimeClient.getInstance()

    // Loading & Error states for Supabase connectivity (Requirements 12 & 13)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _notifications = MutableStateFlow(MockDataSource.notifications)
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

    private val _dailyEligibility = MutableStateFlow(com.example.model.DailyEligibilityResult(isEligible = true))
    val dailyEligibility: StateFlow<com.example.model.DailyEligibilityResult> = _dailyEligibility.asStateFlow()

    private val _dailyLeaderboardState = MutableStateFlow<DailyLeaderboardState>(createInitialDailyState())
    val dailyLeaderboardState: StateFlow<DailyLeaderboardState> = _dailyLeaderboardState.asStateFlow()

    private val _dailyPeerList = MutableStateFlow<List<DailyLeaderboardEntry>>(emptyList())

    // Monthly Streak Architecture (Purely local device state)
    private val _currentMonthKey = MutableStateFlow(DailyTestTimeUtils.getCurrentMonthKey())
    private val _completedDaysInMonth = MutableStateFlow<Set<Int>>(emptySet())

    private val _monthlyStreakState = MutableStateFlow<MonthlyStreakState>(createInitialMonthlyStreakState())
    val monthlyStreakState: StateFlow<MonthlyStreakState> = _monthlyStreakState.asStateFlow()

    init {
        val todayKey = DailyTestTimeUtils.getCurrentDateKey()
        com.example.util.UserPreferences.getInstanceOrNull()?.resetDailyStateIfNewDay(todayKey)

        // Sync initial streak calculation (Device-local)
        recalculateMonthlyStreakState()

        // Automatically sync fresh dynamic content from Supabase
        syncFromSupabase()

        // Check daily eligibility immediately for today's date
        checkDailyEligibility()

        // Connect Supabase Realtime for live Leaderboard & Tests
        setupRealtime()
    }

    private fun setupRealtime() {
        try {
            realtimeClient.addLeaderboardListener {
                repositoryScope.launch {
                    try {
                        val todayKey = _dailyDateKey.value
                        val selectedClass = getSelectedClass()
                        val res = supabaseDataSource.fetchLeaderboard(todayKey, selectedClass.name)
                        if (res.isSuccess) {
                            _dailyPeerList.value = res.getOrThrow()
                            recalculateDailyLeaderboard()
                        }
                    } catch (e: Exception) {
                        Log.w("AppRepository", "Leaderboard realtime update error: ${e.message}")
                    }
                }
            }
            realtimeClient.addTestListener {
                repositoryScope.launch {
                    try {
                        val currentClassId = _student.value.selectedClassId
                        val res = supabaseDataSource.fetchTests(currentClassId)
                        if (res.isSuccess) {
                            _tests.value = res.getOrThrow()
                        }
                    } catch (e: Exception) {
                        Log.w("AppRepository", "Test realtime update error: ${e.message}")
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
                    if (liveClasses.isNotEmpty()) {
                        _classes.value = liveClasses
                        anySuccess = true
                    }
                }

                // 2. Load Categories from Supabase (Requirement 1)
                val categoriesResult = supabaseDataSource.fetchCategories()
                if (categoriesResult.isSuccess) {
                    val liveCategories = categoriesResult.getOrThrow()
                    if (liveCategories.isNotEmpty()) {
                        _categories.value = liveCategories
                        anySuccess = true
                    }
                }

                // 3. Load Subjects for Current Class from Supabase (Requirement 1 & 4)
                val currentClassId = _student.value.selectedClassId
                val subjectsResult = supabaseDataSource.fetchSubjects(currentClassId)
                if (subjectsResult.isSuccess) {
                    val liveSubjects = subjectsResult.getOrThrow()
                    if (liveSubjects.isNotEmpty()) {
                        _subjects.value = liveSubjects
                        anySuccess = true
                    }
                }

                // 4. Load Tests for Current Class from Supabase (Requirement 1 & 5)
                val testsResult = supabaseDataSource.fetchTests(currentClassId)
                if (testsResult.isSuccess) {
                    val liveTests = testsResult.getOrThrow()
                    if (liveTests.isNotEmpty()) {
                        _tests.value = liveTests
                        anySuccess = true
                    }
                }

                // 5. Load Shared Leaderboard for Current Course + Class from Supabase (Rules 1, 7, 8)
                val todayKey = _dailyDateKey.value
                val selectedClass = getSelectedClass()
                val leaderboardResult = supabaseDataSource.fetchLeaderboard(todayKey, selectedClass.name)
                if (leaderboardResult.isSuccess) {
                    val liveEntries = leaderboardResult.getOrThrow()
                    _dailyPeerList.value = liveEntries
                    recalculateDailyLeaderboard()
                    anySuccess = true
                }

                // 6. Check Daily Test Eligibility from Backend (Rules 5, 6, 16)
                val eligibilityRes = supabaseDataSource.checkDailyTestEligibility(
                    studentId = _student.value.id,
                    classId = currentClassId,
                    className = selectedClass.name,
                    dateKey = todayKey
                )
                if (eligibilityRes.isSuccess) {
                    _dailyEligibility.value = eligibilityRes.getOrThrow()
                    recalculateDailyLeaderboard()
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "Supabase sync error: ${e.message}")
                _syncError.value = "Unable to load content. Please check your internet connection."
            } finally {
                _isSyncing.value = false
                onComplete?.invoke(anySuccess)
            }
        }
    }

    fun refreshLeaderboard(className: String? = null) {
        repositoryScope.launch {
            val todayKey = _dailyDateKey.value
            val targetClassName = className ?: getSelectedClass().name
            val leaderboardResult = supabaseDataSource.fetchLeaderboard(todayKey, targetClassName)
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
                classId = currentClass.id,
                className = currentClass.name,
                dateKey = todayKey
            )
            if (eligibilityRes.isSuccess) {
                _dailyEligibility.value = eligibilityRes.getOrThrow()
                recalculateDailyLeaderboard()
            }
        }
    }

    private fun createInitialDailyState(): DailyLeaderboardState {
        val todayKey = DailyTestTimeUtils.getCurrentDateKey()
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
            currentClassName = getSelectedClass().name,
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
            current.copy(streakDays = newState.currentStreakDays)
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

    fun checkDailyMidnightReset(): Boolean {
        val todayKey = DailyTestTimeUtils.getCurrentDateKey()
        com.example.util.UserPreferences.getInstanceOrNull()?.resetDailyStateIfNewDay(todayKey)

        if (_dailyDateKey.value != todayKey) {
            _dailyDateKey.value = todayKey
            _dailyAttempts.value = emptyList()
            _dailyBestResult.value = null
            _dailyEligibility.value = com.example.model.DailyEligibilityResult(isEligible = true)
            _student.update { it.copy(currentDailyRollNo = null) }
            repositoryScope.launch {
                try {
                    supabaseDataSource.resetDailyParticipants(todayKey)
                } catch (e: Exception) {
                    Log.w("AppRepository", "resetDailyParticipants error: ${e.message}")
                }
                val selectedClass = getSelectedClass()
                val res = supabaseDataSource.fetchLeaderboard(todayKey, selectedClass.name)
                if (res.isSuccess) {
                    _dailyPeerList.value = res.getOrThrow()
                    recalculateDailyLeaderboard()
                }
                checkDailyEligibility()
            }
            return true
        }
        return false
    }

    fun resetDailySessionForTesting(forceNewDateKey: String? = null) {
        val targetKey = forceNewDateKey ?: DailyTestTimeUtils.getCurrentDateKey()
        _dailyDateKey.value = targetKey
        _dailyAttempts.value = emptyList()
        _dailyBestResult.value = null
        repositoryScope.launch {
            val selectedClass = getSelectedClass()
            val res = supabaseDataSource.fetchLeaderboard(targetKey, selectedClass.name)
            if (res.isSuccess) {
                _dailyPeerList.value = res.getOrThrow()
                recalculateDailyLeaderboard()
            }
        }
    }

    fun recalculateDailyLeaderboard() {
        val todayKey = _dailyDateKey.value
        val peers = _dailyPeerList.value
        val best = _dailyBestResult.value

        val allEntries = mutableListOf<DailyLeaderboardEntry>()
        val userRollNo = best?.rollNo
            ?: _student.value.currentDailyRollNo
            ?: com.example.util.UserPreferences.getInstanceOrNull()?.getTodayRollNo(todayKey)
        val currentUserId = _student.value.id
        val currentUserName = _student.value.name.trim()

        peers.forEach { peer ->
            val isUserPeer = (peer.studentId == currentUserId) ||
                (!userRollNo.isNullOrBlank() && !peer.rollNo.isNullOrBlank() && peer.rollNo == userRollNo) ||
                (currentUserName.isNotBlank() && currentUserName != "Student" && peer.studentName.equals(currentUserName, ignoreCase = true))

            if (!isUserPeer) {
                allEntries.add(peer)
            }
        }

        if (best != null) {
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

        if (best != null && userRank != null) {
            _dailyBestResult.update {
                it?.copy(rank = userRank, totalParticipants = totalParticipants)
            }
        }

        val ahead = if (userRank != null) userRank - 1 else 0
        val behind = if (userRank != null) maxOf(0, totalParticipants - userRank) else 0

        val currentClass = getSelectedClass()
        _dailyLeaderboardState.value = DailyLeaderboardState(
            dateKey = todayKey,
            displayDate = DailyTestTimeUtils.getFormattedDisplayDate(todayKey),
            countdownToMidnight = DailyTestTimeUtils.formatCountdown(DailyTestTimeUtils.getMillisUntilMidnightIST()),
            totalParticipants = totalParticipants,
            top100 = ranked,
            currentUserBest = _dailyBestResult.value,
            currentUserRank = userRank,
            studentsAhead = ahead,
            studentsBehind = behind,
            attemptsToday = _dailyAttempts.value,
            currentClassName = currentClass.name,
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

    fun getSelectedClass(): ClassData {
        val currentId = _student.value.selectedClassId
        return _classes.value.find { it.id == currentId }
            ?: _classes.value.firstOrNull()
            ?: ClassData(id = currentId, name = "Class 10", subjectCount = 5, displayOrder = 3)
    }

    fun switchClass(classId: String) {
        val foundClass = _classes.value.find { it.id == classId }
        val courseName = foundClass?.name ?: _student.value.courseName
        val className = foundClass?.name ?: "Class 10"

        _student.update { it.copy(selectedClassId = classId, courseName = courseName) }
        com.example.util.UserPreferences.getInstanceOrNull()?.updateClass(courseName, classId, className)

        val localSubs = MockDataSource.subjectsByClass[classId]
        if (!localSubs.isNullOrEmpty()) {
            _subjects.value = localSubs
        }

        val targetClass = getSelectedClass()
        // Dynamically fetch subjects and tests from Supabase for the newly selected class/course
        repositoryScope.launch {
            val subjectsResult = supabaseDataSource.fetchSubjects(classId)
            if (subjectsResult.isSuccess && subjectsResult.getOrThrow().isNotEmpty()) {
                _subjects.value = subjectsResult.getOrThrow()
            }

            val testsResult = supabaseDataSource.fetchTests(classId)
            if (testsResult.isSuccess && testsResult.getOrThrow().isNotEmpty()) {
                _tests.value = testsResult.getOrThrow()
            }

            // Check eligibility for the newly switched Course+Class
            checkDailyEligibility()

            // Refresh leaderboard strictly for the new Course+Class
            refreshLeaderboard(targetClass.name)
        }
    }

    fun selectCourseAndClass(courseName: String, classId: String, className: String) {
        val prefs = com.example.util.UserPreferences.getInstanceOrNull()
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
        val prefs = com.example.util.UserPreferences.getInstanceOrNull()
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
        val prefs = com.example.util.UserPreferences.getInstanceOrNull()
        prefs?.saveStudentName(trimmed)
        _student.update { it.copy(name = trimmed) }
        refreshLeaderboard()
    }

    suspend fun getOrGenerateDailyRollNo(customStudentName: String? = null): String {
        val todayKey = _dailyDateKey.value
        val prefs = com.example.util.UserPreferences.getInstanceOrNull()
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
        val className = getSelectedClass().name
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
            com.example.util.DailyTestTimeUtils.generateTemporaryRollNo(todayKey, 1)
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

        val cached = _chaptersCache[subjectId]
        if (cached != null && cached.isNotEmpty()) {
            _chaptersState.update { it + (subjectId to cached) }
            return cached
        }

        val result = supabaseDataSource.fetchChapters(subjectId)
        if (result.isSuccess) {
            val list = result.getOrThrow()
            _chaptersCache[subjectId] = list
            _chaptersState.update { it + (subjectId to list) }
            return list
        }

        return emptyList()
    }

    fun getChapters(subjectId: String): List<Chapter> {
        val stateList = _chaptersState.value[subjectId]
        if (!stateList.isNullOrEmpty()) return stateList

        val cached = _chaptersCache[subjectId]
        if (cached != null && cached.isNotEmpty()) {
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
     * Strictly belongs to student's Course + Class. Returns null if none exists.
     */
    fun getDailyTest(): Test? {
        val currentClassId = _student.value.selectedClassId
        return _tests.value.firstOrNull { 
            (it.classId.equals(currentClassId, ignoreCase = true)) && (it.isDaily || it.categoryId == "cat_daily") 
        }
    }

    fun getTests(
        categoryId: String? = null,
        subjectId: String? = null,
        chapterId: String? = null
    ): List<Test> {
        val currentClassId = _student.value.selectedClassId
        return _tests.value.filter { test ->
            val matchesClass = test.classId == currentClassId || test.classId.isBlank()
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
        return _tests.value.find { it.id == testId }
    }

    private fun getFallbackQuestions(testId: String): List<Question> {
        return when {
            testId.contains("daily", ignoreCase = true) -> MockDataSource.questionsDaily
            testId.contains("sci_ch2", ignoreCase = true) -> MockDataSource.questionsScienceCh2
            testId.contains("sci", ignoreCase = true) -> MockDataSource.questionsScienceCh1
            testId.contains("math", ignoreCase = true) || testId.contains("ch1", ignoreCase = true) -> MockDataSource.questionsMathCh1
            else -> MockDataSource.questionsForMockTest1
        }
    }

    fun getQuestionsForTest(testId: String): List<Question> {
        val cached = _questionsCache[testId]
        if (cached != null && cached.isNotEmpty()) return cached

        // Trigger asynchronous fetch from Supabase
        repositoryScope.launch {
            val result = supabaseDataSource.fetchQuestions(testId)
            if (result.isSuccess && result.getOrThrow().isNotEmpty()) {
                val questions = result.getOrThrow()
                _questionsCache[testId] = questions
                _activeAttempt.update { current ->
                    if (current != null && current.testId == testId && current.shuffledQuestions.isEmpty()) {
                        val shuffled = questions.shuffled()
                        val optionOrders = shuffled.mapIndexed { idx, q ->
                            val seedList = (0 until q.options.size).toList().shuffled()
                            idx to seedList
                        }.toMap()
                        current.copy(shuffledQuestions = shuffled, optionOrders = optionOrders)
                    } else current
                }
            }
        }

        return getFallbackQuestions(testId)
    }

    suspend fun prefetchQuestionsForTest(testId: String): List<Question> {
        val cached = _questionsCache[testId]
        if (cached != null && cached.isNotEmpty()) {
            _activeAttempt.update { current ->
                if (current != null && current.testId == testId && current.shuffledQuestions.isEmpty()) {
                    val shuffled = cached.shuffled()
                    val optionOrders = shuffled.mapIndexed { idx, q ->
                        val seedList = (0 until q.options.size).toList().shuffled()
                        idx to seedList
                    }.toMap()
                    current.copy(shuffledQuestions = shuffled, optionOrders = optionOrders)
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
                    val shuffled = questions.shuffled()
                    val optionOrders = shuffled.mapIndexed { idx, q ->
                        val seedList = (0 until q.options.size).toList().shuffled()
                        idx to seedList
                    }.toMap()
                    current.copy(shuffledQuestions = shuffled, optionOrders = optionOrders)
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
        val test = getTestById(testId) ?: _tests.value.firstOrNull() ?: MockDataSource.defaultFallbackTest
        val effectiveMode = if (test.isDaily) TestMode.EXAM else mode
        val rawQuestions = _questionsCache[test.id] ?: getQuestionsForTest(test.id)
        val shuffledQuestions = if (rawQuestions.isNotEmpty()) rawQuestions.shuffled() else emptyList()
        val durationSeconds = test.durationMinutes * 60L
        val now = System.currentTimeMillis()

        // Generate randomized option orders for each question
        val optionOrders = shuffledQuestions.mapIndexed { idx, q ->
            val seedList = (0 until q.options.size).toList().shuffled()
            idx to seedList
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
        val test = getTestById(attempt?.testId ?: "test_mock_1") ?: _tests.value.firstOrNull() ?: MockDataSource.defaultFallbackTest
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
        val percentage = ((safeScore / test.totalMarks) * 100).toInt()

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
            totalMarks = test.totalMarks,
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

        // Update student XP, testsAttempted, questionsSolved
        _student.update { current ->
            current.copy(
                xp = current.xp + 45,
                testsAttempted = current.testsAttempted + 1,
                questionsSolved = current.questionsSolved + correctCount + wrongCount
            )
        }

        // Dedicated Daily Test Recording & Best Performance Re-Ranking
        if (test.isDaily || test.id == "test_daily_10") {
            checkDailyMidnightReset()
            val todayKey = _dailyDateKey.value
            val todayAttempts = _dailyAttempts.value.filter { it.dateKey == todayKey }
            val attemptNumber = todayAttempts.size + 1

            val prefs = com.example.util.UserPreferences.getInstanceOrNull()
            val currentRollNo = _student.value.currentDailyRollNo
                ?: prefs?.getTodayRollNo(todayKey)
                ?: com.example.util.DailyTestTimeUtils.generateTemporaryRollNo(todayKey, 1)

            prefs?.markDailyTestAttempted(
                dateKey = todayKey,
                rollNo = currentRollNo,
                attemptedCourse = _student.value.courseName,
                attemptedClass = getSelectedClass().name,
                score = safeScore,
                totalMarks = test.totalMarks
            )

            _dailyEligibility.value = com.example.model.DailyEligibilityResult(
                isEligible = false,
                reason = "You have already completed today's Daily Test (Roll No: $currentRollNo). Students can attempt only once per day across all courses.",
                attemptedClassId = getSelectedClass().id,
                attemptedClassName = "${_student.value.courseName} • ${getSelectedClass().name}",
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
                    studentClass = getSelectedClass().name,
                    dateKey = todayKey,
                    bestScore = safeScore,
                    totalMarks = test.totalMarks,
                    bestCompletionTimeSeconds = timeTaken,
                    rank = currentBest?.rank ?: 1,
                    totalParticipants = maxOf(1, _dailyLeaderboardState.value.totalParticipants),
                    attemptsCount = attemptNumber,
                    correctCount = correctCount,
                    wrongCount = wrongCount,
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
            val studentClass = getSelectedClass().name
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
