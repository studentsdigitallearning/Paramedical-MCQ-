package com.jacboard.mcqmaster.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.jacboard.mcqmaster.R
import com.jacboard.mcqmaster.model.DailyBestResult
import java.io.File
import java.io.FileOutputStream

object CertificateGenerator {

    // A4 Landscape dimensions in pixels (1684 x 1190, ~150 DPI)
    private const val PAGE_WIDTH = 1684
    private const val PAGE_HEIGHT = 1190

    /**
     * Generates a high-quality A4 landscape PNG certificate for the Daily MCQ Test Challenge.
     * Contains ONLY the recipient's verified performance details.
     * Stored strictly in private app cache.
     */
    fun generateCertificate(
        context: Context,
        result: DailyBestResult
    ): File {
        // Safe cleanup of older temporary certificates in private cache
        cleanupTemporaryCertificates(context)

        val certDir = File(context.cacheDir, "certificates")
        if (!certDir.exists()) {
            certDir.mkdirs()
        }

        val sanitizedDate = result.dateKey.replace("-", "")
        val sanitizedName = result.studentName.trim()
            .replace(" ", "_")
            .replace("[^a-zA-Z0-9_]".toRegex(), "")
            .ifBlank { "Student" }
        val pngFile = File(certDir, "Certificate_DailyTest_${sanitizedDate}_${sanitizedName}.png")

        val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawCertificateOnCanvas(context, canvas, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), result)

        FileOutputStream(pngFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        return pngFile
    }

    private fun drawCertificateOnCanvas(
        context: Context,
        canvas: Canvas,
        width: Float,
        height: Float,
        result: DailyBestResult
    ) {
        // 1. Crisp Elegant Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FDFDFE")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Double Frame Border (Royal Purple & Warm Amber Gold)
        val outerBorderPaint = Paint().apply {
            color = Color.parseColor("#4C1D95") // Deep Royal Purple
            style = Paint.Style.STROKE
            strokeWidth = 10f
            isAntiAlias = true
        }
        canvas.drawRect(36f, 36f, width - 36f, height - 36f, outerBorderPaint)

        val innerGoldPaint = Paint().apply {
            color = Color.parseColor("#F59E0B") // Amber Gold
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            isAntiAlias = true
        }
        canvas.drawRect(52f, 52f, width - 52f, height - 52f, innerGoldPaint)

        val hairlinePaint = Paint().apply {
            color = Color.parseColor("#E0E7FF")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawRect(60f, 60f, width - 60f, height - 60f, hairlinePaint)

        // Corner Ornamental Flourishes
        drawCornerDecorations(canvas, width, height)

        // 3. JAC MCQ Master Logo (Top Left)
        try {
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
            if (logoBitmap != null) {
                val logoSize = 130
                val logoDest = RectF(100f, 90f, 100f + logoSize, 90f + logoSize)
                // Draw rounded background card behind logo
                val logoBgPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    isAntiAlias = true
                    setShadowLayer(8f, 0f, 4f, Color.parseColor("#33000000"))
                }
                canvas.drawRoundRect(logoDest, 20f, 20f, logoBgPaint)
                canvas.drawBitmap(logoBitmap, null, logoDest, Paint(Paint.FILTER_BITMAP_FLAG))

                val logoBorderPaint = Paint().apply {
                    color = Color.parseColor("#F59E0B")
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                    isAntiAlias = true
                }
                canvas.drawRoundRect(logoDest, 20f, 20f, logoBorderPaint)
            }
        } catch (_: Exception) {
            // Graceful fallback if resource is unavailable
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // 4. Header & Branding (Centered)
        textPaint.apply {
            color = Color.parseColor("#5B21B6")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.18f
        }
        canvas.drawText("JAC MCQ MASTER • ALL-INDIA DAILY TEST SERIES", width / 2f, 120f, textPaint)

        // 5. Main Certificate Title
        textPaint.apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("CERTIFICATE OF PARTICIPATION", width / 2f, 185f, textPaint)

        // 6. Presentation Line
        textPaint.apply {
            color = Color.parseColor("#64748B")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.04f
        }
        canvas.drawText("This is proudly presented to", width / 2f, 235f, textPaint)

        // 7. Student Name
        textPaint.apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 50f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = 0.02f
        }
        canvas.drawText(result.studentName, width / 2f, 305f, textPaint)

        // Decorative underline with diamond accent
        val linePaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            strokeWidth = 4f
            isAntiAlias = true
        }
        val lineHalfWidth = 260f
        canvas.drawLine(width / 2f - lineHalfWidth, 325f, width / 2f + lineHalfWidth, 325f, linePaint)

        val diamondPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, 325f, 6f, diamondPaint)

        // 8. Student Roll Number & Course/Class Details
        val rollNumberStr = if (!result.rollNo.isNullOrBlank()) result.rollNo else "Unassigned"
        val courseClassStr = result.studentClass.ifBlank { "Course Participant" }

        textPaint.apply {
            color = Color.parseColor("#3730A3")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.04f
        }
        canvas.drawText(
            "Roll Number: $rollNumberStr    •    Course / Class: $courseClassStr",
            width / 2f,
            370f,
            textPaint
        )

        // 9. Test Name & Actual Test Participation Date
        val actualTestDate = DailyTestTimeUtils.getFormattedDisplayDate(result.dateKey)
        textPaint.apply {
            color = Color.parseColor("#475569")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.02f
        }
        canvas.drawText(
            "for participating in the Daily MCQ Test Challenge on $actualTestDate",
            width / 2f,
            412f,
            textPaint
        )

        // 10. Performance Details Grid (No other students, no top 10, purely recipient's verified metrics)
        drawPerformanceGrid(canvas, width, result)

        // 11. Footer Section: App Link, Verified QR Code, and Official Seal
        drawFooterWithQrCode(context, canvas, width, height, actualTestDate)
    }

    private fun drawPerformanceGrid(
        canvas: Canvas,
        width: Float,
        result: DailyBestResult
    ) {
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
        val timeTakenStr = DailyTestTimeUtils.formatTimeTaken(result.bestCompletionTimeSeconds)
        val finalScoreStr = "${result.bestScore.toInt()}/${result.totalMarks.toInt()}"

        // Performance Section Title Card
        val sectionTop = 455f
        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        textPaint.apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("— OFFICIAL TEST PERFORMANCE METRICS —", width / 2f, sectionTop, textPaint)

        // Row 1: 4 Primary Performance Summary Cards
        val row1Top = 475f
        val cardHeight1 = 110f
        val cardWidth1 = 340f
        val cardSpacing1 = 28f
        val totalRow1Width = (4 * cardWidth1) + (3 * cardSpacing1)
        val startX1 = (width - totalRow1Width) / 2f

        val primaryStats = listOf(
            Triple("FINAL SCORE", finalScoreStr, "#059669"),
            Triple("TOTAL MARKS OBTAINED", "${result.bestScore.toInt()} / ${result.totalMarks.toInt()}", "#4F46E5"),
            Triple("TIME TAKEN", timeTakenStr, "#2563EB"),
            Triple("DAILY RANK", if (result.rank > 0) "#${result.rank}" else "Verified", "#D97706")
        )

        primaryStats.forEachIndexed { i, stat ->
            val curX = startX1 + i * (cardWidth1 + cardSpacing1)
            val rect = RectF(curX, row1Top, curX + cardWidth1, row1Top + cardHeight1)

            val bgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, 14f, 14f, bgPaint)

            val borderPaint = Paint().apply {
                color = Color.parseColor(stat.third)
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, 14f, 14f, borderPaint)

            // Label
            textPaint.apply {
                color = Color.parseColor("#64748B")
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
            }
            canvas.drawText(stat.first, curX + cardWidth1 / 2f, row1Top + 38f, textPaint)

            // Value
            textPaint.apply {
                color = Color.parseColor(stat.third)
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.02f
            }
            canvas.drawText(stat.second, curX + cardWidth1 / 2f, row1Top + 86f, textPaint)
        }

        // Row 2: 5 Breakdown Metric Cards
        val row2Top = 610f
        val cardHeight2 = 96f
        val cardWidth2 = 270f
        val cardSpacing2 = 20f
        val totalRow2Width = (5 * cardWidth2) + (4 * cardSpacing2)
        val startX2 = (width - totalRow2Width) / 2f

        val breakdownStats = listOf(
            Triple("TOTAL QUESTIONS", "$totalQuestions", "#334155"),
            Triple("QUESTIONS ATTEMPTED", "$attemptedCount", "#2563EB"),
            Triple("CORRECT ANSWERS", "${result.correctCount}", "#059669"),
            Triple("WRONG ANSWERS", "${result.wrongCount}", "#DC2626"),
            Triple("NOT ATTEMPTED", "$notAttemptedCount", "#D97706")
        )

        breakdownStats.forEachIndexed { i, stat ->
            val curX = startX2 + i * (cardWidth2 + cardSpacing2)
            val rect = RectF(curX, row2Top, curX + cardWidth2, row2Top + cardHeight2)

            val bgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, 12f, 12f, bgPaint)

            val borderPaint = Paint().apply {
                color = Color.parseColor(stat.third).let {
                    // Muted border for row 2
                    Color.argb(160, Color.red(it), Color.green(it), Color.blue(it))
                }
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, 12f, 12f, borderPaint)

            // Label
            textPaint.apply {
                color = Color.parseColor("#64748B")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.06f
            }
            canvas.drawText(stat.first, curX + cardWidth2 / 2f, row2Top + 34f, textPaint)

            // Value
            textPaint.apply {
                color = Color.parseColor(stat.third)
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.02f
            }
            canvas.drawText(stat.second, curX + cardWidth2 / 2f, row2Top + 76f, textPaint)
        }
    }

    private fun drawFooterWithQrCode(
        context: Context,
        canvas: Canvas,
        width: Float,
        height: Float,
        actualTestDate: String
    ) {
        val footerDividerY = 740f
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }
        canvas.drawLine(100f, footerDividerY, width - 100f, footerDividerY, dividerPaint)

        val textPaint = Paint().apply {
            isAntiAlias = true
        }

        // Left Side: App Verification & Trust Info
        textPaint.apply {
            textAlign = Paint.Align.LEFT
            color = Color.parseColor("#4C1D95")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.02f
        }
        canvas.drawText("JAC MCQ Master — Official Certificate", 100f, footerDividerY + 50f, textPaint)

        textPaint.apply {
            color = Color.parseColor("#64748B")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.01f
        }
        canvas.drawText("Learn • Practice • Excel", 100f, footerDividerY + 84f, textPaint)
        canvas.drawText("Participation Date: $actualTestDate", 100f, footerDividerY + 116f, textPaint)
        canvas.drawText("Verified Digital Participant Record (No public ranking disclosure)", 100f, footerDividerY + 148f, textPaint)

        val officialAppUrl = AppConfig.storeUrl
            ?.takeIf { it.isNotBlank() }
            ?: "https://play.google.com/store/apps/details?id=${context.packageName}"

        canvas.drawText("Official App Link: $officialAppUrl", 100f, footerDividerY + 180f, textPaint)

        // Center Seal/Badge
        val sealX = width / 2f + 40f
        val sealY = footerDividerY + 115f

        val sealCirclePaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, 52f, sealCirclePaint)

        val sealInnerPaint = Paint().apply {
            color = Color.parseColor("#FEF3C7")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, 48f, sealInnerPaint)

        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#B45309")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("★ OFFICIAL ★", sealX, sealY - 12f, textPaint)
        canvas.drawText("VERIFIED", sealX, sealY + 6f, textPaint)
        canvas.drawText("PARTICIPANT", sealX, sealY + 24f, textPaint)

        // Right Side: Scannable QR Code pointing to verified official app link
        val qrSize = 170
        val qrX = width - 100f - qrSize
        val qrY = footerDividerY + 25f

        try {
            val qrBitmap = generateQrBitmap(officialAppUrl, qrSize)
            val qrDest = RectF(qrX, qrY, qrX + qrSize, qrY + qrSize)

            // White rounded backing
            val qrBackingPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(qrDest, 12f, 12f, qrBackingPaint)

            val qrBorderPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(qrDest, 12f, 12f, qrBorderPaint)

            canvas.drawBitmap(qrBitmap, null, qrDest, Paint(Paint.FILTER_BITMAP_FLAG))
            qrBitmap.recycle()

            textPaint.apply {
                textAlign = Paint.Align.CENTER
                color = Color.parseColor("#64748B")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("SCAN TO VERIFY", qrX + qrSize / 2f, qrY + qrSize + 22f, textPaint)
        } catch (_: Exception) {
            // In the rare event of QR generation failure, display official link text
            textPaint.apply {
                textAlign = Paint.Align.RIGHT
                color = Color.parseColor("#4C1D95")
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Official App Link:", width - 100f, footerDividerY + 60f, textPaint)
            textPaint.color = Color.parseColor("#2563EB")
            canvas.drawText(officialAppUrl, width - 100f, footerDividerY + 90f, textPaint)
        }
    }

    private fun generateQrBitmap(content: String, size: Int): Bitmap {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val darkColor = Color.parseColor("#1E1B4B")
        val lightColor = Color.WHITE
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) darkColor else lightColor)
            }
        }
        return bmp
    }

    private fun drawCornerDecorations(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val len = 44f

        // Top-left
        canvas.drawLine(52f, 52f, 52f + len, 52f, paint)
        canvas.drawLine(52f, 52f, 52f, 52f + len, paint)

        // Top-right
        canvas.drawLine(width - 52f, 52f, width - 52f - len, 52f, paint)
        canvas.drawLine(width - 52f, 52f, width - 52f, 52f + len, paint)

        // Bottom-left
        canvas.drawLine(52f, height - 52f, 52f + len, height - 52f, paint)
        canvas.drawLine(52f, height - 52f, 52f, height - 52f - len, paint)

        // Bottom-right
        canvas.drawLine(width - 52f, height - 52f, width - 52f - len, height - 52f, paint)
        canvas.drawLine(width - 52f, height - 52f, width - 52f, height - 52f - len, paint)
    }

    /**
     * Share certificate PNG using Android's native Sharesheet with ACTION_SEND and image/png MIME type.
     * Uses temporary read-only URI permission (FLAG_GRANT_READ_URI_PERMISSION).
     * No storage permissions required.
     */
    fun shareCertificate(context: Context, file: File, studentName: String, score: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Certificate of Participation — $studentName"
            )
            val storeUrl = AppConfig.storeUrl
                ?.takeIf { it.isNotBlank() }
                ?: "https://play.google.com/store/apps/details?id=${context.packageName}"
            putExtra(
                Intent.EXTRA_TEXT,
                "Here is my Certificate of Participation for the Daily MCQ Test Challenge ($score)!\nGenerated from JAC MCQ Master App: $storeUrl"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Certificate via"))
    }

    /**
     * View certificate PNG with external image viewer or gallery app.
     * Uses temporary read-only URI permission (FLAG_GRANT_READ_URI_PERMISSION).
     * No storage permissions required.
     */
    fun viewCertificate(context: Context, file: File, studentName: String, score: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "View Certificate"))
        } catch (_: Exception) {
            shareCertificate(context, file, studentName, score)
        }
    }

    /**
     * Safely cleans up temporary certificate PNG files from private app cache.
     * Keeps cache footprint minimal and safe.
     */
    fun cleanupTemporaryCertificates(context: Context) {
        try {
            val certDir = File(context.cacheDir, "certificates")
            if (certDir.exists() && certDir.isDirectory) {
                val files = certDir.listFiles() ?: return
                val now = System.currentTimeMillis()
                // Retain only files created within the last 12 hours, up to maximum 3 files
                files.sortedByDescending { it.lastModified() }.forEachIndexed { index, file ->
                    val isOld = (now - file.lastModified()) > (12 * 60 * 60 * 1000L)
                    if (index >= 2 || isOld) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {
            // Non-critical cache cleanup exception ignored safely
        }
    }
}
