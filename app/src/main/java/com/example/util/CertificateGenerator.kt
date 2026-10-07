package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.DailyBestResult
import com.example.model.DailyLeaderboardEntry
import java.io.File
import java.io.FileOutputStream

object CertificateGenerator {

    /**
     * Generates an elegant, high-quality participation certificate PDF for the Daily Test.
     * Uses A4 Landscape format (842 x 595 points)
     */
    fun generateCertificate(
        context: Context,
        result: DailyBestResult,
        top10: List<DailyLeaderboardEntry>
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 842
        val pageHeight = 595

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawCertificateOnCanvas(canvas, pageWidth.toFloat(), pageHeight.toFloat(), result, top10)

        pdfDocument.finishPage(page)

        // Save PDF to cache/certificates directory
        val certDir = File(context.cacheDir, "certificates")
        if (!certDir.exists()) {
            certDir.mkdirs()
        }

        val sanitizedDate = result.dateKey.replace("-", "")
        val sanitizedName = result.studentName.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val pdfFile = File(certDir, "Certificate_DailyTest_${sanitizedDate}_${sanitizedName}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    private fun drawCertificateOnCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        result: DailyBestResult,
        top10: List<DailyLeaderboardEntry>
    ) {
        // 1. Crisp Elegant Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FDFDFE")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Double Frame Border (Royal Purple & Warm Gold)
        // Outer Border
        val outerBorderPaint = Paint().apply {
            color = Color.parseColor("#4C1D95") // Deep Royal Purple
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }
        canvas.drawRect(18f, 18f, width - 18f, height - 18f, outerBorderPaint)

        // Inner Golden Accent Frame
        val innerGoldPaint = Paint().apply {
            color = Color.parseColor("#F59E0B") // Amber Gold
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            isAntiAlias = true
        }
        canvas.drawRect(26f, 26f, width - 26f, height - 26f, innerGoldPaint)

        // Hairline Inset
        val hairlinePaint = Paint().apply {
            color = Color.parseColor("#E0E7FF")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        canvas.drawRect(30f, 30f, width - 30f, height - 30f, hairlinePaint)

        // Corner Ornamental Flourishes
        drawCornerDecorations(canvas, width, height)

        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // 3. Header: App Branding / Category
        textPaint.apply {
            color = Color.parseColor("#5B21B6") // Primary Purple
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.22f
        }
        canvas.drawText("MCQ TEST SERIES • ALL-INDIA DAILY CHALLENGE", width / 2f, 56f, textPaint)

        // 4. Main Certificate Title: CERTIFICATE OF PARTICIPATION
        textPaint.apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        canvas.drawText("CERTIFICATE OF PARTICIPATION", width / 2f, 95f, textPaint)

        // 5. Subtitle: Presentation line
        textPaint.apply {
            color = Color.parseColor("#64748B")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.04f
        }
        canvas.drawText("This is proudly presented to", width / 2f, 120f, textPaint)

        // 6. Student Name
        textPaint.apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 27f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = 0.02f
        }
        canvas.drawText(result.studentName, width / 2f, 156f, textPaint)

        // Elegant Underline Accent
        val linePaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            strokeWidth = 2.2f
            isAntiAlias = true
        }
        canvas.drawLine(width / 2f - 130f, 166f, width / 2f + 130f, 166f, linePaint)

        // Diamond center on underline
        val diamondPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, 166f, 3.5f, diamondPaint)

        // 7. Commendation / Details
        val displayDate = DailyTestTimeUtils.getFormattedDisplayDate(result.dateKey)
        val studentClass = result.studentClass.ifBlank { "Class 10" }

        textPaint.apply {
            color = Color.parseColor("#475569")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.01f
        }
        canvas.drawText(
            "for participating in the Daily All-India MCQ Test Challenge ($studentClass) on $displayDate.",
            width / 2f,
            190f,
            textPaint
        )

        // 8. Performance 4 Key Stats (Cards Grid)
        val boxTop = 208f
        val boxHeight = 52f
        val boxWidth = 142f
        val boxSpacing = 16f
        val totalBoxWidth = (4 * boxWidth) + (3 * boxSpacing)
        val startBoxX = (width - totalBoxWidth) / 2f

        val stats = listOf(
            Triple("RANK", "#${result.rank}", "#4F46E5"),
            Triple("SCORE", "${result.bestScore.toInt()} / ${result.totalMarks.toInt()}", "#059669"),
            Triple("TIME TAKEN", DailyTestTimeUtils.formatTimeTaken(result.bestCompletionTimeSeconds), "#2563EB"),
            Triple("PARTICIPANTS", "%,d".format(result.totalParticipants), "#D97706")
        )

        stats.forEachIndexed { i, stat ->
            val curX = startBoxX + i * (boxWidth + boxSpacing)
            val rect = RectF(curX, boxTop, curX + boxWidth, boxTop + boxHeight)

            // Box Background
            val statBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, 8f, 8f, statBgPaint)

            // Box Border
            val statBorderPaint = Paint().apply {
                color = Color.parseColor(stat.third)
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, 8f, 8f, statBorderPaint)

            // Stat Label
            textPaint.apply {
                color = Color.parseColor("#64748B")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
            }
            canvas.drawText(stat.first, curX + boxWidth / 2f, boxTop + 18f, textPaint)

            // Stat Value
            textPaint.apply {
                color = Color.parseColor(stat.third)
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.02f
            }
            canvas.drawText(stat.second, curX + boxWidth / 2f, boxTop + 40f, textPaint)
        }

        // 9. Official Daily Top 10 Rankers
        val top10Top = 276f
        val top10Height = 166f
        val top10Width = width - 110f
        val top10X = 55f
        val top10Rect = RectF(top10X, top10Top, top10X + top10Width, top10Top + top10Height)

        val top10BgPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(top10Rect, 10f, 10f, top10BgPaint)

        val top10BorderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(top10Rect, 10f, 10f, top10BorderPaint)

        // Top 10 Header
        textPaint.apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.04f
        }
        canvas.drawText("🏆 OFFICIAL TOP 10 RANKERS — $displayDate", top10X + 18f, top10Top + 24f, textPaint)

        // Top 10 Columns: 5 on left, 5 on right
        val colWidth = (top10Width - 44f) / 2f
        val top10List = top10.take(10)

        top10List.forEachIndexed { idx, entry ->
            val colIndex = if (idx < 5) 0 else 1
            val rowIndex = if (idx < 5) idx else idx - 5

            val entryX = top10X + 18f + (colIndex * (colWidth + 14f))
            val entryY = top10Top + 48f + (rowIndex * 24f)

            val rankPrefix = when (idx + 1) {
                1 -> "🥇 #1"
                2 -> "🥈 #2"
                3 -> "🥉 #3"
                else -> "#${idx + 1}"
            }

            val isUser = entry.isCurrentUser || entry.studentId == result.studentId
            val nameColor = if (isUser) Color.parseColor("#4C1D95") else Color.parseColor("#1F2937")

            textPaint.apply {
                color = nameColor
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, if (isUser) Typeface.BOLD else Typeface.NORMAL)
                textAlign = Paint.Align.LEFT
            }

            val timeStr = DailyTestTimeUtils.formatTimeTaken(entry.completionTimeSeconds)
            val scoreStr = "${entry.score.toInt()}/${entry.totalMarks.toInt()} ($timeStr)"
            val fullText = "$rankPrefix  ${entry.studentName}"

            canvas.drawText(fullText, entryX, entryY, textPaint)

            textPaint.apply {
                color = Color.parseColor("#4B5563")
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(scoreStr, entryX + colWidth, entryY, textPaint)
        }

        // 10. Clean Footer: Generated from App, Official verification badge (NO signatures, NO certificate number)
        val footerDividerY = 466f
        val footerDividerPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        canvas.drawLine(55f, footerDividerY, width - 55f, footerDividerY, footerDividerPaint)

        // Center Seal/Badge: Verified Participant
        val sealX = width / 2f
        val sealY = footerDividerY + 36f

        val sealCirclePaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, 24f, sealCirclePaint)

        val sealInnerCirclePaint = Paint().apply {
            color = Color.parseColor("#FEF3C7")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, 22f, sealInnerCirclePaint)

        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#B45309")
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("★ OFFICIAL ★", sealX, sealY - 4f, textPaint)
        canvas.drawText("PARTICIPANT", sealX, sealY + 5f, textPaint)
        canvas.drawText("DAILY TEST", sealX, sealY + 14f, textPaint)

        // Left Side of Footer: "Generated from MCQ Test Series App"
        textPaint.apply {
            textAlign = Paint.Align.LEFT
            color = Color.parseColor("#4C1D95")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.02f
        }
        canvas.drawText("Generated from MCQ Test Series App", 55f, footerDividerY + 28f, textPaint)

        textPaint.apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.01f
        }
        canvas.drawText("Learn • Practice • Excel", 55f, footerDividerY + 44f, textPaint)
        canvas.drawText("Issued On: $displayDate (All-India Daily Ranking Cycle)", 55f, footerDividerY + 58f, textPaint)

        // Right Side of Footer: Official Portal Links
        textPaint.apply {
            textAlign = Paint.Align.RIGHT
            color = Color.parseColor("#4C1D95")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Official Portal: ${DailyTestTimeUtils.APP_WEBSITE_URL}", width - 55f, footerDividerY + 28f, textPaint)

        textPaint.apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Social: YouTube • Telegram • Instagram @mcqtestseries", width - 55f, footerDividerY + 44f, textPaint)
        canvas.drawText("Verified Digital Participant Record", width - 55f, footerDividerY + 58f, textPaint)
    }

    private fun drawCornerDecorations(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            strokeWidth = 2.2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val len = 22f

        // Top-left
        canvas.drawLine(26f, 26f, 26f + len, 26f, paint)
        canvas.drawLine(26f, 26f, 26f, 26f + len, paint)

        // Top-right
        canvas.drawLine(width - 26f, 26f, width - 26f - len, 26f, paint)
        canvas.drawLine(width - 26f, 26f, width - 26f, 26f + len, paint)

        // Bottom-left
        canvas.drawLine(26f, height - 26f, 26f + len, height - 26f, paint)
        canvas.drawLine(26f, height - 26f, 26f, height - 26f - len, paint)

        // Bottom-right
        canvas.drawLine(width - 26f, height - 26f, width - 26f - len, height - 26f, paint)
        canvas.drawLine(width - 26f, height - 26f, width - 26f, height - 26f - len, paint)
    }

    /**
     * Share certificate PDF using Android system share intent
     */
    fun shareCertificate(context: Context, file: File, studentName: String, rank: Int) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Certificate of Participation — $studentName"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Here is my Certificate of Participation for securing Rank #$rank in today's Daily MCQ Challenge!\nGenerated from MCQ Test Series App\n${DailyTestTimeUtils.APP_WEBSITE_URL}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Certificate via"))
    }

    /**
     * Download certificate PDF directly to system Downloads folder
     */
    fun downloadCertificateToDownloads(context: Context, file: File): Boolean {
        return try {
            val fileName = file.name
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    android.widget.Toast.makeText(context, "Certificate downloaded to Downloads folder!", android.widget.Toast.LENGTH_LONG).show()
                    return true
                }
            }

            // Fallback for older Android
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, fileName)
            file.copyTo(destFile, overwrite = true)
            android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("application/pdf"), null)
            android.widget.Toast.makeText(context, "Certificate saved to Downloads folder!", android.widget.Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Unable to save to Downloads: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Open certificate PDF using external PDF viewer app
     */
    fun openCertificate(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Certificate with"))
        } catch (_: Exception) {
            shareCertificate(context, file, "Student", 1)
        }
    }
}
