package com.jacboard.mcqmaster.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.jacboard.mcqmaster.model.CaptchaChallengeUiState
import com.jacboard.mcqmaster.model.CaptchaGlyph
import com.jacboard.mcqmaster.model.CaptchaVerificationResult
import com.jacboard.mcqmaster.model.ClassData
import com.jacboard.mcqmaster.model.DailyBestResult
import com.jacboard.mcqmaster.model.DailyEligibilityResult
import com.jacboard.mcqmaster.model.DailyTestAttempt
import com.jacboard.mcqmaster.model.Test
import com.jacboard.mcqmaster.ui.components.AppHeaderBar
import com.jacboard.mcqmaster.ui.components.GlassCard
import com.jacboard.mcqmaster.ui.components.GoldGradientButton
import com.jacboard.mcqmaster.ui.theme.AccentGold
import com.jacboard.mcqmaster.ui.theme.AccentOrange
import com.jacboard.mcqmaster.ui.theme.DailyChallengeDarkGradient
import com.jacboard.mcqmaster.ui.theme.DailyChallengeGradient
import com.jacboard.mcqmaster.ui.theme.ErrorRed
import com.jacboard.mcqmaster.ui.theme.PrimaryPurple
import com.jacboard.mcqmaster.ui.theme.SuccessGreen

@Composable
fun DailyTestScreen(
    dailyTest: Test?,
    currentClass: ClassData? = null,
    dailyCountdown: String,
    eligibility: DailyEligibilityResult = DailyEligibilityResult(isEligible = true),
    hasAttemptedToday: Boolean = false,
    savedAttemptDetails: com.jacboard.mcqmaster.util.DailySavedAttemptDetails? = null,
    attemptsToday: List<DailyTestAttempt> = emptyList(),
    bestResult: DailyBestResult? = null,
    studentName: String = "",
    courseName: String = "",
    currentRollNo: String? = null,
    isDarkTheme: Boolean = false,
    isLoading: Boolean = false,
    captchaState: CaptchaChallengeUiState = CaptchaChallengeUiState(),
    onRefreshCaptcha: (() -> Unit)? = null,
    onVerifyCaptchaAndStartTest: (suspend (captchaAnswer: String, studentName: String) -> CaptchaVerificationResult)? = null,
    onRequestRollNumber: (suspend () -> String)? = null,
    onUpdateStudentName: ((String) -> Unit)? = null,
    onStartDailyTestClick: (Test) -> Unit,
    onViewResultClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val isAlreadyAttempted = dailyTest != null && dailyTest.questionCount > 0 && (!eligibility.isEligible || hasAttemptedToday || attemptsToday.isNotEmpty())
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var resolvedRollNo by androidx.compose.runtime.remember(currentRollNo) {
        androidx.compose.runtime.mutableStateOf(currentRollNo ?: "")
    }

    val effectiveRollNo = resolvedRollNo.ifBlank {
        currentRollNo ?: ""
    }

    var showRegistrationDialog by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var registrationNameInput by androidx.compose.runtime.remember(studentName) {
        androidx.compose.runtime.mutableStateOf(if (studentName.equals("Student", ignoreCase = true)) "" else studentName)
    }
    var registrationNameError by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    var captchaInput by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("")
    }
    var localCaptchaError by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    var isVerifyingCaptcha by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    LaunchedEffect(showRegistrationDialog) {
        if (showRegistrationDialog) {
            captchaInput = ""
            localCaptchaError = null
            onRefreshCaptcha?.invoke()
        }
    }

    LaunchedEffect(captchaState.challengeId) {
        captchaInput = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        AppHeaderBar(
            title = "Daily Test",
            subtitle = "${currentClass?.name ?: "All Courses"} • Today's Challenge",
            onBackClick = onBackClick
        )

        if (isLoading) {
            com.jacboard.mcqmaster.ui.components.DailyTestSkeleton(
                modifier = Modifier.fillMaxSize()
            )
        } else if (isAlreadyAttempted) {
            // One Daily Test Attempt Per Day across all courses
            val displayRollNo = eligibility.previousAttemptRollNo
                ?: savedAttemptDetails?.rollNo
                ?: bestResult?.rollNo
                ?: ""
            val displayScore = eligibility.previousAttemptScore
                ?: savedAttemptDetails?.score
                ?: bestResult?.bestScore
                ?: 0f
            val displayTotal = eligibility.previousAttemptTotalMarks
                ?: savedAttemptDetails?.totalMarks
                ?: dailyTest?.totalMarks
                ?: 50f
            val displayCourseClass = eligibility.attemptedClassName
                ?: savedAttemptDetails?.let { "${it.courseName} • ${it.className}" }
                ?: (currentClass?.name ?: "Course")

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.12f))
                            .border(1.5.dp, SuccessGreen.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Attempt Completed",
                            tint = SuccessGreen,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Today's Daily Test Completed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You have already attempted today's Daily Test. In accordance with competition rules, only 1 attempt is allowed per student per day across all courses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Existing Attempt Details Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Your Today's Attempt Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (displayRollNo.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Roll Number:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = displayRollNo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Score Obtained:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${displayScore.toInt()} / ${displayTotal.toInt()} Marks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Course / Class:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = displayCourseClass,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryPurple.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Resets in $dailyCountdown at 12:00 AM IST",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    GoldGradientButton(
                        text = "View Result & Leaderboard",
                        onClick = { onViewResultClick?.invoke() },
                        leadingIcon = Icons.Default.BarChart,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "view_daily_result_leaderboard_button"
                    )
                }
            }
        } else if (dailyTest == null || dailyTest.questionCount <= 0) {
            // Rule 3: No daily test available for current Course+Class today
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Spacer(modifier = Modifier.height(30.dp))

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple.copy(alpha = 0.1f))
                            .border(1.5.dp, PrimaryPurple.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = "Test Not Available",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Today's Daily Test is not available.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "There is currently no Daily Test scheduled for ${currentClass?.name ?: "this course"} today. Please check back later or practice subject-wise chapter tests.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next challenge resets in $dailyCountdown",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (onBackClick != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onBackClick,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(46.dp)
                                .testTag("explore_tests_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Explore Other Tests",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Rule 1, 2, 4: Available Daily Test for Course + Class with unlimited attempts allowed
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Hero Card for Today's Test
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isDarkTheme) DailyChallengeDarkGradient else DailyChallengeGradient)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AccentGold)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "TODAY'S TEST",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Resets in $dailyCountdown",
                                        color = AccentGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = dailyTest.title,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (dailyTest.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dailyTest.subtitle,
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Stats pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatPill(
                                    icon = Icons.Default.School,
                                    text = currentClass?.name ?: "Course",
                                    modifier = Modifier.weight(1f)
                                )
                                StatPill(
                                    icon = Icons.AutoMirrored.Filled.Assignment,
                                    text = "${dailyTest.questionCount} Questions",
                                    modifier = Modifier.weight(1.3f)
                                )
                                StatPill(
                                    icon = Icons.Default.Schedule,
                                    text = "${dailyTest.durationMinutes} Mins",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Participant Name & Roll info bar (clickable to edit details)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { showRegistrationDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (studentName.isNotBlank() && !studentName.equals("Student", ignoreCase = true)) "Participant: $studentName" else "Tap to enter Student Name for Leaderboard",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Name",
                                        tint = AccentGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Edit",
                                        color = AccentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            GoldGradientButton(
                                text = "Start Daily Test Now",
                                onClick = {
                                    showRegistrationDialog = true
                                },
                                leadingIcon = Icons.Default.PlayArrow,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "start_daily_test_dedicated_button"
                            )
                        }
                    }
                }

                // Highlights & Instructions
                item {
                    Text(
                        text = "Daily Challenge Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            FeatureRow(
                                icon = Icons.Default.BarChart,
                                iconTint = PrimaryPurple,
                                title = "Class-Specific Leaderboard",
                                description = "Compete live with other ${currentClass?.name ?: "students"}. Top 100 best scores are ranked."
                            )
                            FeatureRow(
                                icon = Icons.Default.Whatshot,
                                iconTint = AccentOrange,
                                title = "Bot-Protected Fair Evaluation",
                                description = "Secure CAPTCHA verification and unique daily roll numbers ensure authentic topper rankings."
                            )
                        }
                    }
                }
            }
        }
    }

    // Enhanced Student Registration + Bot-Protected Text CAPTCHA Dialog before test start
    if (showRegistrationDialog) {
        val displayCaptchaError = localCaptchaError ?: captchaState.errorMessage
        val attemptsLeft = (captchaState.maxAttempts - captchaState.attemptsUsed).coerceAtLeast(0)
        val minutesLeft = captchaState.remainingSeconds / 60
        val secondsLeft = captchaState.remainingSeconds % 60
        val countdownFormatted = String.format(java.util.Locale.US, "%02d:%02d", minutesLeft, secondsLeft)

        AlertDialog(
            onDismissRequest = {
                if (!isVerifyingCaptcha) {
                    showRegistrationDialog = false
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(PrimaryPurple, Color(0xFF6B21A8))
                            )
                        )
                        .border(2.dp, AccentGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Daily Test Registration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete student details & security CAPTCHA to generate your temporary roll number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = registrationNameInput,
                        onValueChange = {
                            registrationNameInput = it
                            if (it.trim().length >= 2) registrationNameError = null
                        },
                        label = { Text("Full Student Name *") },
                        placeholder = { Text("e.g. Rahul Sharma") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryPurple
                            )
                        },
                        trailingIcon = {
                            if (registrationNameInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { registrationNameInput = "" }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear text",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        isError = registrationNameError != null,
                        supportingText = registrationNameError?.let { { Text(it, color = ErrorRed) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("registration_name_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Security Text CAPTCHA Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryPurple.copy(alpha = 0.06f))
                            .border(
                                width = 1.dp,
                                color = if (displayCaptchaError != null) ErrorRed.copy(alpha = 0.6f)
                                else PrimaryPurple.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Security CAPTCHA",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (captchaState.isExpired || captchaState.remainingSeconds <= 15)
                                                ErrorRed.copy(alpha = 0.14f)
                                            else PrimaryPurple.copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (captchaState.isExpired || captchaState.remainingSeconds <= 0)
                                            "Expired"
                                        else "Expires in $countdownFormatted",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (captchaState.isExpired || captchaState.remainingSeconds <= 15)
                                            ErrorRed
                                        else PrimaryPurple
                                    )
                                }
                            }

                            // CAPTCHA Canvas + Refresh Button Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CaptchaVisualBox(
                                    glyphs = captchaState.glyphs,
                                    isLoading = captchaState.isLoading,
                                    isExpired = captchaState.isExpired,
                                    isDarkTheme = isDarkTheme,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .testTag("captcha_canvas")
                                )

                                IconButton(
                                    onClick = {
                                        if (!isVerifyingCaptcha && !captchaState.isLoading) {
                                            captchaInput = ""
                                            localCaptchaError = null
                                            onRefreshCaptcha?.invoke()
                                        }
                                    },
                                    enabled = !isVerifyingCaptcha && !captchaState.isLoading,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryPurple.copy(alpha = 0.12f))
                                        .border(1.dp, PrimaryPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .testTag("refresh_captcha_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh CAPTCHA",
                                        tint = PrimaryPurple
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Type the 6-character code shown above",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Attempts left: $attemptsLeft/${captchaState.maxAttempts}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (attemptsLeft <= 2) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = captchaInput,
                                onValueChange = { raw ->
                                    val filtered = raw
                                        .uppercase(java.util.Locale.US)
                                        .filter { it.isLetterOrDigit() }
                                        .take(6)
                                    captchaInput = filtered
                                    if (localCaptchaError != null) {
                                        localCaptchaError = null
                                    }
                                },
                                label = { Text("Enter 6-Character CAPTCHA *") },
                                placeholder = { Text("e.g. A7K9M2") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = PrimaryPurple
                                    )
                                },
                                trailingIcon = {
                                    if (captchaInput.isNotEmpty()) {
                                        IconButton(onClick = { captchaInput = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear CAPTCHA input",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                isError = displayCaptchaError != null,
                                supportingText = displayCaptchaError?.let { err ->
                                    { Text(err, color = ErrorRed) }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("captcha_input_field"),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    // Test Metadata Summary Cards
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selected Course:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentClass?.name ?: "Course",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Roll Number:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryPurple.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = effectiveRollNo.ifBlank { "Issued after CAPTCHA" },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Test Date:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = com.jacboard.mcqmaster.util.DailyTestTimeUtils.getFormattedDisplayDate(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isVerifyingCaptcha || captchaState.isLoading) return@Button

                        val trimmed = registrationNameInput.trim()
                        if (trimmed.length < 2) {
                            registrationNameError = "Please enter a valid student name (at least 2 characters)"
                            return@Button
                        }

                        val normalizedCaptcha = captchaInput.trim().uppercase(java.util.Locale.US)
                        if (captchaState.isExpired || captchaState.remainingSeconds <= 0) {
                            localCaptchaError = "CAPTCHA expired. Refreshing a new code..."
                            onRefreshCaptcha?.invoke()
                            return@Button
                        }
                        if (normalizedCaptcha.length != 6) {
                            localCaptchaError = "Please enter the complete 6-character CAPTCHA code."
                            return@Button
                        }

                        coroutineScope.launch {
                            isVerifyingCaptcha = true
                            localCaptchaError = null
                            try {
                                val verifier = onVerifyCaptchaAndStartTest
                                if (verifier != null) {
                                    val result = verifier.invoke(normalizedCaptcha, trimmed)
                                    if (result.verified) {
                                        if (!result.rollNo.isNullOrBlank()) {
                                            resolvedRollNo = result.rollNo
                                        }
                                        onUpdateStudentName?.invoke(trimmed)
                                        showRegistrationDialog = false
                                        dailyTest?.let { onStartDailyTestClick(it) }
                                    } else {
                                        localCaptchaError = result.errorMessage ?: "Invalid CAPTCHA code. Please try again."
                                    }
                                } else {
                                    val issued = onRequestRollNumber?.invoke()
                                    if (!issued.isNullOrBlank()) {
                                        resolvedRollNo = issued
                                    }
                                    onUpdateStudentName?.invoke(trimmed)
                                    showRegistrationDialog = false
                                    dailyTest?.let { onStartDailyTestClick(it) }
                                }
                            } finally {
                                isVerifyingCaptcha = false
                            }
                        }
                    },
                    enabled = !isVerifyingCaptcha && !captchaState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("verify_captcha_and_start_button")
                ) {
                    if (isVerifyingCaptcha) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify & Start Test", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        if (!isVerifyingCaptcha) {
                            showRegistrationDialog = false
                        }
                    },
                    enabled = !isVerifyingCaptcha,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CaptchaVisualBox(
    glyphs: List<CaptchaGlyph>,
    isLoading: Boolean,
    isExpired: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDarkTheme) Color(0xFF1E1B2E) else Color(0xFFF3EEFF)
    val borderColor = if (isExpired) ErrorRed.copy(alpha = 0.5f) else PrimaryPurple.copy(alpha = 0.35f)
    val glyphColors = remember(isDarkTheme) {
        if (isDarkTheme) {
            listOf(
                Color(0xFFE9D5FF),
                Color(0xFFFDE68A),
                Color(0xFF93C5FD),
                Color(0xFF6EE7B7),
                Color(0xFFFCA5A5),
                Color(0xFFC4B5FD)
            )
        } else {
            listOf(
                Color(0xFF4C1D95),
                Color(0xFF7C2D12),
                Color(0xFF1E3A8A),
                Color(0xFF065F46),
                Color(0xFF831843),
                Color(0xFF581C87)
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = PrimaryPurple,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generating CAPTCHA...",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryPurple
                )
            }
        } else if (glyphs.isEmpty() || isExpired) {
            Text(
                text = if (isExpired) "CAPTCHA Expired • Tap Refresh" else "Tap Refresh to Load CAPTCHA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isExpired) ErrorRed else PrimaryPurple
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val widthPx = size.width
                val heightPx = size.height

                // Subtle security grid lines
                val gridColor = PrimaryPurple.copy(alpha = 0.10f)
                val stepX = widthPx / 8f
                for (i in 1..7) {
                    drawLine(
                        color = gridColor,
                        start = Offset(i * stepX, 0f),
                        end = Offset(i * stepX, heightPx),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                drawLine(
                    color = gridColor,
                    start = Offset(0f, heightPx * 0.35f),
                    end = Offset(widthPx, heightPx * 0.65f),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = AccentGold.copy(alpha = 0.35f),
                    start = Offset(widthPx * 0.05f, heightPx * 0.72f),
                    end = Offset(widthPx * 0.95f, heightPx * 0.28f),
                    strokeWidth = 1.8.dp.toPx()
                )

                // Render distorted 6-character glyphs on nativeCanvas
                val slotWidth = widthPx / (glyphs.size + 1)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isSubpixelText = true
                }

                glyphs.forEachIndexed { idx, glyph ->
                    val centerX = slotWidth * (idx + 1)
                    val centerY = (heightPx / 2f) + glyph.verticalOffsetDp.dp.toPx()
                    val textSizePx = 22.sp.toPx() * glyph.scale
                    paint.textSize = textSizePx
                    paint.color = glyphColors[idx % glyphColors.size].toArgb()

                    // Deterministic security dot near each glyph
                    drawCircle(
                        color = glyphColors[(idx + 2) % glyphColors.size].copy(alpha = 0.28f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(
                            x = (centerX + (glyph.rotationDegrees * 0.8f)).coerceIn(8f, widthPx - 8f),
                            y = (centerY - (glyph.verticalOffsetDp * 1.8f)).coerceIn(8f, heightPx - 8f)
                        )
                    )

                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.rotate(glyph.rotationDegrees, centerX, centerY)
                    val fontMetrics = paint.fontMetrics
                    val baselineY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
                    drawContext.canvas.nativeCanvas.drawText(
                        glyph.char.toString(),
                        centerX,
                        baselineY,
                        paint
                    )
                    drawContext.canvas.nativeCanvas.restore()
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
