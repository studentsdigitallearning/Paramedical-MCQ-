package com.jacboard.mcqmaster

import com.jacboard.mcqmaster.data.AppRepository
import com.jacboard.mcqmaster.model.DailyLeaderboardEntry
import com.jacboard.mcqmaster.util.DailyTestTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTestLeaderboardTest {

    @Test
    fun testLeaderboardRankingPriorityRules() {
        // Requirement 3:
        // Priority 1: Highest Score
        // Priority 2: Fastest Time
        // Priority 3: Stable ID
        val students = listOf(
            DailyLeaderboardEntry(
                rank = 0,
                studentId = "D",
                studentName = "Student D",
                className = "Class 10",
                score = 48f,
                totalMarks = 50f,
                completionTimeSeconds = 380L // 06:20
            ),
            DailyLeaderboardEntry(
                rank = 0,
                studentId = "B",
                studentName = "Student B",
                className = "Class 10",
                score = 50f,
                totalMarks = 50f,
                completionTimeSeconds = 550L // 09:10
            ),
            DailyLeaderboardEntry(
                rank = 0,
                studentId = "A",
                studentName = "Student A",
                className = "Class 10",
                score = 50f,
                totalMarks = 50f,
                completionTimeSeconds = 500L // 08:20
            ),
            DailyLeaderboardEntry(
                rank = 0,
                studentId = "C",
                studentName = "Student C",
                className = "Class 10",
                score = 49f,
                totalMarks = 50f,
                completionTimeSeconds = 450L // 07:30
            )
        )

        val sorted = students.sortedWith(
            compareByDescending<DailyLeaderboardEntry> { it.score }
                .thenBy { it.completionTimeSeconds }
                .thenBy { it.studentId }
        )

        assertEquals("Student A", sorted[0].studentName)
        assertEquals("Student B", sorted[1].studentName)
        assertEquals("Student C", sorted[2].studentName)
        assertEquals("Student D", sorted[3].studentName)
    }

    @Test
    fun testBestAttemptLogicHighestScoreAndFastestTime() {
        // Requirement 2:
        // Attempt 1: 38/50 in 12:30 (750s)
        // Attempt 2: 43/50 in 14:10 (850s) -> Best: 43/50 in 14:10
        // Attempt 3: 41/50 in 10:20 (620s) -> Best: 43/50 in 14:10 (worse attempt never replaces best)
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

        // Attempt 3: 41/50 in 620s
        val att3Score = 41f
        val att3Time = 620L
        if (att3Score > bestScore || (att3Score == bestScore && att3Time < bestTime)) {
            bestScore = att3Score
            bestTime = att3Time
        }
        // Score remains 43/50, time remains 850L
        assertEquals(43f, bestScore, 0.01f)
        assertEquals(850L, bestTime)

        // Attempt 4: Equal score 43/50 in faster time 10:15 (615s)
        val att4Score = 43f
        val att4Time = 615L
        if (att4Score > bestScore || (att4Score == bestScore && att4Time < bestTime)) {
            bestScore = att4Score
            bestTime = att4Time
        }
        // Now best time improves to 615s!
        assertEquals(43f, bestScore, 0.01f)
        assertEquals(615L, bestTime)
    }

    @Test
    fun testTop100LimitInRepository() {
        val repo = AppRepository.getInstance()
        val dailyState = repo.dailyLeaderboardState.value

        // Requirement 4: Top 100 only
        assertTrue(dailyState.top100.size <= 100)
    }

    @Test
    fun testTemporaryRollNoFormat() {
        // Requirement 1 & 2: Temporary Roll Number in format "YYMMDDNNNN"
        // Example: "2610090001"
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

        // Simulate midnight reset to a new date
        repo.resetDailySessionForTesting("2026-10-03")

        // Attempts and user best result must be fresh
        assertEquals(0, repo.dailyAttempts.value.size)
        assertNull(repo.dailyBestResult.value)
        assertEquals("2026-10-03", repo.dailyDateKey.value)
    }
}
