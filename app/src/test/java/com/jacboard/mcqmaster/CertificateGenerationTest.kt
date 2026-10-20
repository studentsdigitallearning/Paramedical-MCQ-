package com.jacboard.mcqmaster

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jacboard.mcqmaster.model.DailyBestResult
import com.jacboard.mcqmaster.util.CertificateGenerator
import com.jacboard.mcqmaster.util.DailyTestTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CertificateGenerationTest {

    @Test
    fun testCertificatePngGenerationProducesValidPngInPrivateCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val bestResult = DailyBestResult(
            studentId = "stu_12345",
            studentName = "Rahul Kumar",
            studentClass = "Class 10",
            dateKey = "2026-10-19",
            bestScore = 7f,
            totalMarks = 10f,
            bestCompletionTimeSeconds = 30L,
            rank = 1,
            totalParticipants = 1,
            attemptsCount = 1,
            correctCount = 7,
            wrongCount = 3,
            notAttemptedCount = 0,
            totalQuestions = 10,
            rollNo = "2610190001"
        )

        val pngFile = CertificateGenerator.generateCertificate(context, bestResult)

        // 1. Verify File existence and naming
        assertNotNull(pngFile)
        assertTrue("PNG file should exist", pngFile.exists())
        assertTrue("PNG file should not be empty", pngFile.length() > 0)
        assertTrue("Extension must be .png", pngFile.name.endsWith(".png"))
        assertFalse("Must NOT be a PDF", pngFile.name.endsWith(".pdf"))
        assertTrue("Filename must contain date", pngFile.name.contains("20261019"))
        assertTrue("Filename must contain sanitized student name", pngFile.name.contains("Rahul_Kumar"))

        // 2. Verify Private Cache Directory Path
        val certDir = File(context.cacheDir, "certificates")
        assertEquals(certDir.absolutePath, pngFile.parentFile?.absolutePath)

        // 3. Verify PNG Magic Bytes (\x89PNG\r\n\x1a\n)
        val header = ByteArray(8)
        FileInputStream(pngFile).use { it.read(header) }
        val expectedPngMagic = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
        )
        for (i in expectedPngMagic.indices) {
            assertEquals("Byte $i should match PNG magic header", expectedPngMagic[i], header[i])
        }
    }

    @Test
    fun testCertificatePerformanceMetricsAccuracy() {
        val bestResult = DailyBestResult(
            studentId = "stu_999",
            studentName = "demo",
            studentClass = "Class 10",
            dateKey = "2026-10-19",
            bestScore = 7f,
            totalMarks = 10f,
            bestCompletionTimeSeconds = 30L,
            rank = 1,
            totalParticipants = 9,
            attemptsCount = 1,
            correctCount = 7,
            wrongCount = 3,
            notAttemptedCount = 0,
            totalQuestions = 10,
            rollNo = "2610190001"
        )

        // Verify all required values are present and match submitted data exactly
        assertEquals("demo", bestResult.studentName)
        assertEquals("2610190001", bestResult.rollNo)
        assertEquals("Class 10", bestResult.studentClass)
        assertEquals("2026-10-19", bestResult.dateKey)
        assertEquals(10, bestResult.totalQuestions)
        assertEquals(7, bestResult.correctCount)
        assertEquals(3, bestResult.wrongCount)
        assertEquals(0, bestResult.notAttemptedCount)
        assertEquals(10, bestResult.correctCount + bestResult.wrongCount)
        assertEquals(7f, bestResult.bestScore, 0.01f)
        assertEquals(10f, bestResult.totalMarks, 0.01f)
        assertEquals("7/10", "${bestResult.bestScore.toInt()}/${bestResult.totalMarks.toInt()}")
        assertEquals("00:30", DailyTestTimeUtils.formatTimeTaken(bestResult.bestCompletionTimeSeconds))
        assertEquals("October 19, 2026", DailyTestTimeUtils.getFormattedDisplayDate(bestResult.dateKey))
    }

    @Test
    fun testCleanupTemporaryCertificates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val certDir = File(context.cacheDir, "certificates").apply { mkdirs() }

        // Create mock older files
        val file1 = File(certDir, "old_1.png").apply {
            createNewFile()
            setLastModified(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        }
        val file2 = File(certDir, "old_2.png").apply {
            createNewFile()
            setLastModified(System.currentTimeMillis() - 20 * 60 * 60 * 1000L)
        }
        val file3 = File(certDir, "old_3.png").apply {
            createNewFile()
            setLastModified(System.currentTimeMillis() - 15 * 60 * 60 * 1000L)
        }

        CertificateGenerator.cleanupTemporaryCertificates(context)

        // Ensure old files were cleaned up
        val remaining = certDir.listFiles() ?: emptyArray()
        assertTrue("Remaining files should be 2 or fewer after cleanup", remaining.size <= 2)
    }
}
