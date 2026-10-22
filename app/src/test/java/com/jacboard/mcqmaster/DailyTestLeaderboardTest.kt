package com.jacboard.mcqmaster

import com.jacboard.mcqmaster.data.AppRepository
import com.jacboard.mcqmaster.model.DailyLeaderboardEntry
import com.jacboard.mcqmaster.util.CourseClassUtils
import com.jacboard.mcqmaster.util.DailyTestTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTestLeaderboardTest {

    @Test
    fun testLeaderboardRankingPriorityRules() {
        // Requirement 3 Ranking Rules:
        // 1. Highest score first (score DESC)
        // 2. Highest accuracy next (accuracy DESC)
        // 3. Earliest valid submission timestamp (submittedAtEpoch ASC, completionTimeSeconds ASC, studentId ASC)
        val students = listOf(
            DailyLeaderboardEntry(
                rank = 0,
                studentId = "D",
                studentName = "Student D",
                className = "Class 10",
                score = 48f,
                totalMarks = 50f,
                accuracy = 96f,
                submittedAtEpoch = 1000L,
                completionTimeSeconds = 380L
            ),
            DailyLeaderboardEntry(
                rank = 0,
                studentId = "B",
                studentName = "Student B",
                className = "Class 10",
                score = 50f,
                totalMarks = 50f,
                accuracy = 95f,
                submittedAtEpoch = 900L,
                completionTimeSeconds = 500L
            ),
            DailyLeaderboardEntry(
                rank = 0,
                studentId = "A",
                studentName = "Student A",
                className = "Class 10",
                score = 50f,
                totalMarks = 50f,
                accuracy = 100f,
                submittedAtEpoch = 950L,
                completionTimeSeconds = 500L
            ),
            DailyLeaderboardEntry(
                rank = 0,
                studentId = "C",
                studentName = "Student C",
                className = "Class 10",
                score = 50f,
                totalMarks = 50f,
                accuracy = 100f,
                submittedAtEpoch = 990L, // Later timestamp than Student A -> ranked after A
                completionTimeSeconds = 450L
            )
        )

        val sorted = students.sortedWith(DailyLeaderboardEntry.RANKING_COMPARATOR)

        assertEquals("Student A", sorted[0].studentName)
        assertEquals("Student C", sorted[1].studentName)
        assertEquals("Student B", sorted[2].studentName)
        assertEquals("Student D", sorted[3].studentName)
    }

    @Test
    fun testBestAttemptLogicHighestScoreAndFastestTime() {
        var bestScore = 38f
        var bestTime = 750L

        // Attempt 2: 43/50 in 850s
        val att2Score = 43f
        val att2Time = 850L
        if (att2Score > bestScore || (att2Score == bestScore && att2Time < bestTime)) {
            bestScore = att2Score
            bestTime = att2Time
        }
        assertEquals(43f, bestScore, 0.01f)
        assertEquals(850L, bestTime)

        // Attempt 3: 41/50 in 620s (worse score never replaces best)
        val att3Score = 41f
        val att3Time = 620L
        if (att3Score > bestScore || (att3Score == bestScore && att3Time < bestTime)) {
            bestScore = att3Score
            bestTime = att3Time
        }
        assertEquals(43f, bestScore, 0.01f)
        assertEquals(850L, bestTime)

        // Attempt 4: Equal score 43/50 in faster time 615s
        val att4Score = 43f
        val att4Time = 615L
        if (att4Score > bestScore || (att4Score == bestScore && att4Time < bestTime)) {
            bestScore = att4Score
            bestTime = att4Time
        }
        assertEquals(43f, bestScore, 0.01f)
        assertEquals(615L, bestTime)
    }

    @Test
    fun test100StudentsSubmitAnd101stHigherOrLowerScorePermanentDeletion() {
        val repo = AppRepository.getInstance()
        val testDate = "2026-10-21"
        repo.executeMidnightPermanentDeletion("2099-01-01") // Clear any old state
        repo.resetDailySessionForTesting(testDate)

        // Scenario 1: 100 students submit successfully in Class 10
        // Scores from 110 down to 11 (Student_1 has 110, Student_100 has 11)
        for (i in 1..100) {
            val score = (111 - i).toFloat()
            val rollNo = DailyTestTimeUtils.generateTemporaryRollNo(testDate, i)
            val outcome = repo.submitDailyTestResultToTop100(
                studentId = "std_c10_$i",
                studentName = "Student $i",
                className = "Class 10",
                score = score,
                totalMarks = 120f,
                accuracy = (score / 120f) * 100f,
                completionTimeSeconds = 200L + i,
                submittedAtEpoch = 1000L + i,
                rollNo = rollNo,
                dateKey = testDate
            )
            assertTrue(outcome.qualifiedForTop100)
            assertEquals(i, outcome.retainedCount)
            assertTrue(outcome.permanentlyDeletedStudentIds.isEmpty())
        }

        val top100After100 = repo.getTop100ForClass("Class 10", testDate)
        assertEquals(100, top100After100.size)
        assertEquals("std_c10_1", top100After100.first().studentId)
        assertEquals(1, top100After100.first().rank)
        assertEquals("std_c10_100", top100After100.last().studentId)
        assertEquals(100, top100After100.last().rank)
        assertNotNull(repo.getTemporaryDailyRegistration("std_c10_100", "Class 10", testDate))

        // Scenario 3: 101st student achieves a LOWER score (score = 5, below Rank 100's score of 11)
        // Must NOT qualify and must be permanently deleted (registration, roll number, and attempt)
        val lowChallengerOutcome = repo.submitDailyTestResultToTop100(
            studentId = "std_c10_low_101",
            studentName = "Low Scorer 101",
            className = "Class 10",
            score = 5f,
            totalMarks = 120f,
            accuracy = (5f / 120f) * 100f,
            completionTimeSeconds = 180L,
            submittedAtEpoch = 2000L,
            rollNo = "2610210101",
            dateKey = testDate
        )
        assertFalse(lowChallengerOutcome.qualifiedForTop100)
        assertNull(lowChallengerOutcome.assignedRank)
        assertEquals(100, lowChallengerOutcome.retainedCount)
        assertTrue(lowChallengerOutcome.permanentlyDeletedStudentIds.contains("std_c10_low_101"))
        assertNull(repo.getTemporaryDailyRegistration("std_c10_low_101", "Class 10", testDate))
        assertFalse(repo.dailyAttempts.value.any { it.studentId == "std_c10_low_101" })

        // Scenario 2 & 4: 101st student achieves a HIGHER score (score = 95)
        // Must qualify into Top 100, and the lowest-ranked student (std_c10_100 with score 11)
        // must be permanently deleted along with their temporary registration, rollNo, and attempt.
        val highChallengerOutcome = repo.submitDailyTestResultToTop100(
            studentId = "std_c10_high_101",
            studentName = "High Scorer 101",
            className = "Class 10",
            score = 95.5f,
            totalMarks = 120f,
            accuracy = (95.5f / 120f) * 100f,
            completionTimeSeconds = 150L,
            submittedAtEpoch = 2100L,
            rollNo = "2610210102",
            dateKey = testDate
        )
        assertTrue(highChallengerOutcome.qualifiedForTop100)
        assertNotNull(highChallengerOutcome.assignedRank)
        assertEquals(100, highChallengerOutcome.retainedCount)
        assertTrue(highChallengerOutcome.permanentlyDeletedStudentIds.contains("std_c10_100"))

        val updatedTop100 = repo.getTop100ForClass("Class 10", testDate)
        assertEquals(100, updatedTop100.size)
        assertTrue(updatedTop100.any { it.studentId == "std_c10_high_101" })
        assertFalse(updatedTop100.any { it.studentId == "std_c10_100" })
        // Displaced student's temporary registration and attempt record are permanently deleted
        assertNull(repo.getTemporaryDailyRegistration("std_c10_100", "Class 10", testDate))
        assertFalse(repo.dailyAttempts.value.any { it.studentId == "std_c10_100" })
        // Qualifying student's registration remains intact
        assertNotNull(repo.getTemporaryDailyRegistration("std_c10_high_101", "Class 10", testDate))
    }

    @Test
    fun testFiveClassesMaintainIndependentTop100ListsAndSingleClassFilter() {
        val repo = AppRepository.getInstance()
        val testDate = "2026-10-21"
        repo.executeMidnightPermanentDeletion("2099-01-01")
        repo.resetDailySessionForTesting(testDate)

        val classes = listOf("Class 8", "Class 9", "Class 10", "Class 11", "Class 12")

        // Scenario 5: Populate 100 students in each of the 5 classes independently (500 total across 5 classes)
        for (cls in classes) {
            val classKey = CourseClassUtils.getCanonicalKey(cls)
            for (i in 1..100) {
                val res = repo.submitDailyTestResultToTop100(
                    studentId = "std_${classKey}_$i",
                    studentName = "$cls Student $i",
                    className = cls,
                    score = (101 - i).toFloat(),
                    totalMarks = 100f,
                    accuracy = (101 - i).toFloat(),
                    completionTimeSeconds = 200L + i,
                    submittedAtEpoch = 1000L + i,
                    rollNo = "${classKey.takeLast(2)}${String.format("%04d", i)}",
                    dateKey = testDate
                )
                assertTrue(res.qualifiedForTop100)
                assertEquals(i, res.retainedCount)
            }
        }

        // Verify each of the 5 classes has its own independent 100 entries and no cross-class leakage
        for (cls in classes) {
            val classTop100 = repo.getTop100ForClass(cls, testDate)
            assertEquals(100, classTop100.size)
            assertTrue(classTop100.all { CourseClassUtils.isClassMatch(cls, it.className) })
            assertEquals(1, classTop100.first().rank)
            assertEquals(100, classTop100.last().rank)
        }

        // Scenario 6: Verify Top 100 remains visible throughout the day & Student App exposes Top 10 via top10
        repo.switchClass("class_10")
        repo.recalculateDailyLeaderboard()
        val state = repo.dailyLeaderboardState.value
        assertEquals(100, state.top100.size)
        assertEquals(10, state.top10.size)
        assertEquals(1, state.top10.first().rank)
        assertEquals(10, state.top10.last().rank)
    }

    @Test
    fun testMidnightPermanentDeletionProtectsNewDayAndPermanentContent() {
        val repo = AppRepository.getInstance()
        val yesterdayDate = "2026-10-21"
        val newDayDate = "2026-10-22"
        repo.executeMidnightPermanentDeletion("2099-01-01")
        repo.resetDailySessionForTesting(yesterdayDate)

        // Submit yesterday's records across all 5 classes
        val classes = listOf("Class 8", "Class 9", "Class 10", "Class 11", "Class 12")
        for (cls in classes) {
            for (i in 1..15) {
                repo.submitDailyTestResultToTop100(
                    studentId = "yest_${cls}_$i",
                    studentName = "Yesterday $cls #$i",
                    className = cls,
                    score = (50 - i).toFloat(),
                    totalMarks = 50f,
                    submittedAtEpoch = 1000L + i,
                    dateKey = yesterdayDate
                )
            }
        }

        // Also submit a new-day record (12:00:05 AM IST on 2026-10-22) before cleanup runs
        repo.submitDailyTestResultToTop100(
            studentId = "newday_c10_1",
            studentName = "Fresh Day Topper",
            className = "Class 10",
            score = 49f,
            totalMarks = 50f,
            submittedAtEpoch = 5000L,
            rollNo = "2610220001",
            dateKey = newDayDate
        )

        val permanentBannersBefore = repo.promotionalBanners.value.size

        // Scenarios 7, 8, 9, 10: Execute 12:00 AM IST Midnight Permanent Deletion for newDayDate
        val cleanupSummary = repo.executeMidnightPermanentDeletion(newDayDate)

        // Scenario 8: Yesterday's temporary records across all 5 classes (5 * 15 = 75) are permanently deleted
        assertEquals(75, cleanupSummary.deletedLeaderboardEntries)
        assertEquals(75, cleanupSummary.deletedTemporaryRegistrations)
        for (cls in classes) {
            assertTrue(repo.getTop100ForClass(cls, yesterdayDate).isEmpty())
            assertNull(repo.getTemporaryDailyRegistration("yest_${cls}_1", cls, yesterdayDate))
        }
        assertFalse(repo.dailyAttempts.value.any { it.dateKey == yesterdayDate })

        // Scenario 9: New-day submissions remain 100% safe
        val newDayTop100 = repo.getTop100ForClass("Class 10", newDayDate)
        assertEquals(1, newDayTop100.size)
        assertEquals("newday_c10_1", newDayTop100.first().studentId)
        assertNotNull(repo.getTemporaryDailyRegistration("newday_c10_1", "Class 10", newDayDate))
        assertTrue(repo.dailyAttempts.value.any { it.studentId == "newday_c10_1" && it.dateKey == newDayDate })

        // Scenario 10: Permanent educational content is untouched
        assertEquals(permanentBannersBefore, repo.promotionalBanners.value.size)
    }

    @Test
    fun testTemporaryRollNoFormat() {
        val rollNo1 = DailyTestTimeUtils.generateTemporaryRollNo("2026-10-09", 1)
        assertEquals("2610090001", rollNo1)

        val rollNo2 = DailyTestTimeUtils.generateTemporaryRollNo("2026-10-09", 42)
        assertEquals("2610090042", rollNo2)

        val rollNoNextDay = DailyTestTimeUtils.generateTemporaryRollNo("2026-10-10", 1)
        assertEquals("2610100001", rollNoNextDay)
    }

    @Test
    fun testMidnightResetStartsFresh() {
        val repo = AppRepository.getInstance()
        repo.resetDailySessionForTesting("2026-10-03")

        assertEquals(0, repo.dailyAttempts.value.size)
        assertNull(repo.dailyBestResult.value)
        assertEquals("2026-10-03", repo.dailyDateKey.value)
    }

    @Test
    fun testCaptchaGenerationAndVerificationIssuesRollNo() = kotlinx.coroutines.runBlocking {
        val repo = AppRepository.getInstance()
        val challenge = repo.refreshCaptchaChallenge()

        assertTrue(challenge.challengeId.isNotBlank())
        assertEquals(6, challenge.glyphs.size)
        assertEquals(5, challenge.maxAttempts)
        assertEquals(0, challenge.attemptsUsed)
        assertTrue(!challenge.isExpired)

        val wrongResult = repo.verifyCaptchaAndGenerateRollNo("ZZZZZZ", "Ravi Kumar")
        assertTrue(!wrongResult.verified)
        assertEquals(4, wrongResult.attemptsRemaining)

        val activeChallenge = repo.captchaState.value
        val correctCode = activeChallenge.glyphs.map { it.char }.joinToString("")
        val okResult = repo.verifyCaptchaAndGenerateRollNo(correctCode, "Ravi Kumar")
        assertTrue(okResult.verified)
        assertNotNull(okResult.rollNo)
        assertTrue(okResult.rollNo!!.length >= 10)

        val replayResult = repo.verifyCaptchaAndGenerateRollNo(correctCode, "Ravi Kumar")
        assertTrue(!replayResult.verified)
    }
}
