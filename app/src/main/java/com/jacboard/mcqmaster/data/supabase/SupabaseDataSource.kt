package com.jacboard.mcqmaster.data.supabase

import android.util.Log
import com.jacboard.mcqmaster.model.BannerActionType
import com.jacboard.mcqmaster.model.CaptchaChallengeUiState
import com.jacboard.mcqmaster.model.CaptchaGlyph
import com.jacboard.mcqmaster.model.CaptchaVerificationResult
import com.jacboard.mcqmaster.model.CategoryType
import com.jacboard.mcqmaster.model.Chapter
import com.jacboard.mcqmaster.model.ClassData
import com.jacboard.mcqmaster.model.DailyLeaderboardEntry
import com.jacboard.mcqmaster.model.Difficulty
import com.jacboard.mcqmaster.model.Question
import com.jacboard.mcqmaster.model.PromotionalBanner
import com.jacboard.mcqmaster.model.Subject
import com.jacboard.mcqmaster.model.Test
import com.jacboard.mcqmaster.model.NotificationItem
import com.jacboard.mcqmaster.model.TestCategory
import com.jacboard.mcqmaster.model.TestStatus
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
                val raw = fallbackResult.getOrThrow()
                com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("classes", raw)
                val parsed = parseClassesJson(raw)
                if (parsed.isNotEmpty()) return Result.success(parsed)
            }
            val cached = loadCachedClasses()
            if (cached.isNotEmpty()) return Result.success(cached)
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch classes"))
        }

        val jsonString = result.getOrThrow()
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("classes", jsonString)
        val parsed = parseClassesJson(jsonString)
        return Result.success(parsed)
    }

    fun loadCachedClasses(): List<ClassData> {
        val raw = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getContentCache("classes") ?: return emptyList()
        return parseClassesJson(raw)
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
            val cached = loadCachedSubjects(classId)
            if (cached.isNotEmpty()) return Result.success(cached)
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch subjects"))
        }

        val jsonString = result.getOrThrow()
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("subjects_$classId", jsonString)
        return Result.success(parseSubjectsJson(jsonString, classId))
    }

    fun loadCachedSubjects(classId: String): List<Subject> {
        if (classId.isBlank()) return emptyList()
        val raw = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getContentCache("subjects_$classId") ?: return emptyList()
        return parseSubjectsJson(raw, classId)
    }

    private fun parseSubjectsJson(jsonString: String, classId: String): List<Subject> {
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
        }
        return list
    }

    /**
     * Loads chapters dynamically for a subject from Supabase.
     */
    suspend fun fetchChapters(subjectId: String): Result<List<Chapter>> {
        val query = "subject_id=eq.$subjectId&select=*&order=chapter_number.asc,id.asc"
        val result = client.get("chapters", query)
        if (result.isFailure) {
            val cached = loadCachedChapters(subjectId)
            if (cached.isNotEmpty()) return Result.success(cached)
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch chapters"))
        }

        val jsonString = result.getOrThrow()
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("chapters_$subjectId", jsonString)
        return Result.success(parseChaptersJson(jsonString, subjectId))
    }

    fun loadCachedChapters(subjectId: String): List<Chapter> {
        if (subjectId.isBlank()) return emptyList()
        val raw = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getContentCache("chapters_$subjectId") ?: return emptyList()
        return parseChaptersJson(raw, subjectId)
    }

    private fun parseChaptersJson(jsonString: String, subjectId: String): List<Chapter> {
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
        }
        return list.sortedBy { it.chapterNumber }
    }

    /**
     * Loads test categories dynamically from Supabase.
     */
    suspend fun fetchCategories(): Result<List<TestCategory>> {
        val result = client.get("categories", "select=*&order=id.asc")
        if (result.isFailure) {
            val cached = loadCachedCategories()
            if (cached.isNotEmpty()) return Result.success(cached)
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch categories"))
        }

        val jsonString = result.getOrThrow()
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("categories", jsonString)
        return Result.success(parseCategoriesJson(jsonString))
    }

    fun loadCachedCategories(): List<TestCategory> {
        val raw = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getContentCache("categories") ?: return emptyList()
        return parseCategoriesJson(raw)
    }

    private fun parseCategoriesJson(jsonString: String): List<TestCategory> {
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
        }
        return list
    }

    /**
     * Loads published/active tests dynamically from Supabase.
     */
    suspend fun fetchTests(classId: String? = null): Result<List<Test>> {
        val cacheKey = "tests_${classId ?: "all"}"
        val queryParams = buildString {
            append("select=*")
            if (!classId.isNullOrBlank()) {
                append("&class_id=eq.$classId")
            }
            append("&order=id.asc")
        }

        val result = client.get("tests", queryParams)
        if (result.isFailure) {
            val cached = loadCachedTests(classId)
            if (cached.isNotEmpty()) return Result.success(cached)
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch tests"))
        }

        val jsonString = result.getOrThrow()
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache(cacheKey, jsonString)
        return Result.success(parseTestsJson(jsonString, classId))
    }

    fun loadCachedTests(classId: String? = null): List<Test> {
        val cacheKey = "tests_${classId ?: "all"}"
        val raw = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getContentCache(cacheKey) ?: return emptyList()
        return parseTestsJson(raw, classId)
    }

    private fun parseTestsJson(jsonString: String, classId: String?): List<Test> {
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
                val hasNegFlag = obj.optBoolean("has_negative_marking", obj.optBoolean("is_negative_marking", true))
                val rawNegVal = obj.opt("negative_marking")
                val negativeMarking = when {
                    !hasNegFlag -> 0f
                    rawNegVal == null || rawNegVal == JSONObject.NULL -> 0f
                    rawNegVal is Boolean -> if (rawNegVal) 0.25f else 0f
                    rawNegVal is Number -> kotlin.math.abs(rawNegVal.toFloat()).let { if (it <= 0.0001f) 0f else it }
                    rawNegVal is String -> {
                        val s = rawNegVal.trim().lowercase()
                        if (s.isEmpty() || s == "false" || s == "no" || s == "none" || s == "null" || s == "0" || s == "0.0" || s == "-0" || s == "-0.0") {
                            0f
                        } else {
                            s.removePrefix("-").toFloatOrNull()?.let { if (it <= 0.0001f) 0f else it } ?: 0f
                        }
                    }
                    else -> 0f
                }

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
        }
        return list
    }

    /**
     * Loads questions for a specific test dynamically from Supabase (Requirement 6).
     * Questions are fetched when test starts and cached locally for fast offline/repeat access.
     */
    suspend fun fetchQuestions(testId: String): Result<List<Question>> {
        val query = "test_id=eq.$testId&select=*&order=question_index.asc,id.asc"
        val result = client.get("questions", query)
        if (result.isFailure) {
            val cached = loadCachedQuestions(testId)
            if (cached.isNotEmpty()) return Result.success(cached)
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch questions"))
        }

        val jsonString = result.getOrThrow()
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("questions_$testId", jsonString)
        return Result.success(parseQuestionsJson(jsonString, testId))
    }

    fun loadCachedQuestions(testId: String): List<Question> {
        if (testId.isBlank()) return emptyList()
        val raw = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getContentCache("questions_$testId") ?: return emptyList()
        return parseQuestionsJson(raw, testId)
    }

    private fun parseQuestionsJson(jsonString: String, testId: String): List<Question> {
        val list = mutableListOf<Question>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                val id = obj.optString("id", "q_${testId}_${i + 1}")
                val itemTestId = obj.optString("test_id", testId)
                val questionIndex = obj.optInt("question_index", obj.optInt("index", i))
                var rawText = obj.optString("text", obj.optString("question", "Question ${i + 1}"))

                // Extract optional image URL from column or [IMG:...] tag in text
                var imageUrl = obj.optString(
                    "image_url",
                    obj.optString("imageUrl", obj.optString("image", obj.optString("diagram_url", "")))
                ).trim().takeIf { it.isNotBlank() && it != "null" }

                val imgTagRegex = Regex("""\[IMG:([^\]]+)]""", RegexOption.IGNORE_CASE)
                val imgMatch = imgTagRegex.find(rawText)
                if (imgMatch != null) {
                    if (imageUrl.isNullOrBlank()) {
                        imageUrl = imgMatch.groupValues[1].trim()
                    }
                    rawText = rawText.replace(imgMatch.value, "").trim()
                }

                // Parse Options: supports JSON array, string array, or separate fields (opt_a, opt_b, opt_c, opt_d)
                val options = parseOptions(obj)

                val correctIndex = obj.optInt("correct_index", obj.optInt("correct_answer", 0))
                val explanation = obj.optString("explanation", "Standard solution.")
                val chapterName = obj.optString("chapter_name", "")
                val marks = obj.optDouble("marks", 1.0).toFloat()

                val textHindi = (obj.optString("text_hindi", "").ifBlank {
                    obj.optString("question_hindi", "").ifBlank {
                        obj.optString("text_hi", "").ifBlank {
                            obj.optString("question_hi", "").ifBlank {
                                obj.optString("hindi_text", "").ifBlank {
                                    obj.optString("hindi_question", "")
                                }
                            }
                        }
                    }
                }).takeIf { it.isNotBlank() }

                val explanationHindi = (obj.optString("explanation_hindi", "").ifBlank {
                    obj.optString("explanation_hi", "").ifBlank {
                        obj.optString("hindi_explanation", "")
                    }
                }).takeIf { it.isNotBlank() }

                val optionsHindi = parseHindiOptions(obj)

                list.add(
                    Question(
                        id = id,
                        testId = itemTestId,
                        questionIndex = questionIndex,
                        text = rawText,
                        options = options,
                        correctIndex = correctIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)),
                        explanation = explanation,
                        chapterName = chapterName,
                        marks = marks,
                        textHindi = textHindi,
                        optionsHindi = optionsHindi,
                        explanationHindi = explanationHindi,
                        imageUrl = imageUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing questions JSON: ${e.message}")
        }
        return list
    }

    suspend fun saveOrUpdateTest(test: Test): Result<Test> {
        if (!SupabaseConfig.isConfigured) return Result.success(test)
        val json = JSONObject().apply {
            put("id", test.id)
            put("title", test.title)
            put("class_id", test.classId)
            put("subject_id", test.subjectId)
            put("chapter_id", test.chapterId)
            put("duration_minutes", test.durationMinutes)
            put("total_marks", test.totalMarks)
        }
        val res = client.post("tests", json.toString())
        return if (res.isSuccess) Result.success(test) else Result.failure(res.exceptionOrNull() ?: Exception("Failed to save test"))
    }

    suspend fun deleteTest(testId: String): Result<Boolean> {
        if (!SupabaseConfig.isConfigured) return Result.success(true)
        val res = client.delete("tests", "id=eq.$testId")
        return Result.success(res.isSuccess)
    }

    suspend fun saveOrUpdateQuestion(question: Question): Result<Question> {
        if (!SupabaseConfig.isConfigured) return Result.success(question)
        val json = JSONObject().apply {
            put("id", question.id)
            put("test_id", question.testId)
            put("question_text", question.text)
            put("question_hindi", question.textHindi ?: "")
            put("options", JSONArray(question.options))
            put("correct_answer", question.correctIndex)
            put("explanation", question.explanation)
        }
        val res = client.post("questions", json.toString())
        return if (res.isSuccess) Result.success(question) else Result.failure(res.exceptionOrNull() ?: Exception("Failed to save question"))
    }

    suspend fun deleteQuestion(questionId: String): Result<Boolean> {
        if (!SupabaseConfig.isConfigured) return Result.success(true)
        val res = client.delete("questions", "id=eq.$questionId")
        return Result.success(res.isSuccess)
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
        val list = mutableListOf<String>()
        val keys = listOf("options_hindi", "options_hi", "hindi_options")
        for (k in keys) {
            if (obj.has(k)) {
                try {
                    val optVal = obj.get(k)
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
                    // Try comma split
                    val optStr = obj.optString(k, "")
                    if (optStr.isNotBlank()) {
                        list.addAll(optStr.split(",").map { it.trim() })
                    }
                }
                if (list.isNotEmpty()) return list
            }
        }

        // Check individual columns
        val optA = obj.optString("opt_a_hindi", obj.optString("opt_a_hi", obj.optString("option_a_hindi", obj.optString("option_a_hi", ""))))
        val optB = obj.optString("opt_b_hindi", obj.optString("opt_b_hi", obj.optString("option_b_hindi", obj.optString("option_b_hi", ""))))
        val optC = obj.optString("opt_c_hindi", obj.optString("opt_c_hi", obj.optString("option_c_hindi", obj.optString("option_c_hi", ""))))
        val optD = obj.optString("opt_d_hindi", obj.optString("opt_d_hi", obj.optString("option_d_hindi", obj.optString("option_d_hi", ""))))
        if (optA.isNotBlank() || optB.isNotBlank() || optC.isNotBlank() || optD.isNotBlank()) {
            if (optA.isNotBlank()) list.add(optA)
            if (optB.isNotBlank()) list.add(optB)
            if (optC.isNotBlank()) list.add(optC)
            if (optD.isNotBlank()) list.add(optD)
            return list
        }

        return null
    }

    /**
     * Obtains or registers a temporary daily test roll number from Supabase backend.
     * Format: "YYMMDDNNNN", e.g. "2610090001".
     * If the student already attempted or registered today across any course, returns the existing record.
     */
    suspend fun getOrCreateDailyRollNo(
        studentId: String,
        studentName: String,
        courseName: String,
        className: String,
        dateKey: String
    ): Result<com.jacboard.mcqmaster.model.DailyRollNoResult> {
        val safeStudentId = studentId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        val safeDateKey = dateKey.replace(Regex("[^0-9\\-]"), "")

        // 1. First, check if this student ALREADY completed a test today in daily_participants
        val existingQuery = "student_id=eq.$safeStudentId&date_key=eq.$safeDateKey&has_attempted=eq.true&select=*"
        val checkRes = client.get("daily_participants", existingQuery)
        if (checkRes.isSuccess) {
            try {
                val array = JSONArray(checkRes.getOrThrow())
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val rollNo = obj.optString("roll_no", "")
                    val existingCourse = obj.optString("course_name", courseName)
                    val existingClass = obj.optString("class_name", className)
                    val score = obj.optDouble("score", 0.0).toFloat()
                    val totalMarks = obj.optDouble("total_marks", 50.0).toFloat()
                    if (rollNo.isNotBlank()) {
                        return Result.success(
                            com.jacboard.mcqmaster.model.DailyRollNoResult(
                                rollNo = rollNo,
                                alreadyAttempted = true,
                                courseName = existingCourse,
                                className = existingClass,
                                score = score,
                                totalMarks = totalMarks,
                                isNew = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking existing daily participant: ${e.message}")
            }
        }

        // 2. Also check if already submitted in leaderboard
        val leadQuery = "student_id=eq.$safeStudentId&date_key=eq.$safeDateKey&select=*"
        val leadRes = client.get("leaderboard", leadQuery)
        if (leadRes.isSuccess) {
            try {
                val array = JSONArray(leadRes.getOrThrow())
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val rollNo = obj.optString("roll_no", "")
                    val existingClass = obj.optString("class_name", className)
                    val score = obj.optDouble("score", 0.0).toFloat()
                    val totalMarks = obj.optDouble("total_marks", 50.0).toFloat()
                    if (rollNo.isNotBlank()) {
                        return Result.success(
                            com.jacboard.mcqmaster.model.DailyRollNoResult(
                                rollNo = rollNo,
                                alreadyAttempted = true,
                                courseName = courseName,
                                className = existingClass,
                                score = score,
                                totalMarks = totalMarks,
                                isNew = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking leaderboard for roll no: ${e.message}")
            }
        }

        // 3. Robust Sequence Calculation from existing records (Do NOT insert unattempted rows to DB!)
        var maxSeq = 0
        try {
            val partRes = client.get("daily_participants", "date_key=eq.$dateKey&select=roll_no&order=roll_no.desc&limit=50")
            if (partRes.isSuccess) {
                val arr = JSONArray(partRes.getOrThrow())
                for (k in 0 until arr.length()) {
                    val r = arr.getJSONObject(k).optString("roll_no", "")
                    if (r.length >= 10) {
                        val seqPart = r.takeLast(4).toIntOrNull() ?: 0
                        if (seqPart > maxSeq) maxSeq = seqPart
                    }
                }
            }
            val leadScanRes = client.get("leaderboard", "date_key=eq.$dateKey&select=roll_no&order=roll_no.desc&limit=50")
            if (leadScanRes.isSuccess) {
                val arr = JSONArray(leadScanRes.getOrThrow())
                for (k in 0 until arr.length()) {
                    val r = arr.getJSONObject(k).optString("roll_no", "")
                    if (r.length >= 10) {
                        val seqPart = r.takeLast(4).toIntOrNull() ?: 0
                        if (seqPart > maxSeq) maxSeq = seqPart
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error determining maxSeq fallback: ${e.message}")
        }

        val candidateSeq = maxSeq + 1
        val finalRollNo = com.jacboard.mcqmaster.util.DailyTestTimeUtils.generateTemporaryRollNo(dateKey, candidateSeq)

        // Return candidate roll number locally without writing dummy unattempted rows to database
        return Result.success(
            com.jacboard.mcqmaster.model.DailyRollNoResult(
                rollNo = finalRollNo,
                alreadyAttempted = false,
                courseName = courseName,
                className = className,
                score = 0f,
                totalMarks = 50f,
                isNew = true
            )
        )
    }

    /**
     * Internal server-layer cryptographic CAPTCHA record.
     * Stores ONLY the salted SHA-256 hash (`codeHash`) and salt (`salt`) — never plaintext answers.
     */
    private data class ServerCaptchaChallengeRecord(
        val challengeId: String,
        val studentId: String,
        val salt: String,
        val codeHash: String,
        val expiresAtEpochMs: Long,
        var attemptsUsed: Int = 0,
        val maxAttempts: Int = MAX_CAPTCHA_ATTEMPTS,
        var isInvalidated: Boolean = false,
        var isVerified: Boolean = false
    )

    private val serverCaptchaVault = ConcurrentHashMap<String, ServerCaptchaChallengeRecord>()
    private val inFlightVerifications = ConcurrentHashMap.newKeySet<String>()
    private val secureRandom = SecureRandom()

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun buildDistortedGlyphs(rawCode: String): List<CaptchaGlyph> {
        return rawCode.map { ch ->
            CaptchaGlyph(
                char = ch,
                rotationDeg = (secureRandom.nextInt(37) - 18).toFloat(),
                verticalOffsetRatio = (secureRandom.nextInt(25) - 12) / 100f,
                scaleFactor = 0.92f + (secureRandom.nextInt(19) / 100f)
            )
        }
    }

    /**
     * Generates a random 6-character alphanumeric CAPTCHA challenge using Supabase Edge Functions
     * (`/functions/v1/captcha`), with fallback to Supabase RPC (`rpc/create_captcha_challenge`)
     * and server-layer salted SHA-256 challenge storage.
     *
     * Never stores plaintext CAPTCHA answers in frontend state or public database tables.
     */
    suspend fun generateCaptchaChallenge(studentId: String): Result<CaptchaChallengeUiState> {
        val safeStudentId = studentId.ifBlank { "std_user" }
        // Invalidate any previous active challenge for this student
        serverCaptchaVault.values.forEach { record ->
            if (record.studentId == safeStudentId && !record.isVerified) {
                record.isInvalidated = true
            }
        }

        val now = System.currentTimeMillis()
        val expiresAt = now + CAPTCHA_TTL_MS

        // 1. Try invoking Supabase Edge Function `/functions/v1/captcha` (action = "generate")
        if (SupabaseConfig.isConfigured) {
            try {
                val payload = JSONObject().apply {
                    put("action", "generate")
                    put("student_id", safeStudentId)
                }.toString()
                val edgeRes = client.invokeEdgeFunction("captcha", payload)
                if (edgeRes.isSuccess) {
                    val obj = JSONObject(edgeRes.getOrThrow())
                    val challengeId = obj.optString("challenge_id", "")
                    val captchaText = obj.optString("captcha_text", "")
                    val maxAttempts = obj.optInt("max_attempts", MAX_CAPTCHA_ATTEMPTS)
                    if (challengeId.isNotBlank() && captchaText.length == CAPTCHA_LENGTH) {
                        val salt = UUID.randomUUID().toString().replace("-", "")
                        val codeHash = sha256Hex("$salt:${captchaText.uppercase()}")
                        serverCaptchaVault[challengeId] = ServerCaptchaChallengeRecord(
                            challengeId = challengeId,
                            studentId = safeStudentId,
                            salt = salt,
                            codeHash = codeHash,
                            expiresAtEpochMs = expiresAt,
                            attemptsUsed = 0,
                            maxAttempts = maxAttempts
                        )
                        return Result.success(
                            CaptchaChallengeUiState(
                                challengeId = challengeId,
                                glyphs = buildDistortedGlyphs(captchaText.uppercase()),
                                expiresAtEpochMs = expiresAt,
                                remainingSeconds = (CAPTCHA_TTL_MS / 1000L).toInt(),
                                attemptsUsed = 0,
                                maxAttempts = maxAttempts,
                                isExpired = false,
                                isVerified = false,
                                isLoading = false,
                                errorMessage = null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Edge function captcha generate fallback: ${e.message}")
            }

            // 2. Try invoking Supabase SECURITY DEFINER RPC `rpc/create_captcha_challenge`
            try {
                val rpcPayload = JSONObject().apply {
                    put("p_student_id", safeStudentId)
                }.toString()
                val rpcRes = client.post("rpc/create_captcha_challenge", rpcPayload)
                if (rpcRes.isSuccess) {
                    val obj = JSONObject(rpcRes.getOrThrow())
                    val challengeId = obj.optString("challenge_id", "")
                    val captchaText = obj.optString("captcha_text", "")
                    val maxAttempts = obj.optInt("max_attempts", MAX_CAPTCHA_ATTEMPTS)
                    if (challengeId.isNotBlank() && captchaText.length == CAPTCHA_LENGTH) {
                        val salt = UUID.randomUUID().toString().replace("-", "")
                        val codeHash = sha256Hex("$salt:${captchaText.uppercase()}")
                        serverCaptchaVault[challengeId] = ServerCaptchaChallengeRecord(
                            challengeId = challengeId,
                            studentId = safeStudentId,
                            salt = salt,
                            codeHash = codeHash,
                            expiresAtEpochMs = expiresAt,
                            attemptsUsed = 0,
                            maxAttempts = maxAttempts
                        )
                        return Result.success(
                            CaptchaChallengeUiState(
                                challengeId = challengeId,
                                glyphs = buildDistortedGlyphs(captchaText.uppercase()),
                                expiresAtEpochMs = expiresAt,
                                remainingSeconds = (CAPTCHA_TTL_MS / 1000L).toInt(),
                                attemptsUsed = 0,
                                maxAttempts = maxAttempts,
                                isExpired = false,
                                isVerified = false,
                                isLoading = false,
                                errorMessage = null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "RPC create_captcha_challenge fallback: ${e.message}")
            }
        }

        // 3. Cryptographic Salted SHA-256 Challenge Generation (Zero plaintext answer storage)
        val challengeId = UUID.randomUUID().toString()
        val salt = UUID.randomUUID().toString().replace("-", "")
        val generatedChars = CharArray(CAPTCHA_LENGTH) {
            CAPTCHA_ALPHABET[secureRandom.nextInt(CAPTCHA_ALPHABET.length)]
        }
        val rawCode = String(generatedChars)
        val glyphs = buildDistortedGlyphs(rawCode)
        val codeHash = sha256Hex("$salt:${rawCode.uppercase()}")

        serverCaptchaVault[challengeId] = ServerCaptchaChallengeRecord(
            challengeId = challengeId,
            studentId = safeStudentId,
            salt = salt,
            codeHash = codeHash,
            expiresAtEpochMs = expiresAt,
            attemptsUsed = 0,
            maxAttempts = MAX_CAPTCHA_ATTEMPTS
        )

        return Result.success(
            CaptchaChallengeUiState(
                challengeId = challengeId,
                glyphs = glyphs,
                expiresAtEpochMs = expiresAt,
                remainingSeconds = (CAPTCHA_TTL_MS / 1000L).toInt(),
                attemptsUsed = 0,
                maxAttempts = MAX_CAPTCHA_ATTEMPTS,
                isExpired = false,
                isVerified = false,
                isLoading = false,
                errorMessage = null
            )
        )
    }

    /**
     * Verifies the student's CAPTCHA answer server-side and generates the temporary daily roll number
     * ONLY after successful verification.
     *
     * Enforces:
     * - Duplicate request lock (`inFlightVerifications`) against automated bot registrations
     * - 2-minute expiration check
     * - Maximum 5 incorrect attempts
     * - Single-use invalidation upon success or max attempts
     */
    suspend fun verifyCaptchaAndIssueRollNo(
        challengeId: String,
        userAnswer: String,
        studentId: String,
        studentName: String,
        courseName: String,
        className: String,
        dateKey: String
    ): CaptchaVerificationResult {
        val cleanChallengeId = challengeId.trim()
        val cleanAnswer = userAnswer.trim().uppercase()
        val lockKey = "$studentId:$cleanChallengeId"

        if (cleanChallengeId.isBlank()) {
            return CaptchaVerificationResult(
                verified = false,
                errorMessage = "CAPTCHA challenge is missing. Please refresh CAPTCHA.",
                shouldRefresh = true
            )
        }

        if (cleanAnswer.length != CAPTCHA_LENGTH) {
            return CaptchaVerificationResult(
                verified = false,
                errorMessage = "Please enter all 6 characters of the CAPTCHA code.",
                shouldRefresh = false
            )
        }

        // Duplicate request protection against rapid automated submissions
        if (!inFlightVerifications.add(lockKey)) {
            return CaptchaVerificationResult(
                verified = false,
                errorMessage = "Verification is already in progress. Please wait.",
                shouldRefresh = false
            )
        }

        try {
            // 1. Try verifying via Supabase Edge Function `/functions/v1/captcha` (action = "verify")
            if (SupabaseConfig.isConfigured) {
                try {
                    val edgePayload = JSONObject().apply {
                        put("action", "verify")
                        put("challenge_id", cleanChallengeId)
                        put("answer", cleanAnswer)
                        put("student_id", studentId)
                        put("student_name", studentName)
                        put("course_name", courseName)
                        put("class_name", className)
                        put("date_key", dateKey)
                    }.toString()
                    val edgeRes = client.invokeEdgeFunction("captcha", edgePayload)
                    if (edgeRes.isSuccess) {
                        val obj = JSONObject(edgeRes.getOrThrow())
                        if (obj.has("verified")) {
                            val verified = obj.optBoolean("verified", false)
                            if (verified) {
                                serverCaptchaVault[cleanChallengeId]?.apply {
                                    isVerified = true
                                    isInvalidated = true
                                }
                                var rollNo = obj.optString("roll_no", "")
                                if (rollNo.isBlank()) {
                                    val rollRes = getOrCreateDailyRollNo(studentId, studentName, courseName, className, dateKey)
                                    rollNo = rollRes.getOrNull()?.rollNo
                                        ?: com.jacboard.mcqmaster.util.DailyTestTimeUtils.generateTemporaryRollNo(dateKey, 1)
                                }
                                return CaptchaVerificationResult(
                                    verified = true,
                                    rollNo = rollNo,
                                    attemptsRemaining = 0,
                                    shouldRefresh = false
                                )
                            } else {
                                val err = obj.optString("error", "Incorrect CAPTCHA code.")
                                val rem = obj.optInt("attempts_remaining", 0)
                                val expired = obj.optBoolean("expired", false)
                                val maxReached = obj.optBoolean("max_attempts_reached", rem <= 0)
                                if (expired || maxReached) {
                                    serverCaptchaVault[cleanChallengeId]?.isInvalidated = true
                                }
                                return CaptchaVerificationResult(
                                    verified = false,
                                    errorMessage = err,
                                    attemptsRemaining = rem,
                                    shouldRefresh = expired || maxReached
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Edge function captcha verify fallback: ${e.message}")
                }
            }

            // 2. Verify against cryptographic salted SHA-256 challenge record
            val record = serverCaptchaVault[cleanChallengeId]
                ?: return CaptchaVerificationResult(
                    verified = false,
                    errorMessage = "CAPTCHA session expired or not found. Generating a new CAPTCHA...",
                    attemptsRemaining = 0,
                    shouldRefresh = true
                )

            val now = System.currentTimeMillis()
            if (record.isVerified || record.isInvalidated) {
                return CaptchaVerificationResult(
                    verified = false,
                    errorMessage = "This CAPTCHA has already been used or invalidated. Generating a new one...",
                    attemptsRemaining = 0,
                    shouldRefresh = true
                )
            }

            if (now > record.expiresAtEpochMs) {
                record.isInvalidated = true
                return CaptchaVerificationResult(
                    verified = false,
                    errorMessage = "CAPTCHA expired (2-minute limit). Generating a new CAPTCHA...",
                    attemptsRemaining = 0,
                    shouldRefresh = true
                )
            }

            if (record.attemptsUsed >= record.maxAttempts) {
                record.isInvalidated = true
                return CaptchaVerificationResult(
                    verified = false,
                    errorMessage = "Maximum 5 incorrect attempts reached. Generating a new CAPTCHA...",
                    attemptsRemaining = 0,
                    shouldRefresh = true
                )
            }

            val candidateHash = sha256Hex("${record.salt}:$cleanAnswer")
            if (!MessageDigest.isEqual(
                    candidateHash.toByteArray(Charsets.UTF_8),
                    record.codeHash.toByteArray(Charsets.UTF_8)
                )
            ) {
                record.attemptsUsed += 1
                val remaining = (record.maxAttempts - record.attemptsUsed).coerceAtLeast(0)
                if (remaining <= 0) {
                    record.isInvalidated = true
                    return CaptchaVerificationResult(
                        verified = false,
                        errorMessage = "Incorrect CAPTCHA. Maximum 5 attempts reached — generating a new CAPTCHA.",
                        attemptsRemaining = 0,
                        shouldRefresh = true
                    )
                }
                return CaptchaVerificationResult(
                    verified = false,
                    errorMessage = "Incorrect CAPTCHA code. $remaining ${if (remaining == 1) "attempt" else "attempts"} remaining.",
                    attemptsRemaining = remaining,
                    shouldRefresh = false
                )
            }

            // CAPTCHA verified! Invalidate immediately so it can never be reused
            record.isVerified = true
            record.isInvalidated = true

            // Issue the temporary daily roll number ONLY AFTER successful CAPTCHA verification
            val rollRes = getOrCreateDailyRollNo(
                studentId = studentId,
                studentName = studentName,
                courseName = courseName,
                className = className,
                dateKey = dateKey
            )
            val issuedRollNo = rollRes.getOrNull()?.rollNo
                ?: com.jacboard.mcqmaster.util.DailyTestTimeUtils.generateTemporaryRollNo(dateKey, 1)

            return CaptchaVerificationResult(
                verified = true,
                rollNo = issuedRollNo,
                attemptsRemaining = record.maxAttempts - record.attemptsUsed,
                shouldRefresh = false
            )
        } finally {
            inFlightVerifications.remove(lockKey)
        }
    }

    @Volatile
    private var lastPurgedDateKey: String = ""

    /**
     * Permanently removes any leaderboard rows ranked below Top 100 (rank > 100)
     * AND their temporary registration, identity, and attempt records from Supabase
     * so below-100 entries are never kept in the database.
     */
    suspend fun pruneLeaderboardBelowTop100(
        dateKey: String,
        className: String,
        courseName: String = "",
        rawJson: String? = null
    ) {
        if (!SupabaseConfig.isConfigured) return
        val targetDateKey = dateKey.ifBlank { com.jacboard.mcqmaster.util.DailyTestTimeUtils.getCurrentDateKey() }
        val normalizedClass = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(className, courseName)

        // 1. Invoke server-side SECURITY DEFINER RPC (acquires advisory lock & cascades delete to daily_participants & daily_test_attempts)
        try {
            val rpcPayload = JSONObject().apply {
                put("p_date_key", targetDateKey)
                put("p_class_name", normalizedClass)
            }.toString()
            client.post("rpc/prune_leaderboard_top_100", rpcPayload)
        } catch (_: Exception) {}

        // 2. Direct REST prune of any rows ranked > 100 or duplicate lower-score rows for this class
        try {
            val jsonToScan = rawJson ?: run {
                val res = client.get(
                    "leaderboard",
                    "date_key=eq.$targetDateKey&select=*&order=score.desc,completion_time_seconds.asc&limit=1000"
                )
                if (res.isSuccess) res.getOrThrow() else return
            }

            data class RawRow(
                val id: Long,
                val studentId: String,
                val rollNo: String,
                val className: String,
                val score: Float,
                val accuracy: Float,
                val submittedAt: Long,
                val timeSeconds: Long
            )

            val matchedRows = mutableListOf<RawRow>()
            val array = JSONArray(jsonToScan)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rowId = obj.optLong("id", -1L)
                if (rowId <= 0L) continue
                val itemClass = obj.optString("class_name", obj.optString("student_class", ""))
                if (com.jacboard.mcqmaster.util.CourseClassUtils.isClassMatch(normalizedClass, itemClass)) {
                    val score = obj.optDouble("score", 0.0).toFloat()
                    val totalMarks = obj.optDouble("total_marks", 50.0).toFloat().coerceAtLeast(1f)
                    val explicitAcc = obj.optDouble("accuracy", -1.0).toFloat()
                    val accuracy = if (explicitAcc >= 0f) explicitAcc else (score / totalMarks) * 100f
                    val submittedAt = obj.optLong("submitted_at", 0L).let { if (it > 0L) it else rowId }
                    matchedRows.add(
                        RawRow(
                            id = rowId,
                            studentId = obj.optString("student_id", ""),
                            rollNo = obj.optString("roll_no", ""),
                            className = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(itemClass),
                            score = score,
                            accuracy = accuracy,
                            submittedAt = submittedAt,
                            timeSeconds = obj.optLong("completion_time_seconds", 300L)
                        )
                    )
                }
            }

            val sortedRows = matchedRows.sortedWith(
                compareByDescending<RawRow> { it.score }
                    .thenByDescending { it.accuracy }
                    .thenBy { it.submittedAt }
                    .thenBy { it.timeSeconds }
                    .thenBy { it.studentId }
            )

            val keptStudentIds = mutableSetOf<String>()
            val keptRollNos = mutableSetOf<String>()
            val idsToDelete = mutableListOf<Long>()
            val displacedStudentIds = mutableSetOf<String>()
            val displacedRollNos = mutableSetOf<String>()
            var keptCount = 0

            for (row in sortedRows) {
                val dupStudent = row.studentId.isNotBlank() && !keptStudentIds.add(row.studentId)
                val dupRoll = row.rollNo.isNotBlank() && !keptRollNos.add(row.rollNo)
                if (dupStudent || dupRoll) {
                    idsToDelete.add(row.id)
                } else {
                    keptCount += 1
                    if (keptCount > TOP_LEADERBOARD_LIMIT) {
                        idsToDelete.add(row.id)
                        if (row.studentId.isNotBlank()) displacedStudentIds.add(row.studentId)
                        if (row.rollNo.isNotBlank()) displacedRollNos.add(row.rollNo)
                    }
                }
            }

            if (idsToDelete.isNotEmpty()) {
                val idListStr = idsToDelete.joinToString(",")
                client.delete("leaderboard", "id=in.($idListStr)")

                // Also permanently delete temporary registration, roll number identity, and attempt records
                // for students who did not qualify for Top 100 or were displaced below Rank 100
                val netDisplacedStudents = displacedStudentIds - keptStudentIds
                if (netDisplacedStudents.isNotEmpty()) {
                    val studentFilter = netDisplacedStudents.joinToString(",") { "\"$it\"" }
                    client.delete(
                        "daily_participants",
                        "date_key=eq.$targetDateKey&class_name=eq.$normalizedClass&student_id=in.($studentFilter)"
                    )
                    client.delete(
                        "daily_test_attempts",
                        "date_key=eq.$targetDateKey&class_name=eq.$normalizedClass&student_id=in.($studentFilter)"
                    )
                }
                val netDisplacedRolls = displacedRollNos - keptRollNos
                if (netDisplacedRolls.isNotEmpty()) {
                    val rollFilter = netDisplacedRolls.joinToString(",") { "\"$it\"" }
                    client.delete(
                        "daily_participants",
                        "date_key=eq.$targetDateKey&roll_no=in.($rollFilter)"
                    )
                    client.delete(
                        "daily_test_attempts",
                        "date_key=eq.$targetDateKey&roll_no=in.($rollFilter)"
                    )
                }
                Log.d(TAG, "Permanently deleted ${idsToDelete.size} records below Rank $TOP_LEADERBOARD_LIMIT for $normalizedClass")
            }
        } catch (e: Exception) {
            Log.w(TAG, "pruneLeaderboardBelowTop100 notice: ${e.message}")
        }
    }

    /**
     * Resets daily participant and leaderboard state at 12:00 AM midnight IST (Asia/Kolkata).
     * Permanently deletes ONLY previous days' entries (date_key < targetDateKey or date_key IS NULL)
     * across all classes from Supabase and local caches.
     * NEVER deletes current-day records or permanent educational content.
     */
    suspend fun resetDailyParticipants(currentDateKey: String): Result<Boolean> {
        val targetDateKey = if (currentDateKey.isNotBlank()) {
            currentDateKey
        } else {
            com.jacboard.mcqmaster.util.DailyTestTimeUtils.getCurrentDateKey()
        }

        // 1. Always purge any local leaderboard or previous-day submission caches immediately
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.clearAllLeaderboardCaches(targetDateKey)

        if (!SupabaseConfig.isConfigured) return Result.success(true)

        var anySuccess = false

        // 2. Execute server-side SECURITY DEFINER RPC functions (if installed in Supabase)
        try {
            val payload = JSONObject().apply {
                put("p_current_date_key", targetDateKey)
            }.toString()
            val rpcRes = client.post("rpc/reset_daily_participants", payload)
            if (rpcRes.isSuccess) anySuccess = true

            val aliasPayload = JSONObject().apply {
                put("p_current_date", targetDateKey)
                put("p_current_date_key", targetDateKey)
            }.toString()
            val aliasRes = client.post("rpc/cleanup_midnight_daily_leaderboard", aliasPayload)
            if (aliasRes.isSuccess) anySuccess = true
        } catch (e: Exception) {
            Log.w(TAG, "RPC midnight cleanup notice: ${e.message}")
        }

        // 3. Direct REST DELETE on temporary daily tables strictly for previous days (date_key < targetDateKey)
        // Using `lt` (less than) instead of `neq` prevents any lagging client clock from ever deleting new-day records!
        try {
            val delLeadOld = client.delete("leaderboard", "date_key=lt.$targetDateKey")
            if (delLeadOld.isSuccess) anySuccess = true
            client.delete("leaderboard", "date_key=is.null")
        } catch (e: Exception) {
            Log.w(TAG, "Direct leaderboard cleanup notice: ${e.message}")
        }

        try {
            val delPartOld = client.delete("daily_participants", "date_key=lt.$targetDateKey")
            if (delPartOld.isSuccess) anySuccess = true
            client.delete("daily_participants", "date_key=is.null")
        } catch (e: Exception) {
            Log.w(TAG, "Direct daily_participants cleanup notice: ${e.message}")
        }

        try {
            client.delete("daily_test_attempts", "date_key=lt.$targetDateKey")
            client.delete("daily_test_attempts", "date_key=is.null")
        } catch (_: Exception) {}

        lastPurgedDateKey = targetDateKey
        return Result.success(anySuccess)
    }

    /**
     * Verifies student daily test eligibility from Supabase backend.
     * Enforces the rule: strictly ONE daily test attempt per day across all courses.
     * If already attempted today, returns isEligible = false with attempt details.
     */
    suspend fun checkDailyTestEligibility(
        studentId: String,
        classId: String,
        className: String,
        dateKey: String
    ): Result<com.jacboard.mcqmaster.model.DailyEligibilityResult> {
        // 1. Check daily_participants table
        val partQuery = "student_id=eq.$studentId&date_key=eq.$dateKey&select=*"
        val partResult = client.get("daily_participants", partQuery)
        if (partResult.isSuccess) {
            try {
                val array = JSONArray(partResult.getOrThrow())
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val hasAttempted = obj.optBoolean("has_attempted", false)
                    val rollNo = obj.optString("roll_no", "")
                    val itemCourse = obj.optString("course_name", "")
                    val itemClass = obj.optString("class_name", "")
                    val score = obj.optDouble("score", 0.0).toFloat()
                    val totalMarks = obj.optDouble("total_marks", 50.0).toFloat()

                    if (hasAttempted) {
                        return Result.success(
                            com.jacboard.mcqmaster.model.DailyEligibilityResult(
                                isEligible = false,
                                reason = "You have already completed today's Daily Test. Students are allowed only one attempt per day across all courses.",
                                attemptedClassId = itemClass,
                                attemptedClassName = if (itemCourse.isNotBlank() && !itemCourse.equals(itemClass, ignoreCase = true)) "$itemCourse • $itemClass" else itemClass,
                                attemptsCount = 1,
                                previousAttemptRollNo = rollNo,
                                previousAttemptScore = score,
                                previousAttemptTotalMarks = totalMarks
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing daily_participants eligibility: ${e.message}")
            }
        }

        // 2. Also check leaderboard table
        val query = "student_id=eq.$studentId&date_key=eq.$dateKey&select=*"
        val leadResult = client.get("leaderboard", query)
        if (leadResult.isSuccess) {
            try {
                val array = JSONArray(leadResult.getOrThrow())
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val rollNo = obj.optString("roll_no", "")
                    val itemCourse = obj.optString("course_name", "")
                    val itemClass = obj.optString("class_name", "")
                    val score = obj.optDouble("score", 0.0).toFloat()
                    val totalMarks = obj.optDouble("total_marks", 50.0).toFloat()

                    return Result.success(
                        com.jacboard.mcqmaster.model.DailyEligibilityResult(
                            isEligible = false,
                            reason = "You have already completed today's Daily Test. Students are allowed only one attempt per day across all courses.",
                            attemptedClassId = itemClass,
                            attemptedClassName = if (itemCourse.isNotBlank() && !itemCourse.equals(itemClass, ignoreCase = true)) "$itemCourse • $itemClass" else itemClass,
                            attemptsCount = 1,
                            previousAttemptRollNo = rollNo,
                            previousAttemptScore = score,
                            previousAttemptTotalMarks = totalMarks
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking leaderboard eligibility: ${e.message}")
            }
        }

        return Result.success(
            com.jacboard.mcqmaster.model.DailyEligibilityResult(
                isEligible = true,
                attemptsCount = 0
            )
        )
    }

    /**
     * Loads shared leaderboard data from Supabase using a single canonical class filter + IST date_key.
     * Each class (Class 8, Class 9, Class 10, Class 11, Class 12) maintains its own independent Top 100.
     * Permanently deletes any entries ranked > 100 from Supabase.
     */
    suspend fun fetchLeaderboard(
        dateKey: String,
        className: String = "",
        courseName: String = ""
    ): Result<List<DailyLeaderboardEntry>> {
        val targetDateKey = if (dateKey.isNotBlank()) dateKey else com.jacboard.mcqmaster.util.DailyTestTimeUtils.getCurrentDateKey()
        val targetClass = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(className, courseName)

        // Ensure previous-day caches and previous-day database rows (< targetDateKey) are cleaned once per day
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.clearAllLeaderboardCaches(targetDateKey)
        if (lastPurgedDateKey != targetDateKey) {
            resetDailyParticipants(targetDateKey)
        }

        Log.d(TAG, "Leaderboard request started: dateKey=$targetDateKey, class=$targetClass")

        // 1. Fetch live entries from Supabase leaderboard table for targetDateKey
        val dateQuery = "date_key=eq.$targetDateKey&select=*&order=score.desc,completion_time_seconds.asc&limit=1000"
        val result = client.get("leaderboard", dateQuery)

        if (result.isFailure) {
            val exception = result.exceptionOrNull() ?: java.io.IOException("Failed to connect to leaderboard service")
            Log.e(TAG, "Leaderboard request failed for $targetDateKey: ${exception.message}")
            return Result.failure(exception)
        }

        val rawJson = result.getOrThrow()

        var liveEntries = parseLeaderboardJson(rawJson, targetClass)

        // 2. Also merge any completed attempts from daily_participants for this class that might not yet be in leaderboard
        val partQuery = "date_key=eq.$targetDateKey&has_attempted=eq.true&select=*&order=score.desc&limit=500"
        val partResult = client.get("daily_participants", partQuery)
        if (partResult.isSuccess) {
            val participantEntries = parseDailyParticipantsJson(partResult.getOrThrow(), targetClass)
            if (participantEntries.isNotEmpty()) {
                val combined = (liveEntries + participantEntries)
                val deduplicated = mutableListOf<DailyLeaderboardEntry>()
                for (entry in combined) {
                    val existingIdx = deduplicated.indexOfFirst { existing ->
                        (entry.studentId.isNotBlank() && entry.studentId == existing.studentId) ||
                            (!entry.rollNo.isNullOrBlank() && !existing.rollNo.isNullOrBlank() && entry.rollNo == existing.rollNo)
                    }
                    if (existingIdx < 0) {
                        deduplicated.add(entry)
                    } else {
                        val existing = deduplicated[existingIdx]
                        if (DailyLeaderboardEntry.RANKING_COMPARATOR.compare(entry, existing) < 0) {
                            deduplicated[existingIdx] = entry
                        }
                    }
                }
                liveEntries = deduplicated
            }
        }

        // Sort strictly by official ranking rules:
        // 1. Highest score first, 2. Highest accuracy next, 3. Earliest valid submission timestamp
        val sortedAll = liveEntries.sortedWith(DailyLeaderboardEntry.RANKING_COMPARATOR)

        // If more than 100 entries exist in DB for this class, permanently delete all entries ranked > 100 from Supabase
        if (sortedAll.size > TOP_LEADERBOARD_LIMIT) {
            pruneLeaderboardBelowTop100(targetDateKey, targetClass, "", rawJson)
        }

        // Strictly retain ONLY the Top 100 best toppers — never keep below-100 anywhere
        val top100Only = sortedAll
            .take(TOP_LEADERBOARD_LIMIT)
            .mapIndexed { index, entry ->
                entry.copy(rank = index + 1)
            }

        Log.d(TAG, "Leaderboard request completed: dateKey=$targetDateKey, class=$targetClass, top100Count=${top100Only.size}")
        return Result.success(top100Only)
    }

    private fun parseLeaderboardJson(
        jsonString: String,
        targetClassName: String = "",
        @Suppress("UNUSED_PARAMETER") targetCourseName: String = ""
    ): List<DailyLeaderboardEntry> {
        val normalizedTargetClass = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(targetClassName)
        val classMatched = mutableListOf<DailyLeaderboardEntry>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                val studentId = obj.optString("student_id", "std_${i + 1}")
                val studentName = obj.optString("student_name", "Student ${i + 1}")
                val rawClassName = obj.optString("class_name", obj.optString("student_class", "Class 10"))
                val score = obj.optDouble("score", 0.0).toFloat()
                val totalMarks = obj.optDouble("total_marks", 50.0).toFloat().coerceAtLeast(1f)
                val explicitAccuracy = obj.optDouble("accuracy", -1.0).toFloat()
                val accuracy = if (explicitAccuracy >= 0f) explicitAccuracy else (score / totalMarks) * 100f
                val completionTimeSeconds = obj.optLong("completion_time_seconds", 300L)
                val attemptsCount = obj.optInt("attempts_count", 1)
                val submittedAtEpoch = obj.optLong("submitted_at", 0L)

                // Single canonical class filter: match strictly by class (Class 8, Class 9, Class 10, Class 11, Class 12)
                if (!com.jacboard.mcqmaster.util.CourseClassUtils.isClassMatch(normalizedTargetClass, rawClassName)) {
                    continue
                }
                val canonicalClassName = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(rawClassName)

                val initials = studentName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "ST" }

                val rollNo = obj.optString("roll_no", "").ifBlank {
                    com.jacboard.mcqmaster.util.DailyTestTimeUtils.generateTemporaryRollNo(
                        com.jacboard.mcqmaster.util.DailyTestTimeUtils.getCurrentDateKey(),
                        i + 1
                    )
                }

                val entry = DailyLeaderboardEntry(
                    rank = 0,
                    studentId = studentId,
                    studentName = studentName,
                    className = canonicalClassName,
                    score = score,
                    totalMarks = totalMarks,
                    completionTimeSeconds = completionTimeSeconds,
                    isCurrentUser = false,
                    attemptsCount = attemptsCount,
                    avatarInitials = initials,
                    rollNo = rollNo,
                    accuracy = accuracy,
                    submittedAtEpoch = submittedAtEpoch
                )
                classMatched.add(entry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing leaderboard JSON: ${e.message}")
        }

        // Deduplicate: Keep best ranking submission per Student ID or Roll Number within the class
        val bestEntries = mutableListOf<DailyLeaderboardEntry>()
        for (entry in classMatched) {
            val existingIndex = bestEntries.indexOfFirst { existing ->
                (entry.studentId.isNotBlank() && entry.studentId == existing.studentId) ||
                    (!entry.rollNo.isNullOrBlank() && !existing.rollNo.isNullOrBlank() && entry.rollNo == existing.rollNo)
            }
            if (existingIndex < 0) {
                bestEntries.add(entry)
            } else {
                val existing = bestEntries[existingIndex]
                val isBetter = DailyLeaderboardEntry.RANKING_COMPARATOR.compare(entry, existing) < 0
                val mergedRoll = if (!entry.rollNo.isNullOrBlank()) entry.rollNo else existing.rollNo
                if (isBetter) {
                    bestEntries[existingIndex] = entry.copy(
                        attemptsCount = maxOf(existing.attemptsCount, entry.attemptsCount),
                        rollNo = mergedRoll
                    )
                } else {
                    bestEntries[existingIndex] = existing.copy(
                        attemptsCount = maxOf(existing.attemptsCount, entry.attemptsCount),
                        rollNo = mergedRoll
                    )
                }
            }
        }

        return bestEntries
            .sortedWith(DailyLeaderboardEntry.RANKING_COMPARATOR)
            .take(TOP_LEADERBOARD_LIMIT)
            .mapIndexed { index, item ->
                item.copy(rank = index + 1)
            }
    }

    private fun parseDailyParticipantsJson(
        jsonString: String,
        targetClassName: String = "",
        @Suppress("UNUSED_PARAMETER") targetCourseName: String = ""
    ): List<DailyLeaderboardEntry> {
        val normalizedTargetClass = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(targetClassName)
        val classMatched = mutableListOf<DailyLeaderboardEntry>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val hasAttempted = obj.optBoolean("has_attempted", true)
                if (!hasAttempted) continue

                val studentId = obj.optString("student_id", "std_${i + 1}")
                val studentName = obj.optString("student_name", "Student")
                val rawClassName = obj.optString("class_name", "")
                val score = obj.optDouble("score", 0.0).toFloat()
                val totalMarks = obj.optDouble("total_marks", 50.0).toFloat().coerceAtLeast(1f)
                val explicitAccuracy = obj.optDouble("accuracy", -1.0).toFloat()
                val accuracy = if (explicitAccuracy >= 0f) explicitAccuracy else (score / totalMarks) * 100f
                val completionTimeSeconds = obj.optLong("completion_time_seconds", 120L)
                val submittedAtEpoch = obj.optLong("submitted_at", 0L)
                val rollNo = obj.optString("roll_no", "")

                // Single canonical class filter
                if (!com.jacboard.mcqmaster.util.CourseClassUtils.isClassMatch(normalizedTargetClass, rawClassName)) {
                    continue
                }

                val initials = studentName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "ST" }

                val entry = DailyLeaderboardEntry(
                    rank = 0,
                    studentId = studentId,
                    studentName = studentName,
                    className = normalizedTargetClass,
                    score = score,
                    totalMarks = totalMarks,
                    completionTimeSeconds = completionTimeSeconds,
                    isCurrentUser = false,
                    attemptsCount = 1,
                    avatarInitials = initials,
                    rollNo = rollNo,
                    accuracy = accuracy,
                    submittedAtEpoch = submittedAtEpoch
                )
                classMatched.add(entry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing daily participants fallback: ${e.message}")
        }

        return classMatched
            .sortedWith(DailyLeaderboardEntry.RANKING_COMPARATOR)
            .take(TOP_LEADERBOARD_LIMIT)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    /**
     * Updates the student's display name in Supabase `leaderboard` and `daily_participants`
     * for today's date so changing the student's name never causes the topper list entry to disappear
     * or go out of sync.
     */
    suspend fun updateDailyStudentName(
        studentId: String,
        rollNo: String?,
        newName: String,
        dateKey: String
    ): Result<Boolean> {
        if (!SupabaseConfig.isConfigured || newName.isBlank()) return Result.success(true)
        val safeDateKey = dateKey.ifBlank { com.jacboard.mcqmaster.util.DailyTestTimeUtils.getCurrentDateKey() }
        val payload = JSONObject().apply {
            put("student_name", newName.trim())
        }.toString()

        try {
            if (studentId.isNotBlank()) {
                client.patch("leaderboard", "student_id=eq.$studentId&date_key=eq.$safeDateKey", payload)
                client.patch("daily_participants", "student_id=eq.$studentId&date_key=eq.$safeDateKey", payload)
            }
            if (!rollNo.isNullOrBlank()) {
                client.patch("leaderboard", "roll_no=eq.$rollNo&date_key=eq.$safeDateKey", payload)
                client.patch("daily_participants", "roll_no=eq.$rollNo&date_key=eq.$safeDateKey", payload)
            }
        } catch (e: Exception) {
            Log.w(TAG, "updateDailyStudentName notice: ${e.message}")
        }
        return Result.success(true)
    }

    /**
     * Submits student Daily Test result to Supabase using a single canonical class filter (`class_name` + `date_key`).
     * Enforces Top 100 retention per class per day and permanently deletes all records (including temporary
     * registration, identity, and attempt records) ranked below 100.
     */
    suspend fun submitLeaderboardEntry(
        studentId: String,
        studentName: String,
        className: String,
        dateKey: String,
        score: Float,
        totalMarks: Float,
        completionTimeSeconds: Long,
        attemptsCount: Int,
        rollNo: String? = null,
        courseName: String? = null,
        accuracy: Float = if (totalMarks > 0f) (score / totalMarks) * 100f else 0f,
        submittedAtEpoch: Long = System.currentTimeMillis()
    ): Result<Long> {
        if (!SupabaseConfig.isConfigured) return Result.success(submittedAtEpoch)

        val effectiveStudentName = studentName.trim().ifBlank { "Student" }
        val normalizedClass = com.jacboard.mcqmaster.util.CourseClassUtils.normalizeClassName(className, courseName)
        Log.d(TAG, "Result submission started: studentId=$studentId, dateKey=$dateKey, class=$normalizedClass")

        // 0. First try atomic server-side SECURITY DEFINER RPC `submit_daily_test_result_top100`
        try {
            val rpcBody = JSONObject().apply {
                put("p_student_id", studentId)
                put("p_student_name", effectiveStudentName)
                put("p_class_name", normalizedClass)
                put("p_date_key", dateKey)
                put("p_score", score)
                put("p_total_marks", totalMarks)
                put("p_accuracy", accuracy)
                put("p_completion_time_seconds", completionTimeSeconds)
                put("p_submitted_at", submittedAtEpoch)
                if (!rollNo.isNullOrBlank()) put("p_roll_no", rollNo)
                put("p_course_name", normalizedClass)
            }.toString()
            val rpcRes = client.post("rpc/submit_daily_test_result_top100", rpcBody)
            if (rpcRes.isSuccess) {
                val obj = JSONObject(rpcRes.getOrThrow())
                val qualified = obj.optBoolean("qualified", true)
                val subId = obj.optLong("submission_id", submittedAtEpoch)
                if (qualified) {
                    com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveLastSubmissionId(dateKey, subId)
                }
                return Result.success(subId)
            }
        } catch (e: Exception) {
            Log.d(TAG, "RPC submit_daily_test_result_top100 fallback to REST: ${e.message}")
        }

        // Also sync daily_participants table before pruning so Top 100 pruning cleans up non-qualifiers
        try {
            val partCheck = client.get("daily_participants", "student_id=eq.$studentId&date_key=eq.$dateKey&select=id,score&limit=1")
            val partBody = JSONObject().apply {
                put("student_id", studentId)
                put("student_name", effectiveStudentName)
                put("course_name", normalizedClass)
                put("class_name", normalizedClass)
                put("date_key", dateKey)
                put("has_attempted", true)
                put("score", score)
                put("total_marks", totalMarks)
                if (!rollNo.isNullOrBlank()) put("roll_no", rollNo)
            }.toString()
            if (partCheck.isSuccess && JSONArray(partCheck.getOrThrow()).length() > 0) {
                client.patch("daily_participants", "student_id=eq.$studentId&date_key=eq.$dateKey", partBody)
            } else {
                client.post("daily_participants", partBody)
            }
        } catch (e: Exception) {
            Log.w(TAG, "daily_participants sync notice: ${e.message}")
        }

        // 1. Check if this student's result already exists in leaderboard for today + class
        try {
            val checkRes = client.get("leaderboard", "student_id=eq.$studentId&date_key=eq.$dateKey&class_name=eq.$normalizedClass&select=id,score,completion_time_seconds,roll_no&limit=1")
            if (checkRes.isSuccess) {
                val array = JSONArray(checkRes.getOrThrow())
                if (array.length() > 0) {
                    val existingObj = array.getJSONObject(0)
                    val existingId = existingObj.optLong("id", submittedAtEpoch)
                    val existingScore = existingObj.optDouble("score", -1.0).toFloat()
                    val existingTime = existingObj.optLong("completion_time_seconds", Long.MAX_VALUE)

                    val patchJson = JSONObject().apply {
                        put("student_name", effectiveStudentName)
                        put("class_name", normalizedClass)
                        if (score > existingScore || (score == existingScore && completionTimeSeconds < existingTime)) {
                            put("score", score)
                            put("total_marks", totalMarks)
                            put("completion_time_seconds", completionTimeSeconds)
                            put("submitted_at", submittedAtEpoch)
                        }
                        put("attempts_count", attemptsCount)
                        if (!rollNo.isNullOrBlank()) put("roll_no", rollNo)
                    }.toString()
                    client.patch("leaderboard", "id=eq.$existingId", patchJson)
                    com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveLastSubmissionId(dateKey, existingId)
                    pruneLeaderboardBelowTop100(dateKey, normalizedClass)
                    return Result.success(existingId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pre-submission check notice: ${e.message}")
        }

        val fullJson = JSONObject().apply {
            put("student_id", studentId)
            put("student_name", effectiveStudentName)
            put("class_name", normalizedClass)
            put("date_key", dateKey)
            put("score", score)
            put("total_marks", totalMarks)
            put("accuracy", accuracy)
            put("completion_time_seconds", completionTimeSeconds)
            put("attempts_count", attemptsCount)
            put("submitted_at", submittedAtEpoch)
            if (!rollNo.isNullOrBlank()) put("roll_no", rollNo)
            put("course_name", normalizedClass)
        }

        val compatibleJson = JSONObject().apply {
            put("student_id", studentId)
            put("student_name", effectiveStudentName)
            put("class_name", normalizedClass)
            put("date_key", dateKey)
            put("score", score)
            put("total_marks", totalMarks)
            put("completion_time_seconds", completionTimeSeconds)
            put("attempts_count", attemptsCount)
            put("submitted_at", submittedAtEpoch)
            if (!rollNo.isNullOrBlank()) put("roll_no", rollNo)
        }

        // 2. Perform submission with retry and schema-resilient fallback
        var lastException: Throwable? = null
        val maxAttempts = 3
        for (attempt in 1..maxAttempts) {
            Log.d(TAG, "Submitting leaderboard entry (attempt $attempt of $maxAttempts)...")
            var res = client.post("leaderboard", fullJson.toString(), prefer = "return=representation")
            if (res.isFailure) {
                res = client.post("leaderboard", compatibleJson.toString(), prefer = "return=representation")
            }
            if (res.isSuccess) {
                val body = res.getOrThrow()
                var confirmedId = submittedAtEpoch
                try {
                    val array = JSONArray(body)
                    if (array.length() > 0) {
                        confirmedId = array.getJSONObject(0).optLong("id", confirmedId)
                    }
                } catch (_: Exception) {
                    try {
                        val obj = JSONObject(body)
                        confirmedId = obj.optLong("id", confirmedId)
                    } catch (_: Exception) {}
                }
                Log.d(TAG, "Result submission confirmed by server. Submission ID: $confirmedId")
                com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveLastSubmissionId(dateKey, confirmedId)
                pruneLeaderboardBelowTop100(dateKey, normalizedClass)
                return Result.success(confirmedId)
            } else {
                lastException = res.exceptionOrNull()
                Log.w(TAG, "Submission attempt $attempt failed: ${lastException?.message}")

                try {
                    val doubleCheck = client.get("leaderboard", "student_id=eq.$studentId&date_key=eq.$dateKey&class_name=eq.$normalizedClass&select=id&limit=1")
                    if (doubleCheck.isSuccess) {
                        val arr = JSONArray(doubleCheck.getOrThrow())
                        if (arr.length() > 0) {
                            val serverId = arr.getJSONObject(0).optLong("id", submittedAtEpoch)
                            Log.d(TAG, "Submission confirmed on server verification after blip. ID: $serverId")
                            com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveLastSubmissionId(dateKey, serverId)
                            pruneLeaderboardBelowTop100(dateKey, normalizedClass)
                            return Result.success(serverId)
                        }
                    }
                } catch (_: Exception) {}

                if (attempt < maxAttempts) {
                    val backoffMs = if (attempt == 1) 500L else 1200L
                    kotlinx.coroutines.delay(backoffMs)
                }
            }
        }

        return Result.failure(lastException ?: java.io.IOException("Failed to confirm result submission on server"))
    }

    /**
     * Loads remote application settings from Supabase `app_config` (or `settings` / `app_settings`) table.
     * Supports both key-value rows (`key`, `value`) and column-based rows (`privacy_policy_url`, `terms_conditions_url`, etc.).
     */
    suspend fun fetchAppConfig(): Result<Map<String, String>> {
        if (!SupabaseConfig.isConfigured) {
            return Result.failure(IllegalStateException("Supabase is not configured"))
        }
        val map = mutableMapOf<String, String>()
        val candidateTables = listOf("app_config", "app_settings", "settings")
        for (tableName in candidateTables) {
            val result = client.get(tableName, "select=*")
            if (result.isSuccess) {
                try {
                    val array = JSONArray(result.getOrThrow())
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val key = obj.optString("key", obj.optString("config_key", obj.optString("setting_key", "")))
                        val value = obj.optString("value", obj.optString("config_value", obj.optString("setting_value", "")))
                        if (key.isNotBlank() && value.isNotBlank()) {
                            map[key] = value
                        }
                        // Also extract direct column names if the table uses a single-row schema
                        val keysIter = obj.keys()
                        while (keysIter.hasNext()) {
                            val colName = keysIter.next()
                            if (colName != "id" && colName != "key" && colName != "value" && colName != "updated_at" && colName != "created_at") {
                                val colVal = obj.optString(colName, "")
                                if (colVal.isNotBlank() && colVal != "null") {
                                    if (!map.containsKey(colName)) {
                                        map[colName] = colVal
                                    }
                                }
                            }
                        }
                    }
                    if (map.isNotEmpty()) break
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing $tableName JSON: ${e.message}")
                }
            }
        }

        // Also check __system_app_config__ fallback row in notifications (used by Admin Panel when app_config RLS is read-only)
        try {
            val sysRes = client.get("notifications", "id=eq.__system_app_config__&select=message&limit=1")
            if (sysRes.isSuccess) {
                val arr = JSONArray(sysRes.getOrThrow())
                if (arr.length() > 0) {
                    val msg = arr.getJSONObject(0).optString("message", "")
                    if (msg.isNotBlank()) {
                        val cfgObj = JSONObject(msg)
                        val iter = cfgObj.keys()
                        while (iter.hasNext()) {
                            val k = iter.next()
                            val v = cfgObj.optString(k, "")
                            if (k.isNotBlank() && v.isNotBlank()) {
                                map[k] = v
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return Result.success(map)
    }

    /**
     * Loads dynamic Hero Promotional Banners from Supabase database (`banners`, `promotional_banners`, `hero_banners`, or `app_config`).
     */
    suspend fun fetchPromotionalBanners(classId: String? = null): Result<List<PromotionalBanner>> {
        if (!SupabaseConfig.isConfigured) {
            return Result.failure(IllegalStateException("Supabase is not configured"))
        }

        val candidateTables = listOf("promotional_banners", "banners", "hero_banners", "slider_banners")
        for (table in candidateTables) {
            val orderedRes = client.get(table, "select=*&order=display_order.asc,id.asc")
            val body = if (orderedRes.isSuccess) {
                orderedRes.getOrThrow()
            } else {
                val fallbackRes = client.get(table, "select=*")
                if (fallbackRes.isSuccess) fallbackRes.getOrThrow() else null
            }
            if (!body.isNullOrBlank()) {
                val parsed = parsePromotionalBannersJson(body, classId)
                if (parsed.isNotEmpty()) {
                    com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("banners_${classId ?: "all"}", body)
                    return Result.success(parsed)
                }
            }
        }

        // Also check if banners are configured inside app_config
        val configRes = fetchAppConfig()
        if (configRes.isSuccess) {
            val configMap = configRes.getOrThrow()
            val bannersJson = configMap["promotional_banners"]
                ?: configMap["banners"]
                ?: configMap["hero_banners"]
            if (!bannersJson.isNullOrBlank()) {
                val parsed = parsePromotionalBannersJson(bannersJson, classId)
                if (parsed.isNotEmpty()) {
                    com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("banners_${classId ?: "all"}", bannersJson)
                    return Result.success(parsed)
                }
            }
        }

        return Result.success(emptyList())
    }

    fun loadCachedPromotionalBanners(classId: String? = null): List<PromotionalBanner> {
        val raw = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getContentCache("banners_${classId ?: "all"}") ?: return emptyList()
        return parsePromotionalBannersJson(raw, classId)
    }

    private fun isLikelyImageUrl(candidate: String?): Boolean {
        val s = candidate?.trim()?.takeIf { it.isNotBlank() && it != "null" } ?: return false
        if (s.startsWith("data:image/", ignoreCase = true)) return true
        if (!s.startsWith("http://", ignoreCase = true) && !s.startsWith("https://", ignoreCase = true)) return false
        val lower = s.substringBefore('?').substringBefore('#').lowercase()
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
            lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") ||
            lower.endsWith(".avif") || lower.endsWith(".bmp")
        ) {
            return true
        }
        val fullLower = s.lowercase()
        return fullLower.contains("/storage/v1/object/") ||
            fullLower.contains("imgur.com") ||
            fullLower.contains("ibb.co") ||
            fullLower.contains("postimg.cc") ||
            fullLower.contains("cloudinary.com") ||
            fullLower.contains("unsplash.com") ||
            fullLower.contains("pexels.com") ||
            fullLower.contains("googleusercontent.com") ||
            fullLower.contains("drive.google.com/uc")
    }

    private fun parsePromotionalBannersJson(jsonString: String, classId: String?): List<PromotionalBanner> {
        val list = mutableListOf<Pair<Int, PromotionalBanner>>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isActive = obj.optBoolean("is_active", obj.optBoolean("active", obj.optBoolean("is_published", true)))
                if (!isActive) continue

                val rowClassId = obj.optString("class_id", "").trim()
                if (rowClassId.isNotBlank() &&
                    !rowClassId.equals("all", ignoreCase = true) &&
                    !classId.isNullOrBlank() &&
                    !rowClassId.equals(classId, ignoreCase = true)
                ) {
                    continue
                }

                val id = obj.optString("id", obj.optString("banner_id", "banner_${i + 1}"))
                val rawAction = obj.optString("action_type", obj.optString("actionType", obj.optString("type", obj.optString("action", "")))).uppercase().trim()
                var rawUrl = obj.optString("url", obj.optString("link", obj.optString("action_url", obj.optString("target_url", obj.optString("redirect_url", ""))))).trim().takeIf { it.isNotBlank() && it != "null" }
                var rawTargetId = obj.optString("target_id", obj.optString("targetId", obj.optString("target", obj.optString("page_id", "")))).trim().takeIf { it.isNotBlank() && it != "null" }

                // 1. Direct image_url column if present
                var imageUrl = obj.optString(
                    "image_url",
                    obj.optString("imageUrl", obj.optString("image", obj.optString("banner_image", obj.optString("bg_image", obj.optString("photo_url", "")))))
                ).trim().takeIf { it.isNotBlank() && it != "null" }

                var rawMetaText = obj.optString(
                    "meta_text",
                    obj.optString("metaText", obj.optString("meta", obj.optString("right_tag", obj.optString("subtitle_top", ""))))
                ).trim()

                // 2. Check if meta_text embeds [IMG:...] or is directly an image URL
                val imgTagRegex = Regex("""\[IMG:([^\]]+)]""", RegexOption.IGNORE_CASE)
                val metaImgMatch = imgTagRegex.find(rawMetaText)
                if (metaImgMatch != null) {
                    if (imageUrl.isNullOrBlank()) {
                        imageUrl = metaImgMatch.groupValues[1].trim()
                    }
                    rawMetaText = rawMetaText.replace(metaImgMatch.value, "").trim()
                } else if (imageUrl.isNullOrBlank() && (rawMetaText.startsWith("http://", ignoreCase = true) || rawMetaText.startsWith("https://", ignoreCase = true) || rawMetaText.startsWith("data:image/", ignoreCase = true))) {
                    imageUrl = rawMetaText
                    rawMetaText = ""
                }

                val rawBadge = obj.optString(
                    "badge_text",
                    obj.optString("badgeText", obj.optString("badge", obj.optString("tag", "")))
                ).trim()

                // 3. Check if action_type is IMAGE_BANNER / IMAGE or url points to an image
                val isImageAction = rawAction.contains("IMAGE") || rawBadge.equals("IMAGE", ignoreCase = true)
                if (imageUrl.isNullOrBlank() && !rawUrl.isNullOrBlank() && (isImageAction || isLikelyImageUrl(rawUrl))) {
                    imageUrl = rawUrl
                    // If target_id holds a click destination URL, promote it to rawUrl for click navigation
                    if (!rawTargetId.isNullOrBlank() && (rawTargetId.startsWith("http://", ignoreCase = true) || rawTargetId.startsWith("https://", ignoreCase = true))) {
                        rawUrl = rawTargetId
                    } else if (isImageAction) {
                        rawUrl = null
                    }
                }

                val actionType = when {
                    rawAction.contains("DAILY") || rawTargetId.equals("daily_test", ignoreCase = true) -> BannerActionType.OPEN_DAILY_TEST
                    rawAction.contains("VIDEO") || rawAction.contains("YOUTUBE") || (rawUrl?.contains("youtube.com", ignoreCase = true) == true) || (rawUrl?.contains("youtu.be", ignoreCase = true) == true) -> BannerActionType.OPEN_VIDEO
                    rawAction.contains("INTERNAL") || rawAction.contains("PAGE") || rawAction.contains("CATEGOR") ||
                        rawTargetId.equals("mock_tests", ignoreCase = true) || rawTargetId.equals("subject_wise", ignoreCase = true) || rawTargetId.equals("categories", ignoreCase = true) -> BannerActionType.OPEN_INTERNAL_PAGE
                    rawAction.contains("TEST") -> BannerActionType.OPEN_TEST
                    rawAction.contains("WEB") || rawAction.contains("URL") || rawAction.contains("LINK") || rawAction.contains("EXTERNAL") || rawAction.contains("TELEGRAM") -> BannerActionType.OPEN_WEBSITE
                    isImageAction && rawUrl.isNullOrBlank() -> BannerActionType.IMAGE_BANNER
                    !rawUrl.isNullOrBlank() && rawUrl != imageUrl -> BannerActionType.OPEN_WEBSITE
                    !imageUrl.isNullOrBlank() -> BannerActionType.IMAGE_BANNER
                    else -> BannerActionType.NONE
                }

                val isDailyChallenge = obj.optBoolean(
                    "is_daily_challenge",
                    obj.optBoolean(
                        "isDailyChallenge",
                        obj.optBoolean("daily_challenge", actionType == BannerActionType.OPEN_DAILY_TEST || id == "banner_daily_mcq")
                    )
                )

                val rawTitle = obj.optString(
                    "title",
                    obj.optString("heading", obj.optString("name", obj.optString("banner_title", "")))
                ).trim()

                // Allow blank title if imageUrl is provided (pure image banner)
                if (rawTitle.isBlank() && imageUrl.isNullOrBlank()) continue
                val title = if (rawTitle.equals("IMAGE", ignoreCase = true) && !imageUrl.isNullOrBlank()) "" else rawTitle

                val description = obj.optString(
                    "description",
                    obj.optString("subtitle", obj.optString("body", obj.optString("content", obj.optString("text", ""))))
                ).trim()

                val isPureImage = !imageUrl.isNullOrBlank() && title.isBlank() && description.isBlank()

                val badgeText = when {
                    rawBadge.equals("IMAGE", ignoreCase = true) && isPureImage -> ""
                    rawBadge.isNotBlank() -> rawBadge
                    isPureImage -> ""
                    isDailyChallenge -> "TODAY'S CHALLENGE"
                    else -> "FEATURED"
                }

                val rawButton = obj.optString(
                    "button_text",
                    obj.optString("buttonText", obj.optString("cta_text", obj.optString("cta", obj.optString("btn_text", ""))))
                ).trim()

                val buttonText = when {
                    rawButton.isNotBlank() -> rawButton
                    isPureImage -> null
                    isDailyChallenge -> "Start Test"
                    else -> "Explore"
                }

                val order = obj.optInt("display_order", obj.optInt("order_index", obj.optInt("order", i + 1)))

                list.add(
                    order to PromotionalBanner(
                        id = id,
                        isDailyChallenge = isDailyChallenge,
                        badgeText = badgeText,
                        metaText = rawMetaText,
                        title = title,
                        description = description,
                        buttonText = buttonText,
                        imageUrl = imageUrl,
                        gradientIndex = i,
                        actionType = actionType,
                        targetId = rawTargetId,
                        url = rawUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing promotional banners JSON: ${e.message}")
        }
        return list.sortedBy { it.first }.map { it.second }
    }

    /**
     * Saves or updates a promotional banner in Supabase using exact promotional_banners schema columns.
     */
    suspend fun saveOrUpdateBanner(banner: PromotionalBanner, classId: String? = null): Result<PromotionalBanner> {
        if (!SupabaseConfig.isConfigured) return Result.success(banner)
        val hasImg = !banner.imageUrl.isNullOrBlank()
        val effectiveMeta = if (hasImg && !banner.url.isNullOrBlank() && banner.url != banner.imageUrl) {
            if (banner.metaText.isBlank()) banner.imageUrl!! else "${banner.metaText} [IMG:${banner.imageUrl}]"
        } else {
            banner.metaText
        }
        val effectiveUrl = if (hasImg && (banner.url.isNullOrBlank() || banner.actionType == BannerActionType.IMAGE_BANNER)) {
            banner.imageUrl!!
        } else {
            banner.url ?: ""
        }
        val json = JSONObject().apply {
            put("id", banner.id)
            put("title", banner.title.ifBlank { if (hasImg) "IMAGE" else "Banner" })
            put("description", banner.description)
            put("badge_text", banner.badgeText.ifBlank { if (hasImg) "IMAGE" else "FEATURED" })
            put("meta_text", effectiveMeta)
            put("button_text", banner.buttonText ?: "")
            put("action_type", if (banner.isDailyChallenge) BannerActionType.OPEN_DAILY_TEST.name else banner.actionType.name)
            put("target_id", banner.targetId ?: if (banner.isDailyChallenge) "daily_test" else "")
            put("url", effectiveUrl)
            put("is_active", true)
        }
        val candidateTables = listOf("promotional_banners", "banners", "hero_banners", "home_banners")
        var lastError: Throwable? = null
        for (table in candidateTables) {
            val res = client.post(table, json.toString())
            if (res.isSuccess) {
                return Result.success(banner)
            } else {
                lastError = res.exceptionOrNull()
            }
        }
        return Result.failure(lastError ?: Exception("Failed to save banner"))
    }

    /**
     * Deletes a promotional banner from Supabase by ID.
     */
    suspend fun deleteBanner(bannerId: String): Result<Boolean> {
        if (!SupabaseConfig.isConfigured) return Result.success(true)
        val candidateTables = listOf("promotional_banners", "banners", "hero_banners", "home_banners")
        for (table in candidateTables) {
            val res = client.delete(table, "id=eq.$bannerId")
            if (res.isSuccess) {
                return Result.success(true)
            }
        }
        return Result.success(true)
    }

    /**
     * Loads notifications dynamically from Supabase database.
     */
    suspend fun fetchNotifications(): Result<List<NotificationItem>> {
        val result = client.get("notifications", "select=*&order=created_at.desc&limit=20")
        if (result.isFailure) {
            val fallback = client.get("notifications", "select=*")
            if (fallback.isFailure) {
                return Result.success(loadCachedNotifications())
            }
            val raw = fallback.getOrThrow()
            com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("notifications", raw)
            return Result.success(parseNotificationsJson(raw))
        }
        val raw = result.getOrThrow()
        com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.saveContentCache("notifications", raw)
        return Result.success(parseNotificationsJson(raw))
    }

    fun loadCachedNotifications(): List<NotificationItem> {
        val raw = com.jacboard.mcqmaster.util.UserPreferences.getInstanceOrNull()?.getContentCache("notifications") ?: return emptyList()
        return parseNotificationsJson(raw)
    }

    private fun parseNotificationsJson(jsonString: String): List<NotificationItem> {
        val list = mutableListOf<NotificationItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "n_$i")
                val type = obj.optString("type", "ANNOUNCEMENT")
                if (id == "__system_app_config__" || type == "SYSTEM_CONFIG") continue
                val title = obj.optString("title", "Announcement")
                val message = obj.optString("message", obj.optString("body", ""))
                val timestamp = obj.optString("timestamp", obj.optString("created_at", "Just now"))
                val isRead = obj.optBoolean("is_read", false)
                list.add(NotificationItem(id, title, message, timestamp, type, isRead))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing notifications JSON: ${e.message}")
        }
        return list
    }

    /**
     * Verifies live database connectivity by pinging the classes table.
     */
    suspend fun testConnection(): Result<Int> {
        val result = fetchClasses()
        return if (result.isSuccess) {
            Result.success(result.getOrThrow().size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Connection check failed"))
        }
    }

    companion object {
        private const val TAG = "SupabaseDataSource"
        private const val CAPTCHA_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val CAPTCHA_LENGTH = 6
        private const val CAPTCHA_TTL_MS = 2 * 60 * 1000L // 2 minutes
        private const val MAX_CAPTCHA_ATTEMPTS = 5
        private const val TOP_LEADERBOARD_LIMIT = 100
    }
}
