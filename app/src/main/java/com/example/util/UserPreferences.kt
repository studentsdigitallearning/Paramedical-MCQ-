package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Student
import java.util.UUID

/**
 * Local persistent storage for student identity, course, class,
 * and daily test roll number state.
 *
 * Ensures profile remains available across app restarts without requiring login.
 */
class UserPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getStudentId(): String {
        var id = prefs.getString(KEY_STUDENT_ID, null)
        if (id.isNullOrBlank()) {
            id = "std_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString(KEY_STUDENT_ID, id).apply()
        }
        return id
    }

    fun hasSelectedCourseAndClass(): Boolean {
        val classId = prefs.getString(KEY_CLASS_ID, null)
        return !classId.isNullOrBlank() && prefs.getBoolean(KEY_SETUP_DONE, false)
    }

    fun isProfileCompleted(): Boolean {
        val classId = prefs.getString(KEY_CLASS_ID, null)
        return !classId.isNullOrBlank() && prefs.getBoolean(KEY_SETUP_DONE, false)
    }

    fun saveStudentName(name: String) {
        if (name.isNotBlank()) {
            prefs.edit().putString(KEY_STUDENT_NAME, name.trim()).apply()
        }
    }

    fun saveCourseAndClass(courseName: String, classId: String, className: String) {
        val studentId = getStudentId()
        prefs.edit()
            .putString(KEY_STUDENT_ID, studentId)
            .putString(KEY_COURSE_NAME, courseName.trim())
            .putString(KEY_CLASS_ID, classId.trim())
            .putString(KEY_CLASS_NAME, className.trim())
            .putBoolean(KEY_SETUP_DONE, true)
            .apply()
    }

    fun saveProfile(name: String, courseName: String, classId: String, className: String) {
        val studentId = getStudentId()
        val editor = prefs.edit()
            .putString(KEY_STUDENT_ID, studentId)
            .putString(KEY_COURSE_NAME, courseName.trim())
            .putString(KEY_CLASS_ID, classId.trim())
            .putString(KEY_CLASS_NAME, className.trim())
            .putBoolean(KEY_SETUP_DONE, true)
        if (name.isNotBlank()) {
            editor.putString(KEY_STUDENT_NAME, name.trim())
        }
        editor.apply()
    }

    fun updateClass(courseName: String, classId: String, className: String) {
        prefs.edit()
            .putString(KEY_COURSE_NAME, courseName.trim())
            .putString(KEY_CLASS_ID, classId.trim())
            .putString(KEY_CLASS_NAME, className.trim())
            .apply()
    }

    fun getStudentName(): String = prefs.getString(KEY_STUDENT_NAME, "Student") ?: "Student"

    fun getCourseName(): String = prefs.getString(KEY_COURSE_NAME, "Class 10") ?: "Class 10"

    fun getSelectedClassId(): String = prefs.getString(KEY_CLASS_ID, "class_10") ?: "class_10"

    fun getSelectedClassName(): String = prefs.getString(KEY_CLASS_NAME, "Class 10") ?: "Class 10"

    fun loadStudent(): Student {
        val id = getStudentId()
        val name = getStudentName()
        val classId = getSelectedClassId()
        val course = getCourseName()
        return Student(
            id = id,
            name = name,
            email = "",
            selectedClassId = classId,
            courseName = course,
            level = 1,
            xp = prefs.getInt(KEY_XP, 0),
            streakDays = prefs.getInt(KEY_STREAK, 0)
        )
    }

    fun saveXpAndStreak(xp: Int, streak: Int) {
        prefs.edit()
            .putInt(KEY_XP, xp)
            .putInt(KEY_STREAK, streak)
            .apply()
    }

    // Daily Test One-Attempt-Per-Day & Temporary Roll Number State
    fun getTodayRollNo(dateKey: String): String? {
        val savedDate = prefs.getString(KEY_DAILY_DATE_KEY, null)
        if (savedDate != null && savedDate != dateKey) {
            resetDailyStateIfNewDay(dateKey)
            return null
        }
        return if (savedDate == dateKey) {
            prefs.getString(KEY_DAILY_ROLL_NO, null)
        } else {
            null
        }
    }

    fun saveTodayRollNo(dateKey: String, rollNo: String) {
        prefs.edit()
            .putString(KEY_DAILY_DATE_KEY, dateKey)
            .putString(KEY_DAILY_ROLL_NO, rollNo)
            .apply()
    }

    fun hasAttemptedDailyTestToday(dateKey: String): Boolean {
        val savedDate = prefs.getString(KEY_DAILY_DATE_KEY, null)
        val attempted = prefs.getBoolean(KEY_DAILY_ATTEMPTED, false)
        if (savedDate != null && savedDate != dateKey) {
            resetDailyStateIfNewDay(dateKey)
            return false
        }
        return savedDate == dateKey && attempted
    }

    fun resetDailyStateIfNewDay(currentDateKey: String) {
        val savedDate = prefs.getString(KEY_DAILY_DATE_KEY, null)
        if (savedDate != currentDateKey) {
            prefs.edit()
                .putString(KEY_DAILY_DATE_KEY, currentDateKey)
                .remove(KEY_DAILY_ROLL_NO)
                .putBoolean(KEY_DAILY_ATTEMPTED, false)
                .remove(KEY_DAILY_ATTEMPTED_COURSE)
                .remove(KEY_DAILY_ATTEMPTED_CLASS)
                .remove(KEY_DAILY_ATTEMPTED_SCORE)
                .remove(KEY_DAILY_ATTEMPTED_TOTAL)
                .apply()
        }
    }

    fun markDailyTestAttempted(
        dateKey: String,
        rollNo: String,
        attemptedCourse: String,
        attemptedClass: String,
        score: Float,
        totalMarks: Float
    ) {
        prefs.edit()
            .putString(KEY_DAILY_DATE_KEY, dateKey)
            .putString(KEY_DAILY_ROLL_NO, rollNo)
            .putString(KEY_DAILY_ATTEMPTED_COURSE, attemptedCourse)
            .putString(KEY_DAILY_ATTEMPTED_CLASS, attemptedClass)
            .putFloat(KEY_DAILY_ATTEMPTED_SCORE, score)
            .putFloat(KEY_DAILY_ATTEMPTED_TOTAL, totalMarks)
            .putBoolean(KEY_DAILY_ATTEMPTED, true)
            .apply()
    }

    fun getDailyAttemptDetails(dateKey: String): DailySavedAttemptDetails? {
        val savedDate = prefs.getString(KEY_DAILY_DATE_KEY, null)
        val attempted = prefs.getBoolean(KEY_DAILY_ATTEMPTED, false)
        if (savedDate == dateKey && attempted) {
            return DailySavedAttemptDetails(
                dateKey = dateKey,
                rollNo = prefs.getString(KEY_DAILY_ROLL_NO, "") ?: "",
                courseName = prefs.getString(KEY_DAILY_ATTEMPTED_COURSE, "") ?: "",
                className = prefs.getString(KEY_DAILY_ATTEMPTED_CLASS, "") ?: "",
                score = prefs.getFloat(KEY_DAILY_ATTEMPTED_SCORE, 0f),
                totalMarks = prefs.getFloat(KEY_DAILY_ATTEMPTED_TOTAL, 50f)
            )
        }
        return null
    }

    companion object {
        private const val PREFS_NAME = "mcq_student_local_profile_prefs"

        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_STUDENT_NAME = "student_name"
        private const val KEY_COURSE_NAME = "course_name"
        private const val KEY_CLASS_ID = "class_id"
        private const val KEY_CLASS_NAME = "class_name"
        private const val KEY_SETUP_DONE = "setup_completed"
        private const val KEY_XP = "student_xp"
        private const val KEY_STREAK = "student_streak"

        private const val KEY_DAILY_DATE_KEY = "daily_date_key"
        private const val KEY_DAILY_ROLL_NO = "daily_roll_no"
        private const val KEY_DAILY_ATTEMPTED = "daily_attempted"
        private const val KEY_DAILY_ATTEMPTED_COURSE = "daily_attempted_course"
        private const val KEY_DAILY_ATTEMPTED_CLASS = "daily_attempted_class"
        private const val KEY_DAILY_ATTEMPTED_SCORE = "daily_attempted_score"
        private const val KEY_DAILY_ATTEMPTED_TOTAL = "daily_attempted_total"

        @Volatile
        private var instance: UserPreferences? = null

        fun init(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context).also { instance = it }
            }
        }

        fun getInstance(): UserPreferences {
            return instance ?: throw IllegalStateException("UserPreferences must be initialized first")
        }

        fun getInstanceOrNull(): UserPreferences? = instance
    }
}

data class DailySavedAttemptDetails(
    val dateKey: String,
    val rollNo: String,
    val courseName: String,
    val className: String,
    val score: Float,
    val totalMarks: Float
)
