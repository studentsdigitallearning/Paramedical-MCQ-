package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.ClassData
import com.example.model.Student
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryPurple
import com.example.util.AppConfig

@Composable
fun AppSidebar(
    isOpen: Boolean,
    onClose: () -> Unit,
    student: Student? = null,
    currentClass: ClassData? = null,
    onSwitchClassClick: (() -> Unit)? = null,
    onNavigateAboutUs: () -> Unit,
    onNavigateContactUs: () -> Unit
) {
    val context = LocalContext.current

    // Intercept back button: pressing back closes the sidebar
    if (isOpen) {
        BackHandler {
            onClose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim backdrop (smooth fade)
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onClose()
                    }
                    .testTag("sidebar_scrim")
            )
        }

        // Slide-out Drawer Content (Visual reference: Image 1)
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(280)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(240)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 285.dp, max = 315.dp)
                    .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                    .shadow(20.dp, RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                    .testTag("app_sidebar_drawer"),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                ) {
                    // TOP PURPLE GRADIENT HEADER (Dynamic profile and class)
                    DrawerHeader(
                        student = student,
                        currentClass = currentClass,
                        onSwitchClassClick = onSwitchClassClick,
                        onClose = onClose
                    )

                    // MENU ITEMS (Exactly 7 items in the requested order)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        // 1. Privacy Policy
                        DrawerMenuItem(
                            icon = Icons.Default.Security,
                            iconTint = Color(0xFF7C3AED),
                            iconBg = Color(0xFFF3E8FF),
                            title = "Privacy Policy",
                            onClick = {
                                onClose()
                                AppConfig.openPrivacyPolicy(context)
                            }
                        )
                        DrawerDivider()

                        // 2. Terms & Conditions
                        DrawerMenuItem(
                            icon = Icons.Default.Description,
                            iconTint = Color(0xFF0284C7),
                            iconBg = Color(0xFFE0F2FE),
                            title = "Terms & Conditions",
                            onClick = {
                                onClose()
                                AppConfig.openTermsConditions(context)
                            }
                        )
                        DrawerDivider()

                        // 3. Contact Us
                        DrawerMenuItem(
                            icon = Icons.Default.Mail,
                            iconTint = Color(0xFF10B981),
                            iconBg = Color(0xFFDCFCE7),
                            title = "Contact Us",
                            onClick = {
                                onClose()
                                onNavigateContactUs()
                            }
                        )
                        DrawerDivider()

                        // 4. About Us
                        DrawerMenuItem(
                            icon = Icons.Default.Info,
                            iconTint = Color(0xFFF59E0B),
                            iconBg = Color(0xFFFEF3C7),
                            title = "About Us",
                            onClick = {
                                onClose()
                                onNavigateAboutUs()
                            }
                        )
                        DrawerDivider()

                        // 5. Share App
                        DrawerMenuItem(
                            icon = Icons.Default.Share,
                            iconTint = Color(0xFFF43F5E),
                            iconBg = Color(0xFFFFE4E6),
                            title = "Share App",
                            onClick = {
                                onClose()
                                AppConfig.shareApp(context)
                            }
                        )
                        DrawerDivider()

                        // 6. Rate App
                        DrawerMenuItem(
                            icon = Icons.Default.Star,
                            iconTint = Color(0xFFF59E0B),
                            iconBg = Color(0xFFFEF9C3),
                            title = "Rate App",
                            onClick = {
                                onClose()
                                AppConfig.rateApp(context)
                            }
                        )
                        DrawerDivider()

                        // 7. Official Website
                        DrawerMenuItem(
                            icon = Icons.Default.Language,
                            iconTint = Color(0xFF2563EB),
                            iconBg = Color(0xFFDBEAFE),
                            title = "Official Website",
                            onClick = {
                                onClose()
                                AppConfig.openWebsite(context)
                            }
                        )
                        DrawerDivider()

                        // 8. Telegram Community
                        DrawerMenuItem(
                            icon = Icons.Default.Send,
                            iconTint = Color(0xFF0284C7),
                            iconBg = Color(0xFFE0F2FE),
                            title = "Telegram Channel",
                            onClick = {
                                onClose()
                                AppConfig.openTelegramChannel(context)
                            }
                        )

                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

/**
 * Top Purple Gradient Header matching Image 1:
 * - App Logo (White rounded square with graduation cap)
 * - App Name ("Class Master MCQ" with gold accent)
 * - Tagline ("Learn • Practice • Excel")
 * - Profile row: Dynamic avatar with student initials + Name + Clickable Class dropdown
 * - Subtle book watermark in background
 */
@Composable
private fun DrawerHeader(
    student: Student?,
    currentClass: ClassData?,
    onSwitchClassClick: (() -> Unit)?,
    onClose: () -> Unit
) {
    val displayName = student?.name?.takeIf { it.isNotBlank() } ?: "Student"
    val initials = displayName.trim().split("\\s+".toRegex())
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { "ST" }
    val displayClassName = currentClass?.name ?: "Class 10"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF4C1D95),
                        Color(0xFF6026C8),
                        Color(0xFF6D28D9)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Decorative faint book watermark on the right
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.10f),
            modifier = Modifier
                .size(92.dp)
                .align(Alignment.BottomEnd)
                .rotate(-15f)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // App Logo & App Name Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Beautiful App Logo Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color(0x30000000))
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JAC MCQ ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Master",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentGold
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Learn • Practice • Excel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Profile Row: Dynamic Avatar + Name + Class Selector
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Avatar with dynamic student initials
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C3AED))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Clickable Class row allowing switching directly from sidebar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = onSwitchClassClick != null) {
                                onClose()
                                onSwitchClassClick?.invoke()
                            }
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                            .testTag("sidebar_class_switch_row")
                    ) {
                        Text(
                            text = displayClassName,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Class",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean Menu Item matching Image 1:
 * - Rounded tinted square for the icon (40.dp x 40.dp)
 * - Title text (15.sp, SemiBold)
 * - Trailing subtle chevron `>`
 */
@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag("drawer_item_${title.lowercase().replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Subtle divider between menu items
 */
@Composable
private fun DrawerDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp),
        thickness = 0.7.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    )
}

