package com.jacboard.mcqmaster.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jacboard.mcqmaster.model.Attempt
import com.jacboard.mcqmaster.model.Question
import com.jacboard.mcqmaster.model.Test
import com.jacboard.mcqmaster.model.TestMode
import com.jacboard.mcqmaster.data.AppRepository
import com.jacboard.mcqmaster.ui.components.GlassCard
import com.jacboard.mcqmaster.ui.theme.AccentGold
import com.jacboard.mcqmaster.ui.theme.ErrorRed
import com.jacboard.mcqmaster.ui.theme.ErrorRedBg
import com.jacboard.mcqmaster.ui.theme.PrimaryIndigo
import com.jacboard.mcqmaster.ui.theme.PrimaryPurple
import com.jacboard.mcqmaster.ui.theme.SuccessGreen
import com.jacboard.mcqmaster.ui.theme.SuccessGreenBg
import com.jacboard.mcqmaster.ui.theme.WarningAmber
import com.jacboard.mcqmaster.ui.theme.WarningAmberBg
import com.jacboard.mcqmaster.util.QuizTtsManager
import kotlinx.coroutines.delay

@Composable
fun TestScreen(
    test: Test,
    attempt: Attempt,
    questions: List<Question>,
    onAnswerSelected: (Int, Int) -> Unit,
    onClearAnswer: (Int) -> Unit,
    onToggleMarkForReview: (Int) -> Unit,
    onQuestionIndexChanged: (Int) -> Unit,
    onSubmitTest: () -> Unit,
    onLeaveTest: () -> Unit
) {
    val context = LocalContext.current

    var currentIndex by remember(attempt.currentQuestionIndex) {
        mutableIntStateOf(attempt.currentQuestionIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0)))
    }

    // Language state: initialized from Voice Settings ("EN" or "HI")
    val savedVoiceLang by AppRepository.getInstance().voiceReadingLanguage.collectAsState()
    var currentLanguage by remember(savedVoiceLang) {
        mutableStateOf(if (savedVoiceLang.equals("HI", ignoreCase = true)) "HI" else "EN")
    }

    // TTS Manager
    val ttsManager = remember { QuizTtsManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    // Auto-stop speech when question index changes
    LaunchedEffect(currentIndex) {
        ttsManager.stop()
    }

    // Auto-stop speech when language toggle changes
    LaunchedEffect(currentLanguage) {
        ttsManager.stop()
    }

    // Remaining timer
    var remainingSeconds by remember {
        mutableLongStateOf(
            ((attempt.endsAtEpoch - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        )
    }

    var showPaletteDrawer by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showTimesUpDialog by remember { mutableStateOf(false) }

    // Intercept hardware/system back button to close drawer or show leave confirmation
    BackHandler {
        if (showPaletteDrawer) {
            showPaletteDrawer = false
        } else {
            showLeaveDialog = true
        }
    }

    // Ticking countdown loop
    LaunchedEffect(attempt.id) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
        if (remainingSeconds <= 0) {
            showTimesUpDialog = true
        }
    }

    if (questions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Questions Available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Questions for this test have not been uploaded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onLeaveTest,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val currentQuestion = questions.getOrNull(currentIndex) ?: questions.first()
    val isMarked = attempt.markedForReview.contains(currentIndex)
    val selectedOption = attempt.answers[currentIndex]

    // Determine current language text (cleanly separated)
    val hasHindi = currentQuestion.hasHindi()
    val isHindi = currentLanguage == "HI"

    val questionDisplayTitle = if (isHindi) currentQuestion.getHindiText() else currentQuestion.getEnglishText()
    val rawOptions = if (isHindi) currentQuestion.getHindiOptions() else currentQuestion.getEnglishOptions()

    // Stable randomized option order per question
    val optionOrder = remember(currentQuestion.id, attempt.optionOrders[currentIndex]) {
        attempt.optionOrders[currentIndex] ?: (0 until rawOptions.size).toList()
    }

    // Dynamic Test Mode: Exam Mode vs Practice Mode
    val isPracticeMode = attempt.mode == com.jacboard.mcqmaster.model.TestMode.PRACTICE

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            // 1. Top App Bar (clean single row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = { showLeaveDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .testTag("leave_test_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Leave Test",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = test.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Timer Pill
                val timerBg = when {
                    remainingSeconds < 60 -> ErrorRedBg
                    remainingSeconds < 300 -> WarningAmberBg
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
                val timerColor = when {
                    remainingSeconds < 60 -> ErrorRed
                    remainingSeconds < 300 -> WarningAmber
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(timerBg)
                        .border(0.8.dp, timerColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("test_timer_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = timerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = timeFormatted,
                            color = timerColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Utility Controls Row (Listen, EN/HI Pill, Bookmark, Questions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Listen / Voice TTS Controls (Play/Pause + Stop)
                val preferredGender by AppRepository.getInstance().voiceGender.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (ttsManager.isSpeaking) {
                                    ttsManager.pause()
                                } else if (ttsManager.isPaused) {
                                    ttsManager.resume()
                                } else {
                                    // Speak ONLY Question + 4 Options (A, B, C, D)
                                    val orderedOptions = optionOrder.map { origIdx ->
                                        rawOptions.getOrElse(origIdx) { "" }
                                    }
                                    ttsManager.speakQuestionAndOptions(
                                        questionText = questionDisplayTitle,
                                        options = orderedOptions,
                                        isHindi = isHindi,
                                        gender = preferredGender
                                    )
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("voice_listen_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (ttsManager.isSpeaking) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (ttsManager.isSpeaking) Icons.Default.Pause
                                              else if (ttsManager.isPaused) Icons.Default.PlayArrow
                                              else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read Question",
                                tint = if (ttsManager.isSpeaking) Color.White else PrimaryPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (ttsManager.isSpeaking) "Pause" else if (ttsManager.isPaused) "Resume" else "Read",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryPurple
                        )
                    }

                    // Stop Button (visible while reading or paused)
                    if (ttsManager.isSpeaking || ttsManager.isPaused) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { ttsManager.stop() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("voice_stop_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(ErrorRed.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Reading",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Stop",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ErrorRed
                            )
                        }
                    }
                }

                // Language Toggle Pill (Separate English / Hindi)
                if (hasHindi) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.8.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // EN Tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (currentLanguage == "EN") PrimaryPurple else Color.Transparent)
                                .clickable {
                                    currentLanguage = "EN"
                                    if (ttsManager.isSpeaking) ttsManager.stop()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("lang_en_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "English",
                                color = if (currentLanguage == "EN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // HI Tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (currentLanguage == "HI") PrimaryPurple else Color.Transparent)
                                .clickable {
                                    currentLanguage = "HI"
                                    if (ttsManager.isSpeaking) ttsManager.stop()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("lang_hi_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "हिंदी",
                                color = if (currentLanguage == "HI") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Questions / Palette Drawer Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showPaletteDrawer = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("open_palette_drawer_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Questions Palette",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Questions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Question Progress Tracker
            val progressFraction = (currentIndex + 1).toFloat() / questions.size
            val progressPercent = ((currentIndex + 1) * 100) / questions.size

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of ${questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "$progressPercent%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = PrimaryPurple,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Question Content & Options (Scrollable area)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // Question Card (Clean, Single Language + Optional Question Image)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    val cleanTitle = remember(questionDisplayTitle) {
                        questionDisplayTitle.replace(Regex("\\[IMG:[^\\]]+\\]", RegexOption.IGNORE_CASE), "").trim()
                    }
                    if (cleanTitle.isNotBlank()) {
                        Text(
                            text = cleanTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    }
                    if (!currentQuestion.imageUrl.isNullOrBlank()) {
                        if (cleanTitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentQuestion.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = cleanTitle.ifBlank { "Question Image" },
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .testTag("question_image_${currentQuestion.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Options List (A, B, C, D) with randomized order mapping
                val optionLetters = listOf("A", "B", "C", "D")

                optionOrder.forEachIndexed { displayIndex, originalOptionIndex ->
                    val optionText = rawOptions.getOrElse(originalOptionIndex) { "" }
                    val isSelected = selectedOption == originalOptionIndex
                    val letter = optionLetters.getOrElse(displayIndex) { "${displayIndex + 1}" }

                    // In Practice Mode: ONLY evaluate after student has selected an option
                    val isCorrectOptionInPractice = isPracticeMode && selectedOption != null && originalOptionIndex == currentQuestion.correctIndex
                    val isWrongSelectionInPractice = isPracticeMode && selectedOption != null && isSelected && originalOptionIndex != currentQuestion.correctIndex

                    QuizOptionCard(
                        letter = letter,
                        text = optionText,
                        isSelected = isSelected,
                        isPracticeMode = isPracticeMode,
                        isCorrectInPractice = isCorrectOptionInPractice,
                        isWrongInPractice = isWrongSelectionInPractice,
                        enabled = !isPracticeMode || selectedOption == null,
                        onClick = {
                            if (!isPracticeMode || selectedOption == null) {
                                onAnswerSelected(currentIndex, originalOptionIndex)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Practice Mode Instant Explanation Box (only after an answer is selected)
                if (isPracticeMode && selectedOption != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selectedOption == currentQuestion.correctIndex) SuccessGreenBg else ErrorRedBg
                            )
                            .border(
                                1.dp,
                                if (selectedOption == currentQuestion.correctIndex) SuccessGreen.copy(alpha = 0.3f) else ErrorRed.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (selectedOption == currentQuestion.correctIndex) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (selectedOption == currentQuestion.correctIndex) SuccessGreen else ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedOption == currentQuestion.correctIndex) "Correct Answer! 🎉" else "Incorrect Answer",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedOption == currentQuestion.correctIndex) SuccessGreen else ErrorRed,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Clear Answer link (in Exam mode)
                if (selectedOption != null && !isPracticeMode) {
                    TextButton(
                        onClick = { onClearAnswer(currentIndex) },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = "Clear answer",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // 5. Fixed Full-Width Bottom Action Bar (Previous / Next / Submit)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Previous Button
                    OutlinedButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                onQuestionIndexChanged(currentIndex)
                                if (ttsManager.isSpeaking) ttsManager.stop()
                            }
                        },
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("previous_question_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previous", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Next / Submit Button
                    if (currentIndex < questions.size - 1) {
                        Button(
                            onClick = {
                                currentIndex++
                                onQuestionIndexChanged(currentIndex)
                                if (ttsManager.isSpeaking) ttsManager.stop()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("next_question_button")
                        ) {
                            Text("Next", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { showSubmitDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("submit_test_button")
                        ) {
                            Text("Submit Test", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // 6. Side Drawer for Question Palette (sliding from right)
        if (showPaletteDrawer) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Dimmed Scrim backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { showPaletteDrawer = false }
                )

                // Side Drawer
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 320.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    SideQuestionPaletteDrawer(
                        questions = questions,
                        currentIndex = currentIndex,
                        answers = attempt.answers,
                        marked = attempt.markedForReview,
                        isPracticeMode = isPracticeMode,
                        onSelectQuestion = { targetIndex ->
                            currentIndex = targetIndex
                            onQuestionIndexChanged(targetIndex)
                            showPaletteDrawer = false
                            if (ttsManager.isSpeaking) ttsManager.stop()
                        },
                        onClose = { showPaletteDrawer = false }
                    )
                }
            }
        }
    }

    // Submit Confirmation Dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = {
                Text(
                    text = "Submit Test?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF171733)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Are you sure you want to submit? You cannot change answers after submission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF707694)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TestStatPill("Answered", "${attempt.answers.size}", SuccessGreen)
                        TestStatPill("Unanswered", "${questions.size - attempt.answers.size}", Color(0xFF707694))
                        TestStatPill("Marked", "${attempt.markedForReview.size}", AccentGold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        ttsManager.stop()
                        onSubmitTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Submit Test", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Continue Test")
                }
            }
        )
    }

    // Leave Test Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = {
                Text("Leave Test?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to exit the test?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveDialog = false
                        ttsManager.stop()
                        onLeaveTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTimesUpDialog) {
        AlertDialog(
            onDismissRequest = {
                showTimesUpDialog = false
                ttsManager.stop()
                onSubmitTest()
            },
            title = {
                Text("Time's Up! ⏱", fontWeight = FontWeight.Bold, color = ErrorRed)
            },
            text = {
                Text("Your test duration has completed. Your test has been submitted automatically.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTimesUpDialog = false
                        ttsManager.stop()
                        onSubmitTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("View Results")
                }
            }
        )
    }
}


