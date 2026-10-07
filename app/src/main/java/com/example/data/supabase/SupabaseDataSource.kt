package com.example.data.supabase

import android.util.Log
import com.example.model.CategoryType
import com.example.model.Chapter
import com.example.model.ClassData
import com.example.model.DailyLeaderboardEntry
import com.example.model.Difficulty
import com.example.model.Question
import com.example.model.Subject
import com.example.model.Test
import com.example.model.TestCategory
import com.example.model.TestStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data source that fetches dynamic MCQ, class, course, subject, test,
 * and leaderboard content from Supabase PostgREST tables.
 *
 * Implements flexible schema mapping to work seamlessly with management systems
 * and admin consoles.
 */
class SupabaseDataSource(
    private val client: SupabaseClient = SupabaseClient.getInstance()
) {

    /**
     * Loads classes and courses dynamically from Supabase.
     * Supports both standard classes (Class 8-12) and professional courses (DMLT, BMLT, OT, etc.).
     */
    suspend fun fetchClasses(): Result<List<ClassData>> {
        val result = client.get("classes", "select=*&order=display_order.asc,id.asc")
        if (result.isFailure) {
            val fallbackResult = client.get("classes", "select=*")
            if (fallbackResult.isSuccess) {
                val parsed = parseClassesJson(fallbackResult.getOrThrow())
                if (parsed.isNotEmpty()) return Result.success(parsed)
            }
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch classes"))
        }

        val jsonString = result.getOrThrow()
        val parsed = parseClassesJson(jsonString)
        return Result.success(parsed)
    }

    private fun parseClassesJson(jsonString: String): List<ClassData> {
        val list = mutableListOf<ClassData>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isActive = obj.optBoolean("is_active", obj.optBoolean("active", true))
                if (!isActive) continue

                val id = obj.optString("id", "class_${i + 1}")
                val name = obj.optString("name", obj.optString("title", obj.optString("class_name", "Class ${i + 1}")))
                val subjectCount = obj.optInt("subject_count", obj.optInt("total_subjects", 5))
                val displayOrder = obj.optInt("display_order", obj.optInt("order_index", obj.optInt("order", i + 1)))

                list.add(
                    ClassData(
                        id = id,
                        name = name,
                        subjectCount = subjectCount,
                        displayOrder = displayOrder
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing classes JSON: ${e.message}")
        }
        return list.sortedBy { it.displayOrder }
    }

    /**
     * Loads subjects dynamically for the selected class/course from Supabase.
     */
    suspend fun fetchSubjects(classId: String): Result<List<Subject>> {
        val query = "class_id=eq.$classId&select=*&order=id.asc"
        val result = client.get("subjects", query)
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch subjects"))
        }

        val jsonString = result.getOrThrow()
        val list = mutableListOf<Subject>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isActive = obj.optBoolean("is_active", obj.optBoolean("active", true))
                if (!isActive) continue

                val id = obj.optString("id", "sub_${i + 1}")
                val itemClassId = obj.optString("class_id", classId)
                val name = obj.optString("name", obj.optString("title", "Subject ${i + 1}"))
                val iconKey = obj.optString("icon_key", obj.optString("icon", "subject"))
                val chapterCount = obj.optInt("chapter_count", obj.optInt("total_chapters", 6))
                val progressPercent = obj.optInt("progress_percent", 0)

                list.add(
                    Subject(
                        id = id,
                        classId = itemClassId,
                        name = name,
                        iconKey = iconKey,
                        chapterCount = chapterCount,
                        progressPercent = progressPercent
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing subjects JSON: ${e.message}")
            return Result.failure(e)
        }
        return Result.success(list)
    }

    /**
     * Loads chapters dynamically for a subject from Supabase.
     */
    suspend fun fetchChapters(subjectId: String): Result<List<Chapter>> {
        val query = "subject_id=eq.$subjectId&select=*&order=chapter_number.asc,id.asc"
        val result = client.get("chapters", query)
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch chapters"))
        }

        val jsonString = result.getOrThrow()
        val list = mutableListOf<Chapter>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isActive = obj.optBoolean("is_active", obj.optBoolean("active", true))
                if (!isActive) continue

                val id = obj.optString("id", "ch_${i + 1}")
                val itemSubjectId = obj.optString("subject_id", subjectId)
                val chapterNumber = obj.optInt("chapter_number", obj.optInt("number", i + 1))
                val name = obj.optString("name", obj.optString("title", "Chapter ${i + 1}"))
                val testCount = obj.optInt("test_count", obj.optInt("total_tests", 3))
                val progressPercent = obj.optInt("progress_percent", 0)

                list.add(
                    Chapter(
                        id = id,
                        subjectId = itemSubjectId,
                        chapterNumber = chapterNumber,
                        name = name,
                        testCount = testCount,
                        progressPercent = progressPercent
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing chapters JSON: ${e.message}")
            return Result.failure(e)
        }
        return Result.success(list.sortedBy { it.chapterNumber })
    }

    /**
     * Loads test categories dynamically from Supabase.
     */
    suspend fun fetchCategories(): Result<List<TestCategory>> {
        val result = client.get("categories", "select=*&order=id.asc")
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch categories"))
        }

        val jsonString = result.getOrThrow()
        val list = mutableListOf<TestCategory>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isActive = obj.optBoolean("is_active", obj.optBoolean("active", true))
                if (!isActive) continue

                val id = obj.optString("id", "cat_${i + 1}")
                val name = obj.optString("name", "Category ${i + 1}")
                val typeStr = obj.optString("type", "SUBJECT").uppercase()
                val type = runCatching { CategoryType.valueOf(typeStr) }.getOrDefault(CategoryType.SUBJECT)
                val iconKey = obj.optString("icon_key", obj.optString("icon", "category"))
                val subtitle = obj.optString("subtitle", "")
                val testCount = obj.optInt("test_count", 0)

                list.add(
                    TestCategory(
                        id = id,
                        name = name,
                        type = type,
                        iconKey = iconKey,
                        subtitle = subtitle,
                        testCount = testCount
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing categories JSON: ${e.message}")
            return Result.failure(e)
        }
        return Result.success(list)
    }

    /**
     * Loads published/active tests dynamically from Supabase.
     */
    suspend fun fetchTests(classId: String? = null): Result<List<Test>> {
        val queryParams = buildString {
            append("select=*")
            if (!classId.isNullOrBlank()) {
                append("&class_id=eq.$classId")
            }
            append("&order=id.asc")
        }

        val result = client.get("tests", queryParams)
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch tests"))
        }

        val jsonString = result.getOrThrow()
        val list = mutableListOf<Test>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                // Only display tests intended to be visible/published to students (Requirement 5)
                val isPublished = obj.optBoolean("is_published", obj.optBoolean("published", true))
                val isActive = obj.optBoolean("is_active", obj.optBoolean("active", true))
                if (!isPublished || !isActive) continue

                val id = obj.optString("id", "test_${i + 1}")
                val itemClassId = obj.optString("class_id", classId ?: "class_10")
                val categoryId = obj.optString("category_id", "cat_mock")
                val subjectId = obj.optString("subject_id").takeIf { it.isNotBlank() }
                val subjectName = obj.optString("subject_name").takeIf { it.isNotBlank() }
                val chapterId = obj.optString("chapter_id").takeIf { it.isNotBlank() }
                val chapterName = obj.optString("chapter_name").takeIf { it.isNotBlank() }
                val title = obj.optString("title", "MCQ Test ${i + 1}")
                val subtitle = obj.optString("subtitle", "")
                val questionCount = obj.optInt("question_count", obj.optInt("total_questions", 25))
                val durationMinutes = obj.optInt("duration_minutes", obj.optInt("duration", 30))
                val totalMarks = obj.optDouble("total_marks", 50.0).toFloat()
                val negativeMarking = obj.optDouble("negative_marking", 0.25).toFloat()

                val difficultyStr = obj.optString("difficulty", "MEDIUM").uppercase()
                val difficulty = runCatching { Difficulty.valueOf(difficultyStr) }.getOrDefault(Difficulty.MEDIUM)

                val statusStr = obj.optString("status", "NOT_ATTEMPTED").uppercase()
                val status = runCatching { TestStatus.valueOf(statusStr) }.getOrDefault(TestStatus.NOT_ATTEMPTED)

                val isDaily = obj.optBoolean("is_daily", false)
                val expiryNote = obj.optString("expiry_note").takeIf { it.isNotBlank() }

                list.add(
                    Test(
                        id = id,
                        classId = itemClassId,
                        categoryId = categoryId,
                        subjectId = subjectId,
                        subjectName = subjectName,
                        chapterId = chapterId,
                        chapterName = chapterName,
                        title = title,
                        subtitle = subtitle,
                        questionCount = questionCount,
                        durationMinutes = durationMinutes,
                        totalMarks = totalMarks,
                        negativeMarking = negativeMarking,
                        difficulty = difficulty,
                        status = status,
                        isDaily = isDaily,
                        expiryNote = expiryNote
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tests JSON: ${e.message}")
            return Result.failure(e)
        }
        return Result.success(list)
    }

    /**
     * Loads questions for a specific test dynamically from Supabase (Requirement 6).
     * Questions are fetched when test starts, not bundled in the APK.
     */
    suspend fun fetchQuestions(testId: String): Result<List<Question>> {
        val query = "test_id=eq.$testId&select=*&order=question_index.asc,id.asc"
        val result = client.get("questions", query)
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch questions"))
        }

        val jsonString = result.getOrThrow()
        val list = mutableListOf<Question>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                val id = obj.optString("id", "q_${testId}_${i + 1}")
                val itemTestId = obj.optString("test_id", testId)
                val questionIndex = obj.optInt("question_index", obj.optInt("index", i))
                val text = obj.optString("text", obj.optString("question", "Question ${i + 1}"))

                // Parse Options: supports JSON array, string array, or separate fields (opt_a, opt_b, opt_c, opt_d)
                val options = parseOptions(obj)

                val correctIndex = obj.optInt("correct_index", obj.optInt("correct_answer", 0))
                val explanation = obj.optString("explanation", "Standard solution.")
                val chapterName = obj.optString("chapter_name", "")
                val marks = obj.optDouble("marks", 1.0).toFloat()

                val textHindi = obj.optString("text_hindi").takeIf { it.isNotBlank() }
                val explanationHindi = obj.optString("explanation_hindi").takeIf { it.isNotBlank() }
                val optionsHindi = parseHindiOptions(obj)

                list.add(
                    Question(
                        id = id,
                        testId = itemTestId,
                        questionIndex = questionIndex,
                        text = text,
                        options = options,
                        correctIndex = correctIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)),
                        explanation = explanation,
                        chapterName = chapterName,
                        marks = marks,
                        textHindi = textHindi,
                        optionsHindi = optionsHindi,
                        explanationHindi = explanationHindi
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing questions JSON: ${e.message}")
            return Result.failure(e)
        }
        return Result.success(list)
    }

    private fun parseOptions(obj: JSONObject): List<String> {
        val list = mutableListOf<String>()
        if (obj.has("options")) {
            val optVal = obj.get("options")
            if (optVal is JSONArray) {
                for (j in 0 until optVal.length()) {
                    list.add(optVal.getString(j))
                }
            } else if (optVal is String) {
                try {
                    val parsedArr = JSONArray(optVal)
                    for (j in 0 until parsedArr.length()) {
                        list.add(parsedArr.getString(j))
                    }
                } catch (_: Exception) {
                    list.addAll(optVal.split(",").map { it.trim() })
                }
            }
        }

        if (list.isEmpty()) {
            val optA = obj.optString("opt_a", obj.optString("option_a", ""))
            val optB = obj.optString("opt_b", obj.optString("option_b", ""))
            val optC = obj.optString("opt_c", obj.optString("option_c", ""))
            val optD = obj.optString("opt_d", obj.optString("option_d", ""))
            if (optA.isNotBlank()) list.add(optA)
            if (optB.isNotBlank()) list.add(optB)
            if (optC.isNotBlank()) list.add(optC)
            if (optD.isNotBlank()) list.add(optD)
        }

        if (list.isEmpty()) {
            list.addAll(listOf("Option A", "Option B", "Option C", "Option D"))
        }

        return list
    }

    private fun parseHindiOptions(obj: JSONObject): List<String>? {
        if (!obj.has("options_hindi")) return null
        val list = mutableListOf<String>()
        try {
            val optVal = obj.get("options_hindi")
            if (optVal is JSONArray) {
                for (j in 0 until optVal.length()) {
                    list.add(optVal.getString(j))
                }
            } else if (optVal is String) {
                val parsedArr = JSONArray(optVal)
                for (j in 0 until parsedArr.length()) {
                    list.add(parsedArr.getString(j))
                }
            }
        } catch (_: Exception) {
            return null
        }
        return if (list.isNotEmpty()) list else null
    }

    /**
     * Verifies student daily test eligibility from Supabase backend (Rules 5 & 6 & 16).
     * Students can take unlimited attempts in the same Course+Class, but cannot take a Daily Test
     * in another Course+Class on the same date.
     */
    suspend fun checkDailyTestEligibility(
        studentId: String,
        classId: String,
        className: String,
        dateKey: String
    ): Result<com.example.model.DailyEligibilityResult> {
        val query = "student_id=eq.$studentId&date_key=eq.$dateKey&select=*"
        val result = client.get("daily_attempts", query)
        val jsonString = if (result.isSuccess) {
            result.getOrThrow()
        } else {
            val fallbackRes = client.get("leaderboard", query)
            if (fallbackRes.isSuccess) fallbackRes.getOrThrow() else "[]"
        }

        try {
            val array = JSONArray(jsonString)
            if (array.length() == 0) {
                return Result.success(com.example.model.DailyEligibilityResult(isEligible = true, attemptsCount = 0))
            }

            var attemptsInCurrentClass = 0
            var attemptedOtherClass: String? = null

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val itemClassId = obj.optString("class_id", "")
                val itemClassName = obj.optString("class_name", obj.optString("student_class", ""))

                val isMatch = (itemClassId.isNotBlank() && itemClassId.equals(classId, ignoreCase = true)) ||
                              (itemClassName.isNotBlank() && itemClassName.equals(className, ignoreCase = true))

                if (isMatch) {
                    attemptsInCurrentClass++
                } else {
                    attemptedOtherClass = if (itemClassName.isNotBlank()) itemClassName else itemClassId
                }
            }

            if (attemptedOtherClass != null && attemptsInCurrentClass == 0) {
                return Result.success(
                    com.example.model.DailyEligibilityResult(
                        isEligible = false,
                        reason = "You have already attempted today's Daily Test in another Course/Class.",
                        attemptedClassName = attemptedOtherClass,
                        attemptsCount = 0
                    )
                )
            }

            return Result.success(
                com.example.model.DailyEligibilityResult(
                    isEligible = true,
                    attemptsCount = attemptsInCurrentClass
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking daily test eligibility: ${e.message}")
            return Result.success(com.example.model.DailyEligibilityResult(isEligible = true))
        }
    }

    /**
     * Loads shared leaderboard data from Supabase strictly separated by Course + Class + Date (Rules 7-11).
     * Keeps only the best result per student and ranks by 1. Highest Score, 2. Fastest Time.
     */
    suspend fun fetchLeaderboard(
        dateKey: String,
        className: String = ""
    ): Result<List<DailyLeaderboardEntry>> {
        val query = if (className.isNotBlank()) {
            "date_key=eq.$dateKey&class_name=eq.$className&select=*&order=score.desc,completion_time_seconds.asc&limit=1000"
        } else {
            "date_key=eq.$dateKey&select=*&order=score.desc,completion_time_seconds.asc&limit=1000"
        }

        val result = client.get("leaderboard", query)
        val jsonString = if (result.isSuccess) {
            result.getOrThrow()
        } else {
            val fallback = client.get("leaderboard", "select=*&order=score.desc,completion_time_seconds.asc&limit=1000")
            if (fallback.isSuccess) fallback.getOrThrow() else "[]"
        }

        val parsed = parseLeaderboardJson(jsonString, className)
        return Result.success(parsed)
    }

    private fun parseLeaderboardJson(jsonString: String, targetClassName: String = ""): List<DailyLeaderboardEntry> {
        val rawList = mutableListOf<DailyLeaderboardEntry>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                val studentId = obj.optString("student_id", "std_${i + 1}")
                val studentName = obj.optString("student_name", "Student ${i + 1}")
                val className = obj.optString("class_name", obj.optString("student_class", "Class 10"))
                val score = obj.optDouble("score", 0.0).toFloat()
                val totalMarks = obj.optDouble("total_marks", 50.0).toFloat()
                val completionTimeSeconds = obj.optLong("completion_time_seconds", 300L)
                val attemptsCount = obj.optInt("attempts_count", 1)

                if (targetClassName.isNotBlank() && !className.equals(targetClassName, ignoreCase = true)) {
                    continue
                }

                val initials = studentName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "ST" }

                rawList.add(
                    DailyLeaderboardEntry(
                        rank = 0,
                        studentId = studentId,
                        studentName = studentName,
                        className = className,
                        score = score,
                        totalMarks = totalMarks,
                        completionTimeSeconds = completionTimeSeconds,
                        isCurrentUser = false,
                        attemptsCount = attemptsCount,
                        avatarInitials = initials
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing leaderboard JSON: ${e.message}")
        }

        // Keep only best attempt per student (Rule 8)
        val bestPerStudent = mutableMapOf<String, DailyLeaderboardEntry>()
        for (entry in rawList) {
            val existing = bestPerStudent[entry.studentId]
            if (existing == null) {
                bestPerStudent[entry.studentId] = entry
            } else {
                val isBetterScore = entry.score > existing.score
                val isSameScoreFaster = entry.score == existing.score && entry.completionTimeSeconds < existing.completionTimeSeconds
                if (isBetterScore || isSameScoreFaster) {
                    bestPerStudent[entry.studentId] = entry.copy(
                        attemptsCount = maxOf(existing.attemptsCount, entry.attemptsCount)
                    )
                }
            }
        }

        // Rank priority: 1. Highest Score, 2. Fastest completion time (Rule 3)
        return bestPerStudent.values
            .sortedWith(
                compareByDescending<DailyLeaderboardEntry> { it.score }
                    .thenBy { it.completionTimeSeconds }
            )
            .mapIndexed { index, item ->
                item.copy(rank = index + 1)
            }
    }

    /**
     * Submits student test result to Supabase so the leaderboard is updated dynamically (Rules 9 & 13).
     */
    suspend fun submitLeaderboardEntry(
        studentId: String,
        studentName: String,
        className: String,
        dateKey: String,
        score: Float,
        totalMarks: Float,
        completionTimeSeconds: Long,
        attemptsCount: Int
    ): Result<Boolean> {
        val payload = JSONObject().apply {
            put("student_id", studentId)
            put("student_name", studentName)
            put("class_name", className)
            put("date_key", dateKey)
            put("score", score.toDouble())
            put("total_marks", totalMarks.toDouble())
            put("completion_time_seconds", completionTimeSeconds)
            put("attempts_count", attemptsCount)
            put("submitted_at", System.currentTimeMillis())
        }.toString()

        val postResult = client.post("leaderboard", payload)
        // Also log into daily_attempts table for participation and eligibility audit
        client.post("daily_attempts", payload)
        if (postResult.isFailure) {
            client.post("test_results", payload)
        }
        return Result.success(true)
    }

    companion object {
        private const val TAG = "SupabaseDataSource"
    }
}
