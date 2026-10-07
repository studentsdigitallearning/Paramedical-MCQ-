package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyLeaderboardEntry
import com.example.model.DailyLeaderboardState
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SuccessGreen
import com.example.util.DailyTestTimeUtils

@Composable
fun LeaderboardScreen(
    currentClassText: String,
    dailyState: DailyLeaderboardState,
    dailyCountdown: String,
    isLoading: Boolean = false,
    onStartOrRetakeDailyTest: () -> Unit,
    onShareDailyResult: () -> Unit,
    onGenerateCertificate: () -> Unit
) {
    val bestResult = dailyState.currentUserBest
    val hasAttempted = bestResult != null && dailyState.currentUserRank != null
    val userRank = dailyState.currentUserRank
    val isUserOutsideTop100 = hasAttempted && userRank != null && userRank > 100

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        if (isLoading && dailyState.top100.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Daily Leaderboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "$currentClassText • Today's Challenge",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                com.example.ui.components.LeaderboardSkeleton(
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Class Context & Reset Timer Banner (Rule 1 & 9)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Leaderboard",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "$currentClassText • ${dailyState.displayDate}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Midnight Reset Timer Capsule (Rule 10)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(0.8.dp, AccentGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Resets in $dailyCountdown",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // USER STATUS CARD (Rule 6: If unattempted, show "Take today's Daily Test to get your rank.")
                if (hasAttempted) {
                    // Performance Overview Summary Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.2.dp,
                                    color = if (isUserOutsideTop100) AccentOrange.copy(alpha = 0.5f) else PrimaryPurple.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .testTag("leaderboard_user_performance_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                if (isUserOutsideTop100) AccentOrange.copy(alpha = 0.15f)
                                                else PrimaryPurple.copy(alpha = 0.15f)
                                            ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isUserOutsideTop100) Icons.Default.WarningAmber else Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = if (isUserOutsideTop100) AccentOrange else PrimaryPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Your Daily Performance",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isUserOutsideTop100) AccentOrange.copy(alpha = 0.15f)
                                                else SuccessGreen.copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isUserOutsideTop100) "Rank #$userRank (Outside Top 100)" else "🎉 Rank #$userRank (In Top 100)",
                                            color = if (isUserOutsideTop100) AccentOrange else SuccessGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 4 Key Metrics (Rule 8: Best Result used)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PerformanceMetricItem(
                                        label = "Your Rank",
                                        value = "#$userRank",
                                        color = if (isUserOutsideTop100) AccentOrange else PrimaryPurple,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PerformanceMetricItem(
                                        label = "Best Score",
                                        value = "${bestResult?.bestScore?.toInt() ?: 0}/${bestResult?.totalMarks?.toInt() ?: 50}",
                                        color = SuccessGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PerformanceMetricItem(
                                        label = "Time",
                                        value = DailyTestTimeUtils.formatTimeTaken(bestResult?.bestCompletionTimeSeconds ?: 0L),
                                        color = InfoBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PerformanceMetricItem(
                                        label = "Attempts",
                                        value = "${bestResult?.attemptsCount ?: 1}",
                                        color = AccentGold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Ahead & Behind Statistics
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Text(
                                        text = "Ahead of You: ${dailyState.studentsAhead}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Behind You: %,d".format(dailyState.studentsBehind),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Actions: Unlimited re-attempts in same Course+Class (Rule 8) + Share + Cert
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onStartOrRetakeDailyTest,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("retake_daily_test_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isUserOutsideTop100) AccentOrange else PrimaryPurple
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isUserOutsideTop100) "Try Again to Enter Top 100 🎯" else "Re-attempt to Improve Rank 🔥",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onShareDailyResult,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .testTag("share_daily_result_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Share Result",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = PrimaryPurple
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = onGenerateCertificate,
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .height(42.dp)
                                                .testTag("generate_certificate_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CardMembership,
                                                contentDescription = null,
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Get Certificate",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Rule 6: Unattempted state message (No fake rank, pure info banner)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unattempted_daily_test_notice"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Take today's Daily Test to get your rank.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // MAIN LEADERBOARD = TOP 100 ONLY (Rule 2, 3, 7)
                if (dailyState.top100.isNotEmpty()) {
                    // Top 3 Podium (ranks 1 to 3)
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Top 3 Rankers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val top3 = dailyState.top100.take(3)
                        val first = top3.getOrNull(0)
                        val second = top3.getOrNull(1)
                        val third = top3.getOrNull(2)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // #2 Rank (Left)
                            if (second != null) {
                                DailyPodiumCard(
                                    rank = 2,
                                    entry = second,
                                    badgeBg = Color(0xFF7E8EA6),
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                EmptyPodiumSlot(rank = 2, modifier = Modifier.weight(1f))
                            }

                            // #1 Rank (Center - Highlighted)
                            if (first != null) {
                                DailyPodiumCard(
                                    rank = 1,
                                    entry = first,
                                    badgeBg = AccentGold,
                                    isCenterWinner = true,
                                    modifier = Modifier.weight(1.1f)
                                )
                            } else {
                                EmptyPodiumSlot(rank = 1, isCenter = true, modifier = Modifier.weight(1.1f))
                            }

                            // #3 Rank (Right)
                            if (third != null) {
                                DailyPodiumCard(
                                    rank = 3,
                                    entry = third,
                                    badgeBg = Color(0xFFD97706),
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                EmptyPodiumSlot(rank = 3, modifier = Modifier.weight(1f))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Top 100 Header (Rule 2 & 7: max 100, if 35 show 35 without fake entries)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOP 100 LEADERBOARD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${dailyState.top100.size} students",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Main Leaderboard Items (Rank 1 to 100) (Rule 2 & 5)
                    // Lists all ranked students starting from Rank 1, 2, 3... up to 100.
                    items(dailyState.top100) { entry ->
                        DailyLeaderboardRowItem(entry = entry)
                    }

                    // SEPARATE "YOUR RANK / MY POSITION" SECTION (Rule 4, 5, 12)
                    // Rendered ONLY when current user is outside Top 100 (rank > 100).
                    // If user rank <= 100, this separate section is NOT displayed (Rule 5: No duplicate).
                    if (isUserOutsideTop100 && bestResult != null) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "YOUR RANK",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentOrange
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, AccentOrange, RoundedCornerShape(18.dp))
                                    .testTag("user_outside_top100_separate_row"),
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = AccentOrange.copy(alpha = 0.12f),
                                borderColor = AccentOrange
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#$userRank",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AccentOrange,
                                                modifier = Modifier.width(44.dp)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(AccentOrange),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = bestResult.studentName.take(2).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${bestResult.studentName} (You)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${bestResult.studentClass} • Rank #$userRank of %,d".format(dailyState.totalParticipants),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${bestResult.bestScore.toInt()}/${bestResult.totalMarks.toInt()}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentOrange
                                            )
                                            Text(
                                                text = DailyTestTimeUtils.formatTimeTaken(bestResult.bestCompletionTimeSeconds),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty Leaderboard State (Rule 7)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryPurple.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No rankers yet today",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Rankings for $currentClassText will appear as students submit today's Daily Test.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceMetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DailyPodiumCard(
    rank: Int,
    entry: DailyLeaderboardEntry,
    badgeBg: Color,
    isCenterWinner: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isUser = entry.isCurrentUser

    GlassCard(
        modifier = modifier
            .border(
                width = if (isCenterWinner || isUser) 2.dp else 1.dp,
                color = when {
                    isUser -> PrimaryPurple
                    isCenterWinner -> AccentGold
                    else -> MaterialTheme.colorScheme.outline
                },
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isCenterWinner) 16.dp else 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(if (isCenterWinner) 30.dp else 26.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (rank) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> "#$rank"
                    },
                    fontSize = if (isCenterWinner) 15.sp else 13.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Avatar circle
            Box(
                modifier = Modifier
                    .size(if (isCenterWinner) 50.dp else 44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUser) PrimaryPurple
                        else if (isCenterWinner) AccentGold.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        1.5.dp,
                        if (isUser) PrimaryPurple else if (isCenterWinner) AccentGold else MaterialTheme.colorScheme.outline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.avatarInitials.ifEmpty { entry.studentName.take(2).uppercase() },
                    color = if (isUser) Color.White else if (isCenterWinner) AccentGold else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCenterWinner) 16.sp else 14.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isUser) "${entry.studentName} (You)" else entry.studentName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isUser) PrimaryPurple else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = if (isCenterWinner) 13.sp else 12.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${entry.score.toInt()}/${entry.totalMarks.toInt()}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCenterWinner) AccentGold else SuccessGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = DailyTestTimeUtils.formatTimeTaken(entry.completionTimeSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyPodiumSlot(
    rank: Int,
    isCenter: Boolean = false,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isCenter) 16.dp else 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "#$rank"
                },
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "-",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Vacant",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DailyLeaderboardRowItem(entry: DailyLeaderboardEntry) {
    val isUser = entry.isCurrentUser

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_leaderboard_row_${entry.rank}"),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        borderColor = if (isUser) PrimaryPurple else MaterialTheme.colorScheme.outline
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    when (entry.rank) {
                        1 -> Text("🥇", fontSize = 18.sp)
                        2 -> Text("🥈", fontSize = 18.sp)
                        3 -> Text("🥉", fontSize = 18.sp)
                        else -> Text(
                            text = "#${entry.rank}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Avatar initials circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isUser -> PrimaryPurple
                                entry.rank == 1 -> AccentGold.copy(alpha = 0.25f)
                                entry.rank == 2 -> Color(0xFF7E8EA6).copy(alpha = 0.25f)
                                entry.rank == 3 -> Color(0xFFD97706).copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.avatarInitials.ifEmpty { entry.studentName.take(2).uppercase() },
                        color = when {
                            isUser -> Color.White
                            entry.rank == 1 -> AccentGold
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUser) "${entry.studentName} (You)" else entry.studentName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isUser) PrimaryPurple else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.className} • ${entry.attemptsCount} attempt${if (entry.attemptsCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.score.toInt()}/${entry.totalMarks.toInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) PrimaryPurple else SuccessGreen
                )
                Text(
                    text = DailyTestTimeUtils.formatTimeTaken(entry.completionTimeSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
