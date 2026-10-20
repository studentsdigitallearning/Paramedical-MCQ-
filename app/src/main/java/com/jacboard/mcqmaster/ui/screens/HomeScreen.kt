package com.jacboard.mcqmaster.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacboard.mcqmaster.model.BannerActionType
import com.jacboard.mcqmaster.model.CategoryType
import com.jacboard.mcqmaster.model.ClassData
import com.jacboard.mcqmaster.model.PromotionalBanner
import com.jacboard.mcqmaster.model.Student
import com.jacboard.mcqmaster.model.TestCategory
import com.jacboard.mcqmaster.ui.components.GlassCard
import com.jacboard.mcqmaster.ui.components.GoldGradientButton
import com.jacboard.mcqmaster.ui.components.ScoreProgressRing
import com.jacboard.mcqmaster.ui.theme.AccentGold
import com.jacboard.mcqmaster.ui.theme.DailyChallengeDarkGradient
import com.jacboard.mcqmaster.ui.theme.DailyChallengeGradient
import com.jacboard.mcqmaster.ui.theme.PrimaryPurple
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    student: Student,
    currentClass: ClassData? = null,
    dailyCountdown: String,
    unreadNotificationsCount: Int,
    categories: List<TestCategory> = emptyList(),
    promotionalBanners: List<PromotionalBanner> = emptyList(),
    isDarkTheme: Boolean = false,
    isDailyTestAvailable: Boolean = true,
    onClassChipClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onDailyChallengeStart: () -> Unit,
    onCategoryClick: (TestCategory) -> Unit,
    onViewAllCategories: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val banners = promotionalBanners.ifEmpty { PromotionalBanner.defaultBanners }
    val pagerState = rememberPagerState(pageCount = { banners.size })

    // Auto-sliding mechanism: automatically advances banner every 4.5 seconds if more than 1 banner exists
    LaunchedEffect(pagerState.pageCount) {
        if (pagerState.pageCount > 1) {
            while (true) {
                delay(4500L)
                if (!pagerState.isScrollInProgress) {
                    val count = pagerState.pageCount
                    if (count > 1) {
                        val nextPage = (pagerState.currentPage + 1) % count
                        pagerState.animateScrollToPage(nextPage, animationSpec = tween(600))
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
    ) {
        // 1. Header (matching Screenshot 5)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val studentInitials = student.name.trim().split("\\s+".toRegex())
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "ST" }

                    // Dynamic Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(3.dp, CircleShape, spotColor = PrimaryPurple.copy(alpha = 0.35f))
                            .clip(CircleShape)
                            .background(PrimaryPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = studentInitials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Hello, ${student.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Class dropdown pill chip (matching Screenshot 5)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDarkTheme) Color(0xFF28205C) else Color(0xFFEEEAFE)
                                )
                                .border(
                                    0.8.dp,
                                    if (isDarkTheme) Color(0xFF3B2F7E) else Color(0xFFDDD6FE),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onClassChipClick() }
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                .testTag("class_chip")
                        ) {
                            Text(
                                text = currentClass?.name ?: "Select Course",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) Color(0xFFDDD6FE) else PrimaryPurple
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch Class",
                                tint = if (isDarkTheme) Color(0xFFDDD6FE) else PrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Right Side: Notification Bell + Hamburger Menu Symbol ("humble symbol")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Notification Bell with Red Dot
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .shadow(1.dp, CircleShape, spotColor = Color(0x10000000))
                            .testTag("notification_bell")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Hamburger Menu Button (Sidebar trigger)
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .shadow(1.dp, CircleShape, spotColor = Color(0x10000000))
                            .testTag("menu_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 2. Promotional Slider in Hero Section (Slide 1 is Today's MCQ Challenge matching Screenshot 5)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val banner = banners[page]

                    if (banner.isDailyChallenge) {
                        // The exact visual design of the Today's MCQ Challenge Hero Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = PrimaryPurple.copy(alpha = 0.35f)
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isDarkTheme) DailyChallengeDarkGradient else DailyChallengeGradient)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = if (isDarkTheme) 0.15f else 0.22f),
                                    RoundedCornerShape(24.dp)
                                )
                                .clickable { onDailyChallengeStart() }
                                .padding(20.dp)
                                .testTag("hero_daily_challenge_card")
                        ) {
                            Column {
                                // Top tag row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left tag: TODAY'S CHALLENGE
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = AccentGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = banner.badgeText,
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    // Right tag: 20 Questions • 30 Minutes or Not Scheduled
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isDailyTestAvailable) banner.metaText else "Not Scheduled",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Title
                                Text(
                                    text = if (isDailyTestAvailable) banner.title else "${currentClass?.name ?: "Course"} Daily Challenge",
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (isDailyTestAvailable) banner.description else "No daily test scheduled for ${currentClass?.name ?: "this course"} today.",
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Bottom Row: Resets in + Start Test button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = AccentGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Resets in $dailyCountdown",
                                            color = AccentGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    GoldGradientButton(
                                        text = if (isDailyTestAvailable) (banner.buttonText ?: "Start Test") else "Check Status",
                                        onClick = onDailyChallengeStart,
                                        leadingIcon = if (isDailyTestAvailable) Icons.Default.PlayArrow else Icons.Default.Quiz,
                                        testTag = "start_daily_test_button"
                                    )
                                }
                            }
                        }
                    } else {
                        // Matching Promotional Banners (Requirement 4)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = PrimaryPurple.copy(alpha = 0.35f)
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isDarkTheme) DailyChallengeDarkGradient else DailyChallengeGradient)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = if (isDarkTheme) 0.15f else 0.22f),
                                    RoundedCornerShape(24.dp)
                                )
                                .clickable {
                                    handleBannerClick(context, banner, onDailyChallengeStart, onViewAllCategories, categories, onCategoryClick)
                                }
                                .padding(20.dp)
                                .testTag("promo_banner_${banner.id}")
                        ) {
                            Column {
                                // Top tag row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (banner.actionType == BannerActionType.OPEN_VIDEO) Icons.Default.PlayCircle else Icons.Default.Star,
                                            contentDescription = null,
                                            tint = AccentGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = banner.badgeText,
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = banner.metaText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = banner.title,
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = banner.description,
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (banner.buttonText != null) {
                                        GoldGradientButton(
                                            text = banner.buttonText,
                                            onClick = {
                                                handleBannerClick(context, banner, onDailyChallengeStart, onViewAllCategories, categories, onCategoryClick)
                                            },
                                            leadingIcon = if (banner.actionType == BannerActionType.OPEN_VIDEO) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.OpenInNew,
                                            testTag = "promo_button_${banner.id}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Small Pagination Dots / Indicator (Requirement 4)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(banners.size) { index ->
                        val isCurrent = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(5.dp)
                                .width(if (isCurrent) 18.dp else 5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isCurrent) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }
        }

        // 3. Explore Tests Section (matching Screenshot 5 with Custom Tests replaced by Daily Test)
        item {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Explore Tests",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onViewAllCategories() }
                            .testTag("view_all_categories")
                    ) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val catSubject = categories.firstOrNull { it.type == CategoryType.SUBJECT || it.id == "cat_subject" }
                    ?: TestCategory("cat_subject", "Subject Wise", CategoryType.SUBJECT, "subject", "Explore chapter-wise tests", 0)
                val catMock = categories.firstOrNull { it.type == CategoryType.MOCK || it.id == "cat_mock" }
                    ?: TestCategory("cat_mock", "Mock Tests", CategoryType.MOCK, "mock", "Timed full syllabus simulation", 0)
                val catPrevious = categories.firstOrNull { it.type == CategoryType.PREVIOUS || it.id == "cat_previous" }
                    ?: TestCategory("cat_previous", "Previous Tests", CategoryType.PREVIOUS, "previous", "Past board question papers", 0)
                val catDaily = categories.firstOrNull { it.type == CategoryType.DAILY || it.id == "cat_daily" }
                    ?: TestCategory("cat_daily", "Daily Test", CategoryType.DAILY, "daily", "Today's daily challenge test", 0)

                // Row 1: Subject Wise & Mock Tests
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ExploreCategoryCard(
                        title = "Subject Wise",
                        subtitle = "Chapter-wise tests",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        iconBgLight = Color(0xFFEEEAFE),
                        iconTint = PrimaryPurple,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(catSubject) }
                    )

                    ExploreCategoryCard(
                        title = "Mock Tests",
                        subtitle = "Full syllabus",
                        icon = Icons.Default.Timer,
                        iconBgLight = Color(0xFFEBF3FF),
                        iconTint = Color(0xFF2563EB),
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(catMock) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Row 2: Previous Tests & Daily Test
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ExploreCategoryCard(
                        title = "Previous Tests",
                        subtitle = "Past board papers",
                        icon = Icons.Default.HistoryEdu,
                        iconBgLight = Color(0xFFFFF0E5),
                        iconTint = Color(0xFFF97316),
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(catPrevious) }
                    )

                    ExploreCategoryCard(
                        title = "Daily Test",
                        subtitle = "Today's challenge",
                        icon = Icons.Default.EmojiEvents,
                        iconBgLight = Color(0xFFF5EEFF),
                        iconTint = Color(0xFF8B5CF6),
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(catDaily) }
                    )
                }
            }
        }
    }
}

private fun handleBannerClick(
    context: android.content.Context,
    banner: PromotionalBanner,
    onDailyChallengeStart: () -> Unit,
    onViewAllCategories: () -> Unit,
    categories: List<TestCategory> = emptyList(),
    onCategoryClick: ((TestCategory) -> Unit)? = null
) {
    when (banner.actionType) {
        BannerActionType.OPEN_DAILY_TEST -> onDailyChallengeStart()
        BannerActionType.OPEN_INTERNAL_PAGE, BannerActionType.INTERNAL_ROUTE -> {
            when (banner.targetId) {
                "categories" -> onViewAllCategories()
                "mock_tests", "mock" -> {
                    val mockCat = categories.firstOrNull { it.type == CategoryType.MOCK || it.id == "cat_mock" }
                    if (mockCat != null && onCategoryClick != null) {
                        onCategoryClick(mockCat)
                    } else {
                        onViewAllCategories()
                    }
                }
                "subject_wise", "subjects", "subject" -> {
                    val subCat = categories.firstOrNull { it.type == CategoryType.SUBJECT || it.id == "cat_subject" }
                    if (subCat != null && onCategoryClick != null) {
                        onCategoryClick(subCat)
                    } else {
                        onViewAllCategories()
                    }
                }
                else -> onDailyChallengeStart()
            }
        }
        BannerActionType.OPEN_WEBSITE, BannerActionType.OPEN_VIDEO, BannerActionType.EXTERNAL_URL -> {
            val url = banner.url
            if (!url.isNullOrBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
        BannerActionType.NONE, BannerActionType.OPEN_TEST -> {
            val url = banner.url
            if (!url.isNullOrBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            } else if (banner.targetId == "daily_test") {
                onDailyChallengeStart()
            } else {
                onViewAllCategories()
            }
        }
    }
}

@Composable
fun ExploreCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgLight: Color,
    iconTint: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val resolvedBg = if (isDarkTheme) iconTint.copy(alpha = 0.20f) else iconBgLight

    GlassCard(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("explore_card_$title"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Icon in rounded square + right chevron (Screenshot 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(resolvedBg)
                        .border(
                            0.8.dp,
                            if (isDarkTheme) iconTint.copy(alpha = 0.35f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProgressStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBgLight: Color,
    iconTint: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val resolvedBg = if (isDarkTheme) iconTint.copy(alpha = 0.20f) else iconBgLight

    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(resolvedBg)
                    .border(
                        0.8.dp,
                        if (isDarkTheme) iconTint.copy(alpha = 0.35f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
