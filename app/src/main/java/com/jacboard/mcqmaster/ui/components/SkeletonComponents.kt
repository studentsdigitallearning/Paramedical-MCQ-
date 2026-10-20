package com.jacboard.mcqmaster.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.jacboard.mcqmaster.ui.theme.PrimaryPurple

/**
 * Creates an animated shimmer effect for skeleton loading states.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val baseColor = if (isDark) Color(0xFF262244) else Color(0xFFE2E8F0)
    val highlightColor = if (isDark) Color(0xFF3B3560) else Color(0xFFF1F5F9)

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )

    this.background(brush)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmerEffect()
    )
}

/**
 * Skeleton loading list for Class / Course Selection Screen.
 */
@Composable
fun ClassSelectionSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(5) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(20.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    SkeletonBox(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape
                    )
                }
            }
        }
    }
}

/**
 * Skeleton loading grid for Subject-Wise selection screen.
 */
@Composable
fun SubjectListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(4) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(18.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Skeleton loading list for Chapters Selection screen.
 */
@Composable
fun ChapterListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(16.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Skeleton loading list for Test Cards (Mock Tests, Chapter Tests, etc.).
 */
@Composable
fun TestListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(4) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBox(
                            modifier = Modifier
                                .width(70.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        SkeletonBox(
                            modifier = Modifier
                                .width(50.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(20.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SkeletonBox(
                            modifier = Modifier
                                .width(60.dp)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        SkeletonBox(
                            modifier = Modifier
                                .width(60.dp)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        SkeletonBox(
                            modifier = Modifier
                                .width(60.dp)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Skeleton loading card for Daily Test Screen.
 */
@Composable
fun DailyTestSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SkeletonBox(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.height(18.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(24.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(16.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) {
                        SkeletonBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

/**
 * Skeleton loading for Leaderboard Screen (Podium + Top 100 rows).
 */
@Composable
fun LeaderboardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Podium Skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Rank 2
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonBox(modifier = Modifier.size(54.dp), shape = CircleShape)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp))
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(50.dp).height(65.dp), shape = RoundedCornerShape(8.dp))
            }
            // Rank 1
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonBox(modifier = Modifier.size(68.dp), shape = CircleShape)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.width(70.dp).height(14.dp))
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(56.dp).height(85.dp), shape = RoundedCornerShape(8.dp))
            }
            // Rank 3
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonBox(modifier = Modifier.size(50.dp), shape = CircleShape)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.width(55.dp).height(12.dp))
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(46.dp).height(50.dp), shape = RoundedCornerShape(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List items skeleton
        repeat(5) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(modifier = Modifier.size(32.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(12.dp))
                    SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp))
                    }
                    SkeletonBox(modifier = Modifier.width(48.dp).height(20.dp), shape = RoundedCornerShape(6.dp))
                }
            }
        }
    }
}

/**
 * Skeleton loading for Profile Screen.
 */
@Composable
fun ProfileSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonBox(modifier = Modifier.size(90.dp), shape = CircleShape)
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.45f).height(22.dp))
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
        Spacer(modifier = Modifier.height(24.dp))

        // Stats grid skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SkeletonBox(modifier = Modifier.size(24.dp), shape = CircleShape)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Menu items skeleton
        repeat(4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(modifier = Modifier.size(28.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(14.dp))
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                }
            }
        }
    }
}
