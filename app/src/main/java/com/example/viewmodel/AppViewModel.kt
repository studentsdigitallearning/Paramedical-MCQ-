package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.MockDataSource
import com.example.model.Attempt
import com.example.model.ClassData
import com.example.model.DailyBestResult
import com.example.model.DailyLeaderboardEntry
import com.example.model.DailyLeaderboardState
import com.example.model.DailyTestAttempt
import com.example.model.Difficulty
import com.example.model.NotificationItem
import com.example.model.Question
import com.example.model.Student
import com.example.model.Subject
import com.example.model.Test
import com.example.model.TestCategory
import com.example.model.TestResult
import com.example.model.TestStatus
import com.example.util.CertificateGenerator
import com.example.util.DailyTestTimeUtils
import com.example.util.NetworkMonitor
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

    init {
        com.example.data.supabase.SupabaseAuthStorage.init(application.applicationContext)
    }

    private val networkMonitor = NetworkMonitor.getInstance(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private val _isCheckingConnection = MutableStateFlow(false)
    val isCheckingConnection: StateFlow<Boolean> = _isCheckingConnection.asStateFlow()

    val student: StateFlow<Student> = repository.student
    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
    val activeAttempt: StateFlow<Attempt?> = repository.activeAttempt
    val latestResult: StateFlow<TestResult?> = repository.latestResult

    val authState: StateFlow<com.example.data.supabase.AuthState> = repository.authState
    val isAuthenticated: StateFlow<Boolean> = repository.isAuthenticated

    // Dynamic Supabase flows (Requirement 1 & 11)
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val syncError: StateFlow<String?> = repository.syncError
    val classes: StateFlow<List<ClassData>> = repository.classes
    val subjects: StateFlow<List<Subject>> = repository.subjects
    val tests: StateFlow<List<Test>> = repository.tests
    val categories: StateFlow<List<TestCategory>> = repository.categories
    val chaptersState: StateFlow<Map<String, List<com.example.model.Chapter>>> = repository.chaptersState

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

    fun refreshLeaderboard() {
        if (networkMonitor.isCurrentlyConnected()) {
            repository.refreshLeaderboard()
        }
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
    val dailyEligibility: StateFlow<com.example.model.DailyEligibilityResult> = repository.dailyEligibility

    // Monthly Streak and Rewards state flow
    val monthlyStreakState: StateFlow<com.example.model.MonthlyStreakState> = repository.monthlyStreakState

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
        viewModelScope.launch {
            while (true) {
                val millis = DailyTestTimeUtils.getMillisUntilMidnightIST()
                _dailyCountdown.value = DailyTestTimeUtils.formatCountdown(millis)
                repository.checkDailyMidnightReset()
                repository.checkMonthlyReset()
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
        val top10 = repository.dailyLeaderboardState.value.top100.take(10)
        return CertificateGenerator.generateCertificate(context, best, top10)
    }

    fun downloadCertificate(context: Context, file: File): Boolean {
        return CertificateGenerator.downloadCertificateToDownloads(context, file)
    }

    fun shareCertificate(context: Context, file: File) {
        val best = repository.dailyBestResult.value ?: repository.dailyLeaderboardState.value.currentUserBest
        CertificateGenerator.shareCertificate(
            context = context,
            file = file,
            studentName = best?.studentName ?: repository.student.value.name,
            rank = best?.rank ?: (repository.dailyLeaderboardState.value.currentUserRank ?: 1)
        )
    }

    fun openCertificate(context: Context, file: File) {
        CertificateGenerator.openCertificate(context, file)
    }

    suspend fun checkSession(): Boolean {
        return repository.checkSession()
    }

    suspend fun login(email: String, password: String): Result<Student> {
        return repository.login(email, password)
    }

    suspend fun loginWithGoogle(idToken: String, nonce: String? = null, fallbackName: String? = null): Result<Student> {
        return repository.loginWithGoogle(idToken, nonce, fallbackName)
    }

    suspend fun register(fullName: String, email: String, password: String): Result<com.example.data.supabase.RegistrationResult> {
        return repository.register(fullName, email, password)
    }

    suspend fun resetPassword(email: String): Result<String> {
        return repository.resetPassword(email)
    }

    suspend fun verifyRecoveryOtp(email: String, token: String): Result<com.example.data.supabase.SupabaseSession> {
        return repository.verifyRecoveryOtp(email, token)
    }

    suspend fun resetPasswordWithOtp(email: String, token: String, newPassword: String): Result<Boolean> {
        return repository.resetPasswordWithOtp(email, token, newPassword)
    }

    suspend fun updatePassword(newPassword: String, token: String? = null): Result<Boolean> {
        return repository.updatePassword(newPassword, token)
    }

    suspend fun logout(): Result<Boolean> {
        return repository.logout()
    }

    suspend fun deleteAccount(): Result<Boolean> {
        return repository.deleteAccount()
    }

    fun resetDailySessionForTesting(forceNewDateKey: String? = null) {
        repository.resetDailySessionForTesting(forceNewDateKey)
    }

    fun getClasses(): List<ClassData> = repository.getClasses()

    fun getSelectedClass(): ClassData = repository.getSelectedClass()

    fun switchClass(classId: String) {
        repository.switchClass(classId)
    }

    fun getSubjects(): List<Subject> = repository.getSubjects()

    fun getSubject(subjectId: String): Subject? = repository.getSubject(subjectId)

    fun getChapters(subjectId: String) = repository.getChapters(subjectId)

    suspend fun fetchChapters(subjectId: String): List<com.example.model.Chapter> {
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

    fun refreshLeaderboard(className: String? = null) {
        repository.refreshLeaderboard(className)
    }

    fun getTestById(testId: String): Test? = repository.getTestById(testId)

    fun getQuestionsForTest(testId: String): List<Question> = repository.getQuestionsForTest(testId)

    suspend fun prefetchQuestions(testId: String): List<Question> = repository.prefetchQuestionsForTest(testId)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startOrResumeTest(testId: String, mode: com.example.model.TestMode = com.example.model.TestMode.EXAM): Attempt {
        return repository.startOrResumeAttempt(testId, mode)
    }

    fun clearActiveAttempt() {
        repository.clearActiveAttempt()
    }

    fun restartTest(testId: String, mode: com.example.model.TestMode = com.example.model.TestMode.EXAM): Attempt {
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

    fun markNotificationsRead() {
        repository.markNotificationsRead()
    }
}
