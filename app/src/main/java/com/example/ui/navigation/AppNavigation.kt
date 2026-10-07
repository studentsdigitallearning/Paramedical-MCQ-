package com.example.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockDataSource
import com.example.model.CategoryType
import com.example.model.Chapter
import com.example.model.DailyBestResult
import com.example.model.Subject
import com.example.model.Test
import com.example.model.TestCategory
import com.example.model.TestResult
import com.example.model.TestStatus
import com.example.ui.components.AppSidebar
import com.example.ui.components.CertificateDialog
import com.example.ui.screens.AboutUsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.ChapterSelectionScreen
import com.example.ui.screens.ClassSelectionScreen
import com.example.ui.screens.ContactUsScreen
import com.example.ui.screens.DailyTestScreen
import com.example.ui.screens.DetailedAnalysisScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MonthlyStreakRewardsScreen
import com.example.ui.screens.NoInternetScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ResetPasswordScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SubjectWiseScreen
import com.example.ui.screens.TestInstructionsScreen
import com.example.ui.screens.TestListScreen
import com.example.ui.screens.TestScreen
import com.example.ui.theme.PrimaryPurple
import com.example.util.DailyTestTimeUtils
import com.example.viewmodel.AppViewModel
import java.io.File

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object Register : Screen()
    object ForgotPassword : Screen()
    object ResetPassword : Screen()
    data class ClassSelection(val isSwitchMode: Boolean = false) : Screen()
    object Main : Screen()
    object Categories : Screen()
    object SubjectWise : Screen()
    data class ChapterSelection(val subject: Subject) : Screen()
    data class TestList(val title: String, val subtitle: String, val categoryId: String? = null, val subjectId: String? = null, val chapterId: String? = null) : Screen()
    data class TestInstructions(val test: Test) : Screen()
    data class TestExecution(val test: Test) : Screen()
    data class Result(val result: TestResult) : Screen()
    data class DetailedAnalysis(val result: TestResult) : Screen()
    object Notifications : Screen()
    object Settings : Screen()
    object HelpSupport : Screen()
    object MonthlyStreakRewards : Screen()
    object AboutUs : Screen()
    object ContactUs : Screen()
}

@Composable
fun AppNavigation(
    viewModel: AppViewModel
) {
    // Navigation Stack for proper hierarchical step-by-step back navigation
    var backStack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Splash)) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Main

    fun navigateTo(screen: Screen) {
        backStack = backStack + screen
    }

    fun popBack(): Boolean {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
            return true
        }
        return false
    }

    fun replaceTop(screen: Screen) {
        backStack = if (backStack.isNotEmpty()) {
            backStack.dropLast(1) + screen
        } else {
            listOf(screen)
        }
    }

    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Leaderboard, 2: Profile

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val student by viewModel.student.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val dailyCountdown by viewModel.dailyCountdown.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val activeAttempt by viewModel.activeAttempt.collectAsState()
    val latestResult by viewModel.latestResult.collectAsState()
    val dailyState by viewModel.dailyLeaderboardState.collectAsState()
    val dailyBestResult by viewModel.dailyBestResult.collectAsState()
    val monthlyStreakState by viewModel.monthlyStreakState.collectAsState()

    // Dynamic Supabase collections
    val classes by viewModel.classes.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val tests by viewModel.tests.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val chaptersMap by viewModel.chaptersState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isCheckingConnection by viewModel.isCheckingConnection.collectAsState()

    var certificateDialogData by remember {
        mutableStateOf<Pair<DailyBestResult, File>?>(null)
    }

    // Double-Back to Exit state when on root Home dashboard
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val currentClass = classes.find { it.id == student.selectedClassId } ?: viewModel.getSelectedClass()
    val unreadNotificationsCount = notifications.count { !it.isRead }

    var isSidebarOpen by remember { mutableStateOf(false) }

    // Intercept Back on Main screen (first close sidebar, or switch to Home tab, or show exit dialog)
    BackHandler(enabled = currentScreen is Screen.Main) {
        when {
            isSidebarOpen -> isSidebarOpen = false
            selectedBottomTab != 0 -> selectedBottomTab = 0
            else -> showExitConfirmDialog = true
        }
    }

    // Auto-recovery when internet connection is restored
    androidx.compose.runtime.LaunchedEffect(isOnline) {
        if (isOnline) {
            viewModel.refreshDataFromSupabase()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState is Screen.Main && initialState !is Screen.Main) {
                    // Navigating back to Main screen
                    (slideInHorizontally(animationSpec = tween(280)) { -it / 4 } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(240)) { it } + fadeOut(animationSpec = tween(200)))
                } else if (initialState is Screen.Main && targetState !is Screen.Main) {
                    // Navigating forward into a sub-screen
                    (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(260)) { -it / 4 } + fadeOut(animationSpec = tween(220)))
                } else {
                    // Smooth fade and slight slide between other screens
                    (slideInHorizontally(animationSpec = tween(260)) { it / 5 } + fadeIn(animationSpec = tween(260)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { -it / 5 } + fadeOut(animationSpec = tween(200)))
                }
            },
            label = "screen_transition"
        ) { screen ->
        when (screen) {
            is Screen.Splash -> {
                SplashScreen(
                    onSplashFinished = {
                        coroutineScope.launch {
                            val hasValidSession = viewModel.checkSession()
                            if (hasValidSession) {
                                backStack = listOf(Screen.Main)
                            } else {
                                backStack = listOf(Screen.Login)
                            }
                        }
                    }
                )
            }

            is Screen.Login -> {
                LoginScreen(
                    onLogin = { email, password ->
                        val res = viewModel.login(email, password)
                        if (res.isSuccess) {
                            backStack = listOf(Screen.Main)
                        }
                        res
                    },
                    onGoogleLogin = {
                        coroutineScope.launch {
                            val googleResult = com.example.util.GoogleSignInHelper.initiateGoogleSignIn(context)
                            if (googleResult.isSuccess) {
                                val accountData = googleResult.getOrThrow()
                                val authRes = viewModel.loginWithGoogle(
                                    idToken = accountData.idToken,
                                    nonce = accountData.nonce,
                                    fallbackName = accountData.displayName
                                )
                                if (authRes.isSuccess) {
                                    backStack = listOf(Screen.Main)
                                } else {
                                    val error = authRes.exceptionOrNull()?.message ?: "Google authentication failed"
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val error = googleResult.exceptionOrNull()?.message ?: "Google sign in cancelled"
                                if (!error.contains("cancel", ignoreCase = true)) {
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onNavigateToRegister = {
                        navigateTo(Screen.Register)
                    },
                    onForgotPasswordClick = {
                        navigateTo(Screen.ForgotPassword)
                    }
                )
            }

            is Screen.Register -> {
                BackHandler { popBack() }
                RegisterScreen(
                    onRegister = { fullName, email, password ->
                        val res = viewModel.register(fullName, email, password)
                        if (res.isSuccess && !res.getOrThrow().isEmailConfirmationRequired) {
                            backStack = listOf(Screen.Main)
                        }
                        res
                    },
                    onGoogleLogin = {
                        coroutineScope.launch {
                            val googleResult = com.example.util.GoogleSignInHelper.initiateGoogleSignIn(context)
                            if (googleResult.isSuccess) {
                                val accountData = googleResult.getOrThrow()
                                val authRes = viewModel.loginWithGoogle(
                                    idToken = accountData.idToken,
                                    nonce = accountData.nonce,
                                    fallbackName = accountData.displayName
                                )
                                if (authRes.isSuccess) {
                                    backStack = listOf(Screen.Main)
                                } else {
                                    val error = authRes.exceptionOrNull()?.message ?: "Google authentication failed"
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val error = googleResult.exceptionOrNull()?.message ?: "Google sign in cancelled"
                                if (!error.contains("cancel", ignoreCase = true)) {
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onNavigateToLogin = {
                        popBack()
                    },
                    onBackClick = {
                        popBack()
                    }
                )
            }

            is Screen.ForgotPassword -> {
                BackHandler { popBack() }
                ForgotPasswordScreen(
                    onResetPassword = { email ->
                        viewModel.resetPassword(email)
                    },
                    onVerifyOtpAndReset = { email, otp, newPassword ->
                        viewModel.resetPasswordWithOtp(email, otp, newPassword)
                    },
                    onBackClick = {
                        popBack()
                    }
                )
            }

            is Screen.ResetPassword -> {
                BackHandler { popBack() }
                ResetPasswordScreen(
                    onUpdatePassword = { newPassword ->
                        viewModel.updatePassword(newPassword)
                    },
                    onBackClick = {
                        popBack()
                    },
                    onSuccessLoginRedirect = {
                        backStack = listOf(Screen.Login)
                    }
                )
            }

            is Screen.ClassSelection -> {
                BackHandler(enabled = screen.isSwitchMode) {
                    popBack()
                }
                ClassSelectionScreen(
                    classes = classes,
                    currentClassId = student.selectedClassId,
                    isLoading = isSyncing,
                    errorMessage = syncError,
                    onRetry = { viewModel.refreshDataFromSupabase() },
                    onClassSelected = { classId ->
                        viewModel.switchClass(classId)
                        if (screen.isSwitchMode) {
                            popBack()
                        } else {
                            backStack = listOf(Screen.Main)
                        }
                    },
                    onBackClick = if (screen.isSwitchMode) {
                        { popBack() }
                    } else null
                )
            }

            is Screen.Main -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            StandardBottomNavigationBar(
                                selectedTab = selectedBottomTab,
                                unreadCount = unreadNotificationsCount,
                                onTabSelected = { selectedBottomTab = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            when (selectedBottomTab) {
                                0 -> {
                                    HomeScreen(
                                        student = student,
                                        currentClass = currentClass,
                                        dailyCountdown = dailyCountdown,
                                        unreadNotificationsCount = unreadNotificationsCount,
                                        categories = categories,
                                        isDarkTheme = isDarkTheme,
                                        onClassChipClick = {
                                            navigateTo(Screen.ClassSelection(isSwitchMode = true))
                                        },
                                        onNotificationsClick = {
                                            navigateTo(Screen.Notifications)
                                        },
                                        onDailyChallengeStart = {
                                            val dailyTest = viewModel.getDailyTest() 
                                                ?: viewModel.getTests(categoryId = "cat_daily").firstOrNull() 
                                                ?: viewModel.getTests().firstOrNull() 
                                                ?: MockDataSource.defaultFallbackTest
                                            navigateTo(Screen.TestInstructions(dailyTest))
                                        },
                                        onCategoryClick = { cat ->
                                            when (cat.type) {
                                                CategoryType.DAILY -> selectedBottomTab = 1
                                                CategoryType.SUBJECT -> navigateTo(Screen.SubjectWise)
                                                CategoryType.MOCK -> navigateTo(Screen.TestList("Mock Tests", currentClass.name, categoryId = "cat_mock"))
                                                CategoryType.PREVIOUS -> navigateTo(Screen.TestList("Previous Year Papers", currentClass.name, categoryId = "cat_previous"))
                                                else -> navigateTo(Screen.TestList(cat.name, currentClass.name, categoryId = cat.id))
                                            }
                                        },
                                        onViewAllCategories = {
                                            navigateTo(Screen.Categories)
                                        },
                                        onViewLeaderboard = {
                                            selectedBottomTab = 2
                                        },
                                        onMenuClick = {
                                            isSidebarOpen = true
                                        }
                                    )
                                }
                                1 -> {
                                    val dailyTest = viewModel.getDailyTest() 
                                        ?: viewModel.getTests(categoryId = "cat_daily").firstOrNull() 
                                        ?: viewModel.getTests().firstOrNull() 
                                        ?: MockDataSource.defaultFallbackTest
                                    DailyTestScreen(
                                        dailyTest = dailyTest,
                                        currentClass = currentClass,
                                        dailyCountdown = dailyCountdown,
                                        isDarkTheme = isDarkTheme,
                                        isLoading = isSyncing && tests.isEmpty(),
                                        onStartDailyTestClick = { test ->
                                            navigateTo(Screen.TestInstructions(test))
                                        }
                                    )
                                }
                                2 -> {
                                    androidx.compose.runtime.LaunchedEffect(Unit) {
                                        viewModel.refreshLeaderboard()
                                    }
                                    LeaderboardScreen(
                                        currentClassText = currentClass.name,
                                        dailyState = dailyState,
                                        dailyCountdown = dailyCountdown,
                                        isLoading = isSyncing && dailyState.top100.isEmpty(),
                                        onStartOrRetakeDailyTest = {
                                            val dailyTest = viewModel.getDailyTest() 
                                                ?: viewModel.getTests(categoryId = "cat_daily").firstOrNull() 
                                                ?: viewModel.getTests().firstOrNull() 
                                                ?: MockDataSource.defaultFallbackTest
                                            navigateTo(Screen.TestInstructions(dailyTest))
                                        },
                                        onShareDailyResult = { viewModel.shareDailyResult(context) },
                                        onGenerateCertificate = {
                                            val best = dailyState.currentUserBest ?: dailyBestResult
                                            if (best != null) {
                                                val file = viewModel.generateCertificate(context)
                                                if (file != null) {
                                                    certificateDialogData = Pair(best, file)
                                                }
                                            }
                                        }
                                    )
                                }
                                3 -> {
                                    ProfileScreen(
                                        student = student,
                                        currentClass = currentClass,
                                        isDarkTheme = isDarkTheme,
                                        isLoading = isSyncing && student.name.isBlank(),
                                        onToggleTheme = { viewModel.toggleTheme() },
                                        onSwitchClassClick = {
                                            navigateTo(Screen.ClassSelection(isSwitchMode = true))
                                        },
                                        onSettingsClick = {
                                            navigateTo(Screen.Settings)
                                        },
                                        onHelpClick = {
                                            navigateTo(Screen.HelpSupport)
                                        },
                                        onStreakClick = {
                                            navigateTo(Screen.MonthlyStreakRewards)
                                        },
                                        onLogoutClick = {
                                            coroutineScope.launch {
                                                viewModel.logout()
                                                backStack = listOf(Screen.Login)
                                            }
                                        },
                                        onDeleteAccountClick = {
                                            coroutineScope.launch {
                                                val res = viewModel.deleteAccount()
                                                if (res.isSuccess) {
                                                    Toast.makeText(context, "Account and personal data permanently deleted", Toast.LENGTH_LONG).show()
                                                }
                                                backStack = listOf(Screen.Login)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Slide-out App Sidebar overlay
                    AppSidebar(
                        isOpen = isSidebarOpen,
                        onClose = { isSidebarOpen = false },
                        student = student,
                        currentClass = currentClass,
                        onSwitchClassClick = {
                            navigateTo(Screen.ClassSelection(isSwitchMode = true))
                        },
                        onNavigateAboutUs = {
                            navigateTo(Screen.AboutUs)
                        },
                        onNavigateContactUs = {
                            navigateTo(Screen.ContactUs)
                        },
                        onLogoutClick = {
                            backStack = listOf(Screen.Login)
                        }
                    )
                }
            }

            is Screen.Categories -> {
                BackHandler { popBack() }
                CategoriesScreen(
                    categories = categories,
                    onCategoryClick = { cat ->
                        when (cat.type) {
                            CategoryType.DAILY -> {
                                popBack()
                                selectedBottomTab = 1
                            }
                            CategoryType.SUBJECT -> navigateTo(Screen.SubjectWise)
                            CategoryType.MOCK -> navigateTo(Screen.TestList("Mock Tests", currentClass.name, categoryId = "cat_mock"))
                            CategoryType.PREVIOUS -> navigateTo(Screen.TestList("Previous Year Papers", currentClass.name, categoryId = "cat_previous"))
                            else -> navigateTo(Screen.TestList(cat.name, currentClass.name, categoryId = cat.id))
                        }
                    },
                    onBackClick = { popBack() }
                )
            }

            is Screen.SubjectWise -> {
                BackHandler { popBack() }
                SubjectWiseScreen(
                    currentClass = currentClass,
                    subjects = subjects,
                    isSyncing = isSyncing,
                    syncError = syncError,
                    onRetry = { viewModel.refreshDataFromSupabase() },
                    onSubjectClick = { subject ->
                        navigateTo(Screen.ChapterSelection(subject))
                    },
                    onBackClick = { popBack() }
                )
            }

            is Screen.ChapterSelection -> {
                BackHandler { popBack() }
                val currentChapters = chaptersMap[screen.subject.id] ?: viewModel.getChapters(screen.subject.id)
                androidx.compose.runtime.LaunchedEffect(screen.subject.id) {
                    viewModel.fetchChapters(screen.subject.id)
                }
                ChapterSelectionScreen(
                    subject = screen.subject,
                    chapters = currentChapters,
                    isLoading = isSyncing,
                    errorMessage = syncError,
                    onRetry = {
                        coroutineScope.launch {
                            viewModel.fetchChapters(screen.subject.id)
                        }
                    },
                    onChapterClick = { chapter ->
                        navigateTo(
                            Screen.TestList(
                                title = chapter.name,
                                subtitle = "${screen.subject.name} • ${currentClass.name}",
                                chapterId = chapter.id
                            )
                        )
                    },
                    onBackClick = { popBack() }
                )
            }

            is Screen.TestList -> {
                BackHandler { popBack() }
                val currentTests = viewModel.getTests(screen.categoryId, screen.subjectId, screen.chapterId)

                TestListScreen(
                    title = screen.title,
                    subtitle = screen.subtitle,
                    tests = currentTests,
                    isSyncing = isSyncing,
                    syncError = syncError,
                    onRetry = { viewModel.refreshDataFromSupabase() },
                    onTestClick = { test ->
                        navigateTo(Screen.TestInstructions(test))
                    },
                    onBackClick = { popBack() }
                )
            }

            is Screen.TestInstructions -> {
                BackHandler { popBack() }
                TestInstructionsScreen(
                    test = screen.test,
                    onStartTestClick = { mode ->
                        coroutineScope.launch {
                            viewModel.prefetchQuestions(screen.test.id)
                            viewModel.startOrResumeTest(screen.test.id, mode)
                            navigateTo(Screen.TestExecution(screen.test))
                        }
                    },
                    onBackClick = { popBack() }
                )
            }

            is Screen.TestExecution -> {
                val attempt = activeAttempt ?: viewModel.startOrResumeTest(screen.test.id)
                val questions = attempt.shuffledQuestions.ifEmpty { viewModel.getQuestionsForTest(screen.test.id) }

                TestScreen(
                    test = screen.test,
                    attempt = attempt,
                    questions = questions,
                    onAnswerSelected = { qIdx, optIdx ->
                        viewModel.recordAnswer(qIdx, optIdx)
                    },
                    onClearAnswer = { qIdx ->
                        viewModel.clearAnswer(qIdx)
                    },
                    onToggleMarkForReview = { qIdx ->
                        viewModel.toggleMarkForReview(qIdx)
                    },
                    onQuestionIndexChanged = { qIdx ->
                        viewModel.setCurrentQuestionIndex(qIdx)
                    },
                    onSubmitTest = {
                        val result = viewModel.submitCurrentTest()
                        // Cleanly replace TestExecution & TestInstructions in the stack with the Result screen
                        val cleanStack = backStack.filterNot { it is Screen.TestExecution || it is Screen.TestInstructions }
                        backStack = if (cleanStack.isEmpty()) listOf(Screen.Main, Screen.Result(result)) else cleanStack + Screen.Result(result)
                    },
                    onLeaveTest = {
                        viewModel.clearActiveAttempt()
                        popBack()
                    }
                )
            }

            is Screen.Result -> {
                BackHandler { popBack() }
                val isDaily = screen.result.testId == "test_daily_10" || screen.result.testTitle.contains("Daily", ignoreCase = true)
                ResultScreen(
                    result = screen.result,
                    onRestartTest = {
                        val test = viewModel.getTestById(screen.result.testId) 
                            ?: viewModel.getTests().firstOrNull() 
                            ?: MockDataSource.defaultFallbackTest
                        viewModel.restartTest(test.id)
                        replaceTop(Screen.TestExecution(test))
                    },
                    onViewDetailedAnalysis = {
                        navigateTo(Screen.DetailedAnalysis(screen.result))
                    },
                    onRetryWrongQuestions = {
                        val wrongQuestions = screen.result.questionsReview.filter { !it.isCorrect }.map { it.question }
                        val retryTest = screen.result.run {
                            Test(
                                id = "retry_${testId}",
                                classId = "class_10",
                                categoryId = "cat_mock",
                                title = "Retry: $testTitle",
                                subtitle = "Re-attempt Missed Questions",
                                questionCount = wrongQuestions.size,
                                durationMinutes = 15,
                                totalMarks = wrongQuestions.size * 1f,
                                negativeMarking = 0f
                            )
                        }
                        viewModel.startOrResumeTest(retryTest.id, com.example.model.TestMode.EXAM)
                        replaceTop(Screen.TestExecution(retryTest))
                    },
                    onBackClick = {
                        popBack()
                    },
                    onShareResult = { viewModel.shareDailyResult(context) },
                    onGenerateCertificate = {
                        val file = viewModel.generateCertificate(context)
                        val best = dailyState.currentUserBest ?: dailyBestResult
                        if (file != null && best != null) {
                            certificateDialogData = Pair(best, file)
                        }
                    },
                    onRetakeDailyTest = {
                        val dailyTest = viewModel.getTests().find { it.isDaily } 
                            ?: viewModel.getTests().firstOrNull() 
                            ?: MockDataSource.defaultFallbackTest
                        viewModel.restartTest(dailyTest.id)
                        replaceTop(Screen.TestExecution(dailyTest))
                    },
                    isDailyTest = isDaily,
                    dailyBestResult = dailyBestResult,
                    totalParticipants = dailyState.totalParticipants
                )
            }

            is Screen.DetailedAnalysis -> {
                BackHandler { popBack() }
                DetailedAnalysisScreen(
                    result = screen.result,
                    onBackClick = { popBack() }
                )
            }

            is Screen.Notifications -> {
                BackHandler { popBack() }
                NotificationsScreen(
                    notifications = notifications,
                    onMarkAllRead = { viewModel.markNotificationsRead() },
                    onBackClick = { popBack() }
                )
            }

            is Screen.Settings -> {
                BackHandler { popBack() }
                val voiceLanguage by viewModel.voiceReadingLanguage.collectAsState()
                val voiceGender by viewModel.voiceGender.collectAsState()

                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { viewModel.toggleTheme() },
                    voiceLanguage = voiceLanguage,
                    voiceGender = voiceGender,
                    onSetVoiceLanguage = { viewModel.setVoiceReadingLanguage(it) },
                    onSetVoiceGender = { viewModel.setVoiceGender(it) },
                    onBackClick = { popBack() }
                )
            }

            is Screen.HelpSupport -> {
                BackHandler { popBack() }
                HelpSupportScreen(
                    onBackClick = { popBack() }
                )
            }

            is Screen.MonthlyStreakRewards -> {
                BackHandler { popBack() }
                MonthlyStreakRewardsScreen(
                    monthlyState = monthlyStreakState,
                    isDarkTheme = isDarkTheme,
                    onBackClick = { popBack() },
                    onStartDailyTest = {
                        val dailyTest = viewModel.getDailyTest() 
                            ?: viewModel.getTests(categoryId = "cat_daily").firstOrNull() 
                            ?: viewModel.getTests().firstOrNull() 
                            ?: MockDataSource.defaultFallbackTest
                        navigateTo(Screen.TestInstructions(dailyTest))
                    }
                )
            }

            is Screen.AboutUs -> {
                BackHandler { popBack() }
                AboutUsScreen(
                    onBackClick = { popBack() }
                )
            }

            is Screen.ContactUs -> {
                BackHandler { popBack() }
                ContactUsScreen(
                    onBackClick = { popBack() }
                )
            }
        }
    }

    // Single Unified Dedicated Full-Screen No Internet Screen Overlay for offline states
    androidx.compose.animation.AnimatedVisibility(
        visible = !isOnline && currentScreen !is Screen.Splash && currentScreen !is Screen.TestExecution,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        NoInternetScreen(
            onRetryClick = {
                viewModel.retryConnection { success ->
                    if (success) {
                        viewModel.refreshDataFromSupabase()
                    }
                }
            },
            onBackClick = null,
            isChecking = isCheckingConnection
        )
    }

    // Double-Back Exit Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Exit App?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to exit?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitConfirmDialog = false }
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Official PDF Certificate Preview Dialog
    certificateDialogData?.let { (result, file) ->
        CertificateDialog(
            result = result,
            top10 = dailyState.top100.take(10),
            pdfFile = file,
            onDismiss = { certificateDialogData = null },
            onOpenPdf = { viewModel.openCertificate(context, file) },
            onSharePdf = { viewModel.shareCertificate(context, file) },
            onDownloadPdf = { viewModel.downloadCertificate(context, file) }
        )
    }
    }
}

@Composable
fun StandardBottomNavigationBar(
    selectedTab: Int,
    unreadCount: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_bottom_navigation_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                NavigationItemData("Home", Icons.Default.Home, "tab_home", 0),
                NavigationItemData("Daily Test", Icons.Default.EmojiEvents, "tab_daily_test", 0),
                NavigationItemData("Leaderboard", Icons.Default.BarChart, "tab_leaderboard", 0),
                NavigationItemData("Profile", Icons.Default.Person, "tab_profile", 0)
            )

            items.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                val itemColor = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(index) }
                        .testTag(item.tag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (item.badgeCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = PrimaryPurple,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (item.badgeCount > 9) "9+" else item.badgeCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = itemColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = itemColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = itemColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

data class NavigationItemData(
    val title: String,
    val icon: ImageVector,
    val tag: String,
    val badgeCount: Int = 0
)
