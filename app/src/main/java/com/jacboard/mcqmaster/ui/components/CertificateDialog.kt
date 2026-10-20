package com.jacboard.mcqmaster.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jacboard.mcqmaster.R
import com.jacboard.mcqmaster.model.DailyBestResult
import com.jacboard.mcqmaster.ui.theme.AccentGold
import com.jacboard.mcqmaster.ui.theme.PrimaryPurple
import com.jacboard.mcqmaster.ui.theme.SuccessGreen
import com.jacboard.mcqmaster.util.AppConfig
import com.jacboard.mcqmaster.util.DailyTestTimeUtils
import java.io.File

@Composable
fun CertificateDialog(
    result: DailyBestResult,
    pngFile: File,
    onDismiss: () -> Unit,
    onViewCertificate: () -> Unit,
    onShareCertificate: () -> Unit
) {
    val context = LocalContext.current
    val totalQuestions = if (result.totalQuestions > 0) {
        result.totalQuestions
    } else {
        maxOf(1, result.correctCount + result.wrongCount + result.notAttemptedCount)
    }
    val attemptedCount = result.correctCount + result.wrongCount
    val notAttemptedCount = if (result.notAttemptedCount > 0) {
        result.notAttemptedCount
    } else {
        maxOf(0, totalQuestions - attemptedCount)
    }
    val rollNumberStr = if (!result.rollNo.isNullOrBlank()) result.rollNo else "Unassigned"
    val actualDateDisplay = DailyTestTimeUtils.getFormattedDisplayDate(result.dateKey)
    val officialAppUrl = AppConfig.storeUrl
        ?.takeIf { it.isNotBlank() }
        ?: "https://play.google.com/store/apps/details?id=${context.packageName}"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("certificate_dialog_surface"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Official Certificate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "High-Quality A4 Landscape PNG",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("close_certificate_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Certificate Visual Representation Card (strictly recipient data only, no other students)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, AccentGold, RoundedCornerShape(16.dp))
                        .testTag("certificate_card_preview"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFE)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Logo
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, AccentGold, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "JAC MCQ Master Logo",
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "JAC MCQ MASTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "CERTIFICATE OF PARTICIPATION",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E1B4B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "This is proudly presented to",
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = result.studentName,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1B4B),
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(2.dp)
                                .background(AccentGold)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Student Roll Number & Course/Class
                        Text(
                            text = "Roll Number: $rollNumberStr   •   Class: ${result.studentClass.ifBlank { "Class 10" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3730A3),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Test Name & Actual Participation Date
                        Text(
                            text = "Test Name: Daily MCQ Test Challenge",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Actual Test Participation Date: $actualDateDisplay",
                            fontSize = 10.sp,
                            color = Color(0xFF4B5563),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Performance Stats (Row 1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CertificateMiniBadge(
                                label = "FINAL SCORE",
                                value = "${result.bestScore.toInt()}/${result.totalMarks.toInt()}",
                                color = SuccessGreen
                            )
                            CertificateMiniBadge(
                                label = "MARKS",
                                value = "${result.bestScore.toInt()} / ${result.totalMarks.toInt()}",
                                color = PrimaryPurple
                            )
                            CertificateMiniBadge(
                                label = "TIME TAKEN",
                                value = DailyTestTimeUtils.formatTimeTaken(result.bestCompletionTimeSeconds),
                                color = Color(0xFF2563EB)
                            )
                            CertificateMiniBadge(
                                label = "DAILY RANK",
                                value = if (result.rank > 0) "#${result.rank}" else "—",
                                color = AccentGold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Detailed Questions Breakdown (Row 2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CertificateMiniBadge(
                                label = "TOTAL",
                                value = "$totalQuestions Qs",
                                color = Color(0xFF334155)
                            )
                            CertificateMiniBadge(
                                label = "ATTEMPTED",
                                value = "$attemptedCount Qs",
                                color = Color(0xFF2563EB)
                            )
                            CertificateMiniBadge(
                                label = "CORRECT",
                                value = "${result.correctCount}",
                                color = SuccessGreen
                            )
                            CertificateMiniBadge(
                                label = "WRONG",
                                value = "${result.wrongCount}",
                                color = Color(0xFFDC2626)
                            )
                            CertificateMiniBadge(
                                label = "NOT ATTEMPTED",
                                value = "$notAttemptedCount",
                                color = Color(0xFFD97706)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Verification Trust Banner (NO other students, NO top 10 lists)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Official Verified Performance",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                                Text(
                                    text = "Private Record",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Footer Link & Info
                        Text(
                            text = "Official App: $officialAppUrl",
                            fontSize = 8.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Includes Scannable Verification QR Code on Exported PNG",
                            fontSize = 7.5.sp,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons: View Certificate and Share Certificate ONLY (No download / save to device)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewCertificate,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("view_certificate_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View Certificate",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = PrimaryPurple
                        )
                    }

                    Button(
                        onClick = onShareCertificate,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_certificate_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share Certificate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CertificateMiniBadge(
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.6.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}
