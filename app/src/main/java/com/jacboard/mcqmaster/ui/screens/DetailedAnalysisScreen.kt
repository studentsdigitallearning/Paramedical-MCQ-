package com.jacboard.mcqmaster.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacboard.mcqmaster.model.QuestionReview
import com.jacboard.mcqmaster.model.TestResult
import com.jacboard.mcqmaster.ui.components.AppHeaderBar
import com.jacboard.mcqmaster.ui.components.GlassCard
import com.jacboard.mcqmaster.ui.theme.AccentGold
import com.jacboard.mcqmaster.ui.theme.ErrorRed
import com.jacboard.mcqmaster.ui.theme.ErrorRedBg
import com.jacboard.mcqmaster.ui.theme.PrimaryPurple
import com.jacboard.mcqmaster.ui.theme.SuccessGreen
import com.jacboard.mcqmaster.ui.theme.SuccessGreenBg

@Composable
fun DetailedAnalysisScreen(
    result: TestResult,
    onBackClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedLanguage by remember { mutableStateOf("HI") }

    val filteredQuestions = remember(result.questionsReview, selectedFilter) {
        when (selectedFilter) {
            "Correct" -> result.questionsReview.filter { it.isCorrect }
            "Wrong" -> result.questionsReview.filter { it.userAnswerIndex != null && !it.isCorrect }
            "Not Attempted" -> result.questionsReview.filter { it.userAnswerIndex == null }
            else -> result.questionsReview
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        AppHeaderBar(
            title = "Detailed Analysis",
            subtitle = "${result.score.toInt()}/${result.totalMarks.toInt()} Marks • ${result.percentage}%",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Chapter Performance Insight Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen.copy(alpha = 0.14f))
                                    .border(0.8.dp, SuccessGreen.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ThumbUp, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Strongest Chapter", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(result.strongestChapter, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                            }
                        }
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ErrorRed.copy(alpha = 0.14f))
                                    .border(0.8.dp, ErrorRed.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ThumbDown, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Needs Revision", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(result.weakestChapter, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Language Toggle + Filter Tabs Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterOptions = listOf(
                            "All" to result.questionsReview.size,
                            "Correct" to result.correctCount,
                            "Wrong" to result.wrongCount,
                            "Not Attempted" to result.notAttemptedCount
                        )

                        filterOptions.forEach { (label, count) ->
                            val isSelected = selectedFilter == label
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedFilter = label }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("analysis_filter_$label")
                            ) {
                                Text(
                                    text = "$label ($count)",
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Language Selector (EN | हिंदी)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.8.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selectedLanguage == "EN") PrimaryPurple else Color.Transparent)
                                .clickable { selectedLanguage = "EN" }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "EN",
                                color = if (selectedLanguage == "EN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selectedLanguage == "HI") PrimaryPurple else Color.Transparent)
                                .clickable { selectedLanguage = "HI" }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "हिंदी",
                                color = if (selectedLanguage == "HI") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Question Items
            itemsIndexed(filteredQuestions) { _, item ->
                QuestionAnalysisCard(
                    review = item,
                    isHindi = selectedLanguage == "HI"
                )
            }
        }
    }
}

@Composable
fun QuestionAnalysisCard(
    review: QuestionReview,
    isHindi: Boolean = false
) {
    var expandedExplanation by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Status and Chapter Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (badgeBg, badgeColor, badgeIcon, statusText) = when {
                        review.isCorrect -> Quadruple(SuccessGreenBg, SuccessGreen, Icons.Default.Check, "Correct")
                        review.userAnswerIndex != null -> Quadruple(ErrorRedBg, ErrorRed, Icons.Default.Close, "Incorrect")
                        else -> Quadruple(Color(0xFFF3F4F6), Color.Gray, Icons.Default.Remove, "Unanswered")
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = statusText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = review.question.chapterName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${review.question.questionIndex + 1}. ${review.question.getDisplayText(isHindi)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Options List with verification state
            val optionLetters = listOf("A", "B", "C", "D")
            val optionsList = review.question.getDisplayOptions(isHindi)
            val displayOrder = if (review.optionOrder.isNotEmpty()) review.optionOrder else (0 until optionsList.size).toList()

            displayOrder.forEachIndexed { displayIdx, originalOptIdx ->
                val optText = optionsList.getOrElse(originalOptIdx) { "" }
                val isCorrectOption = originalOptIdx == review.question.correctIndex
                val isUserPicked = review.userAnswerIndex == originalOptIdx

                val (cardBg, cardBorder, badgeColor) = when {
                    isCorrectOption -> Triple(SuccessGreenBg, SuccessGreen, SuccessGreen)
                    isUserPicked && !review.isCorrect -> Triple(ErrorRedBg, ErrorRed, ErrorRed)
                    else -> Triple(Color.Transparent, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), Color.Gray)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${optionLetters.getOrElse(displayIdx) { "" }}. ",
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                fontSize = 14.sp
                            )
                            Text(
                                text = optText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isCorrectOption) {
                            Icon(Icons.Default.Check, contentDescription = "Correct Answer", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        } else if (isUserPicked) {
                            Icon(Icons.Default.Close, contentDescription = "Your Pick", tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
