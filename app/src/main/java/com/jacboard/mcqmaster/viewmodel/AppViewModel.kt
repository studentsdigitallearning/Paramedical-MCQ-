package com.jacboard.mcqmaster.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jacboard.mcqmaster.data.AppRepository
import com.jacboard.mcqmaster.model.Attempt
import com.jacboard.mcqmaster.model.ClassData
import com.jacboard.mcqmaster.model.DailyBestResult
import com.jacboard.mcqmaster.model.DailyLeaderboardEntry
import com.jacboard.mcqmaster.model.DailyLeaderboardState
import com.jacboard.mcqmaster.model.DailyTestAttempt
import com.jacboard.mcqmaster.model.Difficulty
import com.jacboard.mcqmaster.model.NotificationItem
import com.jacboard.mcqmaster.model.Question
import com.jacboard.mcqmaster.model.Student
import com.jacboard.mcqmaster.model.Subject
import com.jacboard.mcqmaster.model.Test
import com.jacboard.mcqmaster.model.TestCategory
import com.jacboard.mcqmaster.model.TestResult
import com.jacboard.mcqmaster.model.TestStatus
import com.jacboard.mcqmaster.util.CertificateGenerator
import com.jacboard.mcqmaster.util.DailyTestTimeUtils
import com.jacboard.mcqmaster.util.NetworkMonitor
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, AppRepository.getInstance())

    private val networkMonitor = NetworkMonitor.getInstance(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private val _isCheckingConnection = MutableStateFlow(false)
    val isCheckingConnection: StateFlow<Boolean> = _isCheckingConnection.asStateFlow()

    val student: StateFlow<Student> = repository.student
    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
    val activeAttempt: StateFlow<Attempt?> = repository.activeAttempt
    val latestResult: StateFlow<TestResult?> = repository.latestResult

    val isAuthenticated: StateFlow<Boolean> = repository.isAuthenticated

    // Dynamic Supabase flows (Requirement 1 & 11)
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val syncError: StateFlow<String?> = repository.syncError
    val classes: StateFlow<List<ClassData>> = repository.classes
    val subjects: StateFlow<List<Subject>> = repository.subjects
    val tests: StateFlow<List<Test>> = repository.tests
    val categories: StateFlow<List<TestCategory>> = repository.categories
    val promotionalBanners: StateFlow<List<com.jacboard.mcqmaster.model.PromotionalBanner>> = repository.promotionalBanners
    val chaptersState: StateFlow<Map<String, List<com.jacboard.mcqmaster.model.Chapter>>> = repository.chaptersState

    fun refreshDataFromSupabase(onComplete: ((Boolean) -> Unit)? = null) {
        if (!networkMonitor.isCurrentlyConnected()) {
            onComplete?.invoke(false)
            return
        }
        repository.syncFromSupabase(onComplete)
    }

    fun retryConnection(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isCheckingConnection.value = true
            val isConnected = networkMonitor.refreshStatus()
            if (isConnected) {
                repository.syncFromSupabase { success ->
                    _isCheckingConnection.value = false
                    onComplete?.invoke(success)
                }
            } else {
                _isCheckingConnection.value = false
                onComplete?.invoke(false)
            }
        }
    }

    fun refreshLeaderboard(className: String? = null) {
        repository.refreshLeaderboard(className)
    }

    // Voice Reading System StateFlows
    val voiceReadingLanguage: StateFlow<String> = repository.voiceReadingLanguage
    val voiceGender: StateFlow<String> = repository.voiceGender
    val voiceSpeechRate: StateFlow<Float> = repository.voiceSpeechRate

    fun setVoiceReadingLanguage(lang: String) {
        repository.setVoiceReadingLanguage(lang)
    }

    fun setVoiceGender(gender: String) {
        repository.setVoiceGender(gender)
    }

    fun setVoiceSpeechRate(rate: Float) {
        repository.setVoiceSpeechRate(rate)
    }

    // Daily Test state flows
    val dailyLeaderboardState: StateFlow<DailyLeaderboardState> = repository.dailyLeaderboardState
    val dailyBestResult: StateFlow<DailyBestResult?> = repository.dailyBestResult
    val dailyAttempts: StateFlow<List<DailyTestAttempt>> = repository.dailyAttempts
    val dailyEligibility: StateFlow<com.jacboard.mcqmaster.model.DailyEligibilityResult> = repository.dailyEligibility

    // Monthly Streak and Rewards state flow
    val monthlyStreakState: StateFlow<com.jacboard.mcqmaster.model.MonthlyStreakState> = repository.monthlyStreakState

    // Local transient states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Countdown timer for daily challenge synchronized to 12:00:00 AM IST
    private val _dailyCountdown = MutableStateFlow(
        DailyTestTimeUtils.formatCountdown(DailyTestTimeUtils.getMillisUntilMidnightIST())
    )
    val dailyCountdown: StateFlow<String> = _dailyCountdown.asStateFlow()

    init {
        // Automatic recovery: when internet returns, sync fresh content seamlessly
        networkMonitor.setOnNetworkRestoredListener {
            viewModelScope.launch {
                repository.syncFromSupabase()
            }
        }

        // Continuous live ticking timer to next midnight 12:00:00 AM IST
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            var tickCount = 0
            var lastDateKey = DailyTestTimeUtils.getCurrentDateKey()
            var previousMillis = DailyTestTimeUtils.getMillisUntilMidnightIST()
            while (true) {
                val millis = DailyTestTimeUtils.getMillisUntilMidnightIST()
                _dailyCountdown.value = DailyTestTimeUtils.formatCountdown(millis)
                val currentDateKey = DailyTestTimeUtils.getCurrentDateKey()
                val isMidnightCrossed = currentDateKey != repository.dailyDateKey.value ||
                    currentDateKey != lastDateKey ||
                    (millis > previousMillis + 60_000L)
                lastDateKey = currentDateKey
                previousMillis = millis
                if (isMidnightCrossed) {
                    repository.checkDailyMidnightReset(force = true)
                    repository.checkMonthlyReset()
                } else if (tickCount % 30 == 0) {
                    repository.checkDailyMidnightReset(force = false)
                    repository.checkMonthlyReset()
                }
                tickCount++
                delay(1000)
            }
        }
    }

    fun shareDailyResult(context: Context) {
        val best = repository.dailyBestResult.value ?: repository.dailyLeaderboardState.value.currentUserBest ?: return
        val rank = repository.dailyLeaderboardState.value.currentUserRank ?: best.rank
        val rankStr = if (rank > 0) "#$rank" else "Rank #${best.rank}"
        val scoreStr = "${best.bestScore.toInt()}/${best.totalMarks.toInt()}"
        val timeStr = DailyTestTimeUtils.formatTimeTaken(best.bestCompletionTimeSeconds)
        val totalPart = "%,d".format(repository.dailyLeaderboardState.value.totalParticipants)

        val shareText = """
            I scored $scoreStr in today's Daily Test for ${best.studentClass}! 🏆
            Rank: $rankStr (out of $totalPart students)
            Time: $timeStr

            Can you beat my score? Attempt today's Daily Test!
            ${DailyTestTimeUtils.APP_WEBSITE_URL}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Daily Test Result — $rankStr")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Result via"))
    }

    fun generateCertificate(context: Context): File? {
        val best = repository.dailyBestResult.value
            ?: repository.dailyLeaderboardState.value.currentUserBest
            ?: return null
        return CertificateGenerator.generateCertificate(context, best)
    }

    fun shareCertificate(context: Context, file: File) {
        val best = repository.dailyBestResult.value ?: repository.dailyLeaderboardState.value.currentUserBest
        val studentName = best?.studentName ?: repository.student.value.name
        val score = "${best?.bestScore?.toInt() ?: 0}/${best?.totalMarks?.toInt() ?: 0}"
        CertificateGenerator.shareCertificate(
            context = context,
            file = file,
            studentName = studentName,
            score = score
        )
    }

    fun viewCertificate(context: Context, file: File) {
        val best = repository.dailyBestResult.value ?: repository.dailyLeaderboardState.value.currentUserBest
        val studentName = best?.studentName ?: repository.student.value.name
        val score = "${best?.bestScore?.toInt() ?: 0}/${best?.totalMarks?.toInt() ?: 0}"
        CertificateGenerator.viewCertificate(context, file, studentName, score)
    }

    suspend fun checkSession(): Boolean {
        return repository.checkSession()
    }

    fun selectCourseAndClass(courseName: String, classId: String, className: String) {
        repository.selectCourseAndClass(courseName, classId, className)
    }

    fun saveInitialProfile(name: String, courseName: String, classId: String, className: String) {
        repository.saveInitialProfile(name, courseName, classId, className)
    }

    fun updateProfileName(name: String) {
        repository.updateProfileName(name)
    }

    fun deleteProfile() {
        repository.deleteProfile()
    }

    suspend fun getOrGenerateDailyRollNo(customStudentName: String? = null): String {
        return repository.getOrGenerateDailyRollNo(customStudentName)
    }

    fun hasAttemptedDailyTestToday(): Boolean {
        val todayKey = repository.dailyDateKey.value
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        return prefs?.hasAttemptedDailyTestToday(todayKey) == true
    }

    fun getDailyAttemptDetails(): com.jacboard.mcqmaster.util.DailySavedAttemptDetails? {
        val todayKey = repository.dailyDateKey.value
        val prefs = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()
        return prefs?.getDailyAttemptDetails(todayKey)
    }

    fun resetDailySessionForTesting(forceNewDateKey: String? = null) {
        repository.resetDailySessionForTesting(forceNewDateKey)
    }

    fun getClasses(): List<ClassData> = repository.getClasses()

    fun getSelectedClass(): ClassData? = repository.getSelectedClass()

    fun switchClass(classId: String) {
        repository.switchClass(classId)
    }

    fun getSubjects(): List<Subject> = repository.getSubjects()

    fun getSubject(subjectId: String): Subject? = repository.getSubject(subjectId)

    fun getChapters(subjectId: String) = repository.getChapters(subjectId)

    suspend fun fetchChapters(subjectId: String): List<com.jacboard.mcqmaster.model.Chapter> {
        return repository.fetchChaptersForSubject(subjectId)
    }

    fun getCategories(): List<TestCategory> = repository.getCategories()

    fun getTests(categoryId: String? = null, subjectId: String? = null, chapterId: String? = null): List<Test> {
        return repository.getTests(categoryId, subjectId, chapterId)
    }

    fun getDailyTest(): Test? = repository.getDailyTest()

    fun checkDailyEligibility() {
        repository.checkDailyEligibility()
    }



    fun resetDailyLeaderboardAtMidnight() {
        repository.resetDailyLeaderboardAtMidnight()
    }


    fun getTestById(testId: String): Test? = repository.getTestById(testId)

    fun getQuestionsForTest(testId: String): List<Question> = repository.getQuestionsForTest(testId)

    suspend fun prefetchQuestions(testId: String): List<Question> = repository.prefetchQuestionsForTest(testId)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startOrResumeTest(testId: String, mode: com.jacboard.mcqmaster.model.TestMode = com.jacboard.mcqmaster.model.TestMode.EXAM): Attempt {
        return repository.startOrResumeAttempt(testId, mode)
    }

    fun clearActiveAttempt() {
        repository.clearActiveAttempt()
    }

    fun restartTest(testId: String, mode: com.jacboard.mcqmaster.model.TestMode = com.jacboard.mcqmaster.model.TestMode.EXAM): Attempt {
        return repository.restartAttempt(testId, mode)
    }

    fun recordAnswer(questionIndex: Int, optionIndex: Int) {
        repository.recordAnswer(questionIndex, optionIndex)
    }

    fun clearAnswer(questionIndex: Int) {
        repository.clearAnswer(questionIndex)
    }

    fun toggleMarkForReview(questionIndex: Int) {
        repository.toggleMarkForReview(questionIndex)
    }

    fun setCurrentQuestionIndex(index: Int) {
        repository.setCurrentQuestionIndex(index)
    }

    fun submitCurrentTest(): TestResult {
        val attempt = repository.activeAttempt.value
        val attemptId = attempt?.id ?: "attempt_${System.currentTimeMillis()}"
        return repository.submitAttempt(attemptId)
    }

    fun toggleTheme() {
        repository.setDarkTheme(!repository.isDarkTheme.value)
    }

    suspend fun saveOrUpdateTest(test: Test): Boolean {
        return repository.saveOrUpdateTest(test)
    }

    suspend fun deleteTest(testId: String): Boolean {
        return repository.deleteTest(testId)
    }

    suspend fun saveOrUpdateQuestion(question: Question): Boolean {
        return repository.saveOrUpdateQuestion(question)
    }

    suspend fun deleteQuestion(questionId: String, testId: String): Boolean {
        return repository.deleteQuestion(questionId, testId)
    }

    suspend fun saveOrUpdateBanner(banner: com.jacboard.mcqmaster.model.PromotionalBanner): Boolean {
        return repository.saveOrUpdateBanner(banner)
    }

    suspend fun deleteBanner(bannerId: String): Boolean {
        return repository.deleteBanner(bannerId)
    }

    fun markNotificationsRead() {
        repository.markNotificationsRead()
    }
}
