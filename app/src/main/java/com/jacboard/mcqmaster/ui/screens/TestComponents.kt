package com.jacboard.mcqmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.jacboard.mcqmaster.model.Question
import com.jacboard.mcqmaster.ui.theme.*

@Composable
fun QuizOptionCard(
    letter: String,
    text: String,
    isSelected: Boolean,
    isPracticeMode: Boolean = false,
    isCorrectInPractice: Boolean = false,
    isWrongInPractice: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor = when {
        isPracticeMode && isCorrectInPractice -> SuccessGreen
        isPracticeMode && isWrongInPractice -> ErrorRed
        isSelected -> PrimaryPurple
        else -> MaterialTheme.colorScheme.outline
    }

    val backgroundColor = when {
        isPracticeMode && isCorrectInPractice -> SuccessGreenBg
        isPracticeMode && isWrongInPractice -> ErrorRedBg
        isSelected -> Color(0xFFF3E8FF)
        else -> MaterialTheme.colorScheme.surface
    }

    val letterBg = when {
        isPracticeMode && isCorrectInPractice -> SuccessGreen
        isPracticeMode && isWrongInPractice -> ErrorRed
        isSelected -> PrimaryPurple
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val letterTextColor = when {
        isSelected || (isPracticeMode && (isCorrectInPractice || isWrongInPractice)) -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderWidth = when {
        isSelected || (isPracticeMode && (isCorrectInPractice || isWrongInPractice)) -> 2.dp
        else -> 1.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("option_$letter")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(letterBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    color = letterTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isSelected && !isPracticeMode) Color(0xFF1E1B4B) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
            }

            if (isPracticeMode && isCorrectInPractice) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correct",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else if (isPracticeMode && isWrongInPractice) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ErrorRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Incorrect",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SideQuestionPaletteDrawer(
    questions: List<Question>,
    currentIndex: Int,
    answers: Map<Int, Int>,
    marked: Set<Int>,
    isPracticeMode: Boolean,
    onSelectQuestion: (Int) -> Unit,
    onClose: () -> Unit
) {
    var selectedPaletteIndex by remember(currentIndex) { mutableIntStateOf(currentIndex) }
    val questionsCount = questions.size

    val correctCount = questions.indices.count { answers[it] == questions[it].correctIndex }
    val incorrectCount = questions.indices.count { answers.containsKey(it) && answers[it] != questions[it].correctIndex }
    val answeredCount = answers.size
    val notAnsweredCount = questionsCount - answeredCount
    val markedCount = marked.count { !answers.containsKey(it) }
    val answeredAndMarkedCount = marked.count { answers.containsKey(it) }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 280.dp, max = 360.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Question Palette",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF171733)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F3F7))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Palette",
                        tint = Color(0xFF171733),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isPracticeMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8F9FD))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaletteDotLegend(color = PrimaryPurple, label = "Current")
                        PaletteDotLegend(color = SuccessGreen, label = "Correct")
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaletteDotLegend(color = ErrorRed, label = "Incorrect")
                        PaletteDotLegend(color = Color(0xFFEDE8FF), borderColor = PrimaryPurple, label = "Unanswered", isBordered = true)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8F9FD))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaletteDotLegend(color = PrimaryPurple, label = "Current")
                        PaletteDotLegend(color = Color(0xFFDCFCE7), borderColor = Color(0xFF86EFAC), label = "Answered", isBordered = true)
                        PaletteDotLegend(color = Color(0xFFFEF3C7), borderColor = AccentGold, label = "Marked", isBordered = true)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaletteDotLegend(color = Color(0xFFF2F3F7), label = "Not Visited")
                        PaletteDotLegend(color = Color(0xFFDCFCE7), borderColor = Color(0xFFEF4444), label = "Ans & Marked", isBordered = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = Color(0xFFFAFAFC),
                shape = RoundedCornerShape(16.dp)
            ) {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(questionsCount) { index ->
                        val isCurrent = index == currentIndex
                        val isAnswered = answers.containsKey(index)
                        val isMarked = marked.contains(index)
                        val isSelected = index == selectedPaletteIndex

                        val isCorrectInPractice = isPracticeMode && isAnswered && answers[index] == questions[index].correctIndex
                        val isWrongInPractice = isPracticeMode && isAnswered && answers[index] != questions[index].correctIndex

                        QuestionPaletteTile(
                            number = index + 1,
                            isCurrent = isCurrent,
                            isAnswered = isAnswered,
                            isMarked = isMarked,
                            isSelected = isSelected,
                            isPracticeMode = isPracticeMode,
                            isCorrectInPractice = isCorrectInPractice,
                            isWrongInPractice = isWrongInPractice,
                            onClick = {
                                selectedPaletteIndex = index
                                onSelectQuestion(index)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF8F9FD),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Attempt Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF171733)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusSummaryRow(iconColor = Color(0xFF10B981), count = answeredCount, label = "Answered")
                        StatusSummaryRow(iconColor = Color(0xFF94A3B8), count = notAnsweredCount, label = "Not Answered")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusSummaryRow(iconColor = AccentGold, count = markedCount, label = "Marked")
                        StatusSummaryRow(iconColor = Color(0xFF8B5CF6), count = answeredAndMarkedCount, label = "Ans & Marked")
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionPaletteTile(
    number: Int,
    isCurrent: Boolean,
    isAnswered: Boolean,
    isMarked: Boolean,
    isSelected: Boolean,
    isPracticeMode: Boolean = false,
    isCorrectInPractice: Boolean = false,
    isWrongInPractice: Boolean = false,
    onClick: () -> Unit
) {
    val bg = when {
        isPracticeMode && isCorrectInPractice -> Color(0xFFDCFCE7)
        isPracticeMode && isWrongInPractice -> Color(0xFFFEE2E2)
        !isPracticeMode && isAnswered -> Color(0xFFDCFCE7)
        !isPracticeMode && isMarked -> Color(0xFFFEF3C7)
        !isPracticeMode && isCurrent -> Color(0xFFEDE9FE)
        isAnswered && isMarked -> Color(0xFFDCFCE7)
        isAnswered -> Color(0xFFDCFCE7)
        isMarked -> Color(0xFFFEF3C7)
        else -> Color(0xFFF2F3F7)
    }

    val textColor = when {
        isPracticeMode && isCorrectInPractice -> Color(0xFF15803D)
        isPracticeMode && isWrongInPractice -> Color(0xFFB91C1C)
        !isPracticeMode && isAnswered -> Color(0xFF15803D)
        !isPracticeMode && isMarked -> Color(0xFFB45309)
        !isPracticeMode && isCurrent -> PrimaryPurple
        isAnswered && isMarked -> Color(0xFF15803D)
        isAnswered -> Color(0xFF15803D)
        isMarked -> Color(0xFFB45309)
        else -> Color(0xFF171733)
    }

    val border = when {
        isCurrent -> 2.5.dp
        isSelected -> 1.5.dp
        isPracticeMode && isCorrectInPractice -> 1.dp
        isPracticeMode && isWrongInPractice -> 1.dp
        isAnswered -> 1.dp
        isMarked -> 1.dp
        else -> 0.dp
    }

    val borderColor = when {
        isCurrent -> PrimaryPurple
        isSelected -> PrimaryPurple
        isPracticeMode && isCorrectInPractice -> SuccessGreen
        isPracticeMode && isWrongInPractice -> ErrorRed
        isAnswered -> Color(0xFF86EFAC)
        isMarked -> AccentGold
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (border > 0.dp) Modifier.border(border, borderColor, RoundedCornerShape(12.dp)) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            fontSize = 14.sp,
            fontWeight = if (isCurrent || isAnswered || isMarked) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )

        if (isMarked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isAnswered) Color(0xFFEF4444) else AccentGold)
            )
        }
    }
}

@Composable
fun PaletteDotLegend(
    color: Color,
    label: String,
    borderColor: Color? = null,
    isBordered: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isBordered && borderColor != null) Modifier.border(1.dp, borderColor, CircleShape) else Modifier
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF707694),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatusSummaryRow(
    iconColor: Color,
    count: Int,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            if (iconColor == Color(0xFF10B981)) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "$count",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF171733)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF707694)
            )
        }
    }
}

@Composable
fun TestStatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF707694)
        )
    }
}
