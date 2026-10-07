package com.example.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MonthlyStreakDayStatus
import com.example.model.MonthlyStreakState
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SuccessGreen

@Composable
fun MonthlyStreakRewardsScreen(
    monthlyState: MonthlyStreakState,
    isDarkTheme: Boolean = false,
    onBackClick: () -> Unit,
    onUnlockReward: ((String) -> Boolean)? = null,
    onStartDailyTest: () -> Unit
) {
    var selectedDayDetail by remember { mutableStateOf<MonthlyStreakDayStatus?>(null) }
    var showStreakRulesInfo by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .testTag("streak_back_button")
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Profile",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Practice Streak",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${monthlyState.monthDisplayName} Challenge",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = { showStreakRulesInfo = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Streak Rules",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Hero Streak Summary Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = if (isDarkTheme) {
                                    listOf(Color(0xFF2E190E), Color(0xFF1E1B2E), Color(0xFF16192E))
                                } else {
                                    listOf(Color(0xFFFFF3E0), Color(0xFFF3E8FF), Color(0xFFEDE9FE))
                                }
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Streak Display
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(AccentOrange.copy(alpha = 0.15f))
                                    .border(1.5.dp, AccentOrange.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Active Streak Flame",
                                    tint = AccentOrange,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Current Streak",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "🔥 ${monthlyState.currentStreakDays} Days",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFE65100)
                                )
                            }
                        }

                        // Days Active Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(PrimaryPurple.copy(alpha = 0.12f))
                                .border(1.dp, PrimaryPurple.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${monthlyState.totalDaysCompleted}/${monthlyState.totalDaysInMonth}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PrimaryPurple
                                )
                                Text(
                                    text = "Days Active",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Next Streak Milestone Progress Tracker
                    val streakInCurrent7Cycle = monthlyState.currentStreakDays % 7
                    val progressFraction = streakInCurrent7Cycle / 7f

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "7-Day Milestone Goal",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = if (streakInCurrent7Cycle == 0 && monthlyState.currentStreakDays > 0) {
                                    "Milestone Achieved! 🎉"
                                } else {
                                    "${monthlyState.nextMilestoneDaysRemaining} days remaining"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (streakInCurrent7Cycle == 0 && monthlyState.currentStreakDays > 0) SuccessGreen else PrimaryPurple
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { if (streakInCurrent7Cycle == 0 && monthlyState.currentStreakDays > 0) 1f else progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentOrange,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Practice continuous days on device to keep your flame active!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Action CTA Banner: Practice Today
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(PrimaryPurple, PrimaryIndigo)
                        )
                    )
                    .clickable { onStartDailyTest() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Take Today's Daily Test",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Extend your streak and practice MCQs",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Start Test",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Monthly Streak Calendar Section
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${monthlyState.monthDisplayName} Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Tap a day for details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day of Week Header
                    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        weekDays.forEach { dayName ->
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 7-Column Calendar Grid
                    val totalDays = monthlyState.totalDaysInMonth
                    val offset = monthlyState.startDayOfWeekOffset
                    val totalSlots = offset + totalDays
                    val rowCount = (totalSlots + 6) / 7

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (row in 0 until rowCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0..6) {
                                    val slotIndex = row * 7 + col
                                    val dayNumber = slotIndex - offset + 1

                                    if (dayNumber in 1..totalDays) {
                                        val dayStatus = monthlyState.days.find { it.dayOfMonth == dayNumber }
                                            ?: MonthlyStreakDayStatus(dayNumber, false, false, false)

                                        CalendarDayCell(
                                            status = dayStatus,
                                            isDarkTheme = isDarkTheme,
                                            onClick = { selectedDayDetail = dayStatus }
                                        )
                                    } else {
                                        // Empty slot filler
                                        Spacer(modifier = Modifier.width(36.dp).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Calendar Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(
                            color = AccentOrange,
                            label = "Completed",
                            icon = Icons.Default.LocalFireDepartment
                        )
                        LegendItem(
                            color = PrimaryPurple,
                            label = "Today",
                            icon = Icons.Default.Today
                        )
                        LegendItem(
                            color = AccentGold,
                            label = "7-Day Milestone",
                            icon = Icons.Default.Star
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Streak Rules & Tips Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "How Practice Streak Works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StreakRuleRow(
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = AccentOrange,
                        title = "Practice Every Day",
                        desc = "Take the Daily MCQ Challenge or any practice quiz to mark the day as active."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    StreakRuleRow(
                        icon = Icons.Default.Star,
                        iconTint = AccentGold,
                        title = "7-Day Continuous Milestones",
                        desc = "Reach 7, 14, 21, and 28 days of continuous practice to unlock milestone badges."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    StreakRuleRow(
                        icon = Icons.Default.DateRange,
                        iconTint = PrimaryPurple,
                        title = "Build Consistent Habits",
                        desc = "Practice every day to maintain your streak, build strong study habits, and achieve top scores."
                    )
                }
            }
        }
    }

    // Day Detail Dialog
    selectedDayDetail?.let { day ->
        AlertDialog(
            onDismissRequest = { selectedDayDetail = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (day.isCompleted) AccentOrange.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (day.isCompleted) Icons.Default.LocalFireDepartment else Icons.Default.Today,
                        contentDescription = null,
                        tint = if (day.isCompleted) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "${monthlyState.monthDisplayName.split(" ").firstOrNull() ?: ""} ${day.dayOfMonth}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            day.isCompleted && day.is7DayMilestone -> "🔥 7-Day Continuous Milestone Completed!"
                            day.isCompleted -> "✅ Daily Practice Completed"
                            day.isToday -> "⏳ Today's Daily Practice Pending"
                            day.isFuture -> "Upcoming calendar date"
                            else -> "❌ Missed practice on this day"
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            day.isCompleted -> AccentOrange
                            day.isToday -> PrimaryPurple
                            day.isFuture -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.error
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when {
                            day.isCompleted -> "Great job! You maintained your active practice streak on this day."
                            day.isToday -> "Complete today's Daily Test or practice quiz to extend your daily streak!"
                            day.isFuture -> "Practice on this day to keep your monthly consistency high."
                            else -> "No tests were taken on this date. Keep practicing to build a longer continuous streak!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                if (day.isToday && !day.isCompleted) {
                    Button(
                        onClick = {
                            selectedDayDetail = null
                            onStartDailyTest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Start Today's Test")
                    }
                } else {
                    TextButton(onClick = { selectedDayDetail = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Streak Rules Dialog
    if (showStreakRulesInfo) {
        AlertDialog(
            onDismissRequest = { showStreakRulesInfo = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Daily Practice Streak",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "• Practice Consistency: Take at least 1 test each day to maintain your streak.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• 7-Day Milestones: Earn milestone highlights for every 7 continuous days (Day 7, 14, 21, 28).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Track Progress: Maintain daily practice to build your knowledge and improve leaderboard ranks.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStreakRulesInfo = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun CalendarDayCell(
    status: MonthlyStreakDayStatus,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val isDone = status.isCompleted
    val isToday = status.isToday
    val isFuture = status.isFuture
    val isMilestone = status.is7DayMilestone

    val bgColor = when {
        isDone && isMilestone -> AccentOrange.copy(alpha = 0.25f)
        isDone -> AccentOrange.copy(alpha = 0.18f)
        isToday -> PrimaryPurple.copy(alpha = 0.15f)
        isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = when {
        isToday -> PrimaryPurple
        isDone && isMilestone -> AccentGold
        isDone -> AccentOrange.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val textColor = when {
        isDone -> if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFD97706)
        isToday -> PrimaryPurple
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (isToday || (isDone && isMilestone)) 1.5.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isDone) {
            Icon(
                imageVector = if (isMilestone) Icons.Default.Star else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isMilestone) AccentGold else AccentOrange,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = "${status.dayOfMonth}",
                fontSize = 13.sp,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StreakRuleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
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
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
