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
    ): Result<com.example.model.DailyRollNoResult> {
        // 1. First, check if this student ALREADY has a participant record for today
        // Rule: Each student can receive only ONE temporary Roll Number per day across all courses.
        val existingQuery = if (studentName.isNotBlank() && studentName != "Student") {
            "or=(student_id.eq.$studentId,student_name.eq.$studentName)&date_key=eq.$dateKey&select=*"
        } else {
            "student_id=eq.$studentId&date_key=eq.$dateKey&select=*"
        }
        val checkRes = client.get("daily_participants", existingQuery)
        if (checkRes.isSuccess) {
            try {
                val array = JSONArray(checkRes.getOrThrow())
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val rollNo = obj.optString("roll_no", "")
                    val hasAttempted = obj.optBoolean("has_attempted", false)
                    val existingCourse = obj.optString("course_name", courseName)
                    val existingClass = obj.optString("class_name", className)
                    val score = obj.optDouble("score", 0.0).toFloat()
                    val totalMarks = obj.optDouble("total_marks", 50.0).toFloat()
                    if (rollNo.isNotBlank()) {
                        return Result.success(
                            com.example.model.DailyRollNoResult(
                                rollNo = rollNo,
                                alreadyAttempted = hasAttempted,
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
        val leadQuery = if (studentName.isNotBlank() && studentName != "Student") {
            "or=(student_id.eq.$studentId,student_name.eq.$studentName)&date_key=eq.$dateKey&select=*"
        } else {
            "student_id=eq.$studentId&date_key=eq.$dateKey&select=*"
        }
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
                        // Ensure backfilled into daily_participants
                        val ensurePayload = JSONObject().apply {
                            put("student_id", studentId)
                            put("student_name", studentName)
                            put("course_name", courseName)
                            put("class_name", className)
                            put("roll_no", rollNo)
                            put("date_key", dateKey)
                            put("has_attempted", true)
                            put("score", score.toDouble())
                            put("total_marks", totalMarks.toDouble())
                        }.toString()
                        client.post("daily_participants", ensurePayload)

                        return Result.success(
                            com.example.model.DailyRollNoResult(
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

        // 3. Try server-side RPC procedure
        val payload = JSONObject().apply {
            put("p_student_id", studentId)
            put("p_student_name", studentName)
            put("p_course_name", courseName)
            put("p_class_name", className)
            put("p_date_key", dateKey)
        }.toString()

        val rpcRes = client.post("rpc/get_or_create_daily_roll_no", payload)
        if (rpcRes.isSuccess) {
            try {
                val json = JSONObject(rpcRes.getOrThrow())
                val roll = json.optString("roll_no", "")
                if (roll.isNotBlank()) {
                    return Result.success(
                        com.example.model.DailyRollNoResult(
                            rollNo = roll,
                            alreadyAttempted = json.optBoolean("already_attempted", false),
                            courseName = json.optString("course_name", courseName),
                            className = json.optString("class_name", className),
                            score = json.optDouble("score", 0.0).toFloat(),
                            totalMarks = json.optDouble("total_marks", 50.0).toFloat(),
                            isNew = json.optBoolean("is_new", true)
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing roll no RPC response: ${e.message}")
            }
        }

        // 4. Robust Fallback: Scan existing roll numbers from both daily_participants and leaderboard
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

        var candidateSeq = maxSeq + 1
        var finalRollNo = com.example.util.DailyTestTimeUtils.generateTemporaryRollNo(dateKey, candidateSeq)

        // Insert new participant record with collision retry loop (up to 5 attempts)
        for (attempt in 1..5) {
            finalRollNo = com.example.util.DailyTestTimeUtils.generateTemporaryRollNo(dateKey, candidateSeq)
            val insertPayload = JSONObject().apply {
                put("student_id", studentId)
                put("student_name", studentName)
                put("course_name", courseName)
                put("class_name", className)
                put("roll_no", finalRollNo)
                put("date_key", dateKey)
                put("has_attempted", false)
                put("score", 0.0)
                put("total_marks", 50.0)
            }.toString()

            val insertRes = client.post("daily_participants", insertPayload)
            if (insertRes.isSuccess) {
                break
            } else {
                val errMsg = insertRes.exceptionOrNull()?.message ?: ""
                // If collision on (student_id, date_key), fetch existing
                if (errMsg.contains("uq_daily_participants_student_date", ignoreCase = true)) {
                    val fetchExisting = client.get("daily_participants", "student_id=eq.$studentId&date_key=eq.$dateKey&select=*")
                    if (fetchExisting.isSuccess) {
                        try {
                            val arr = JSONArray(fetchExisting.getOrThrow())
                            if (arr.length() > 0) {
                                val obj = arr.getJSONObject(0)
                                val roll = obj.optString("roll_no", "")
                                if (roll.isNotBlank()) {
                                    return Result.success(
                                        com.example.model.DailyRollNoResult(
                                            rollNo = roll,
                                            alreadyAttempted = obj.optBoolean("has_attempted", false),
                                            courseName = obj.optString("course_name", courseName),
                                            className = obj.optString("class_name", className),
                                            score = obj.optDouble("score", 0.0).toFloat(),
                                            totalMarks = obj.optDouble("total_marks", 50.0).toFloat(),
                                            isNew = false
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
                // Otherwise roll number collision, advance sequence and retry
                candidateSeq++
            }
        }

        return Result.success(
            com.example.model.DailyRollNoResult(
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
     * Resets daily participant session state at 12:00 AM IST.
     * All historical leaderboard entries, scores, and attempts are preserved permanently.
     */
    suspend fun resetDailyParticipants(currentDateKey: String): Result<Boolean> {
        // Do NOT delete historical leaderboard or attempt records.
        // We preserve all historical user records for student history and analytics.
        return Result.success(true)
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
    ): Result<com.example.model.DailyEligibilityResult> {
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
                            com.example.model.DailyEligibilityResult(
                                isEligible = false,
                                reason = "You have already completed today's Daily Test. Students are allowed only one attempt per day across all courses.",
                                attemptedClassId = itemClass,
                                attemptedClassName = "$itemCourse • $itemClass",
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
                        com.example.model.DailyEligibilityResult(
                            isEligible = false,
                            reason = "You have already completed today's Daily Test. Students are allowed only one attempt per day across all courses.",
                            attemptedClassId = itemClass,
                            attemptedClassName = if (itemCourse.isNotBlank()) "$itemCourse • $itemClass" else itemClass,
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
            com.example.model.DailyEligibilityResult(
                isEligible = true,
                attemptsCount = 0
            )
        )
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

                val rollNo = obj.optString("roll_no", "").ifBlank {
                    com.example.util.DailyTestTimeUtils.generateTemporaryRollNo(
                        com.example.util.DailyTestTimeUtils.getCurrentDateKey(),
                        i + 1
                    )
                }

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
                        avatarInitials = initials,
                        rollNo = rollNo
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing leaderboard JSON: ${e.message}")
        }

        // Zero duplicate data: Deduplicate by Roll Number, Student ID, and Student Name
        val bestEntries = mutableListOf<DailyLeaderboardEntry>()
        for (entry in rawList) {
            val existingIndex = bestEntries.indexOfFirst { existing ->
                (!entry.rollNo.isNullOrBlank() && !existing.rollNo.isNullOrBlank() && entry.rollNo == existing.rollNo) ||
                (entry.studentId.isNotBlank() && entry.studentId == existing.studentId) ||
                (entry.studentName.isNotBlank() && entry.studentName != "Student" && entry.studentName.equals(existing.studentName, ignoreCase = true))
            }
            if (existingIndex < 0) {
                bestEntries.add(entry)
            } else {
                val existing = bestEntries[existingIndex]
                val isBetterScore = entry.score > existing.score
                val isSameScoreFaster = entry.score == existing.score && entry.completionTimeSeconds < existing.completionTimeSeconds
                val mergedRoll = if (!entry.rollNo.isNullOrBlank()) entry.rollNo else existing.rollNo
                if (isBetterScore || isSameScoreFaster) {
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

        // Rank priority: 1. Highest Score, 2. Fastest completion time (Rule 3)
        return bestEntries
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
     * Prevents duplicate data rows for the same student or roll number on the same day.
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
        courseName: String? = null
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
            if (!rollNo.isNullOrBlank()) put("roll_no", rollNo)
            if (!courseName.isNullOrBlank()) put("course_name", courseName)
            put("submitted_at", System.currentTimeMillis())
        }.toString()

        // Check if an entry for this student or roll number already exists on this date to prevent duplicate data
        val checkQuery = if (!rollNo.isNullOrBlank()) {
            "or=(roll_no.eq.$rollNo,student_id.eq.$studentId)&date_key=eq.$dateKey&select=id,score,completion_time_seconds"
        } else {
            "student_id=eq.$studentId&date_key=eq.$dateKey&select=id,score,completion_time_seconds"
        }
        val existingRes = client.get("leaderboard", checkQuery)
        var existingRowId: Int? = null
        if (existingRes.isSuccess) {
            try {
                val arr = JSONArray(existingRes.getOrThrow())
                if (arr.length() > 0) {
                    val rowId = arr.getJSONObject(0).optInt("id", -1)
                    if (rowId > 0) existingRowId = rowId
                }
            } catch (_: Exception) {}
        }

        val postResult = if (existingRowId != null) {
            // Update existing row so no duplicate rows are created in leaderboard table
            client.patch("leaderboard", "id=eq.$existingRowId", payload)
        } else {
            client.post("leaderboard", payload)
        }

        // Also log/update into daily_participants table to mark attempt completed
        val partPayload = JSONObject().apply {
            put("score", score.toDouble())
            put("total_marks", totalMarks.toDouble())
            put("has_attempted", true)
        }.toString()
        client.patch("daily_participants", "student_id=eq.$studentId&date_key=eq.$dateKey", partPayload)

        client.post("daily_attempts", payload)
        if (postResult.isFailure && existingRowId == null) {
            client.post("test_results", payload)
        }
        return Result.success(true)
    }

    companion object {
        private const val TAG = "SupabaseDataSource"
    }
}
