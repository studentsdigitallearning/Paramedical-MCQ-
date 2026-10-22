package com.jacboard.mcqmaster.util

/**
 * Utility for canonical class normalization and single-filter class matching.
 * Ensures Class 8, Class 9, Class 10, Class 11, and Class 12 are strictly independent
 * and filtered by a single canonical class filter without course_name mismatch bugs.
 */
object CourseClassUtils {

    private val otWordRegex = Regex("\\bOT\\b")
    private val class8Regex = Regex("\\b8(TH)?\\b|CLASS[_\\s-]*8|EIGHTH")
    private val class9Regex = Regex("\\b9(TH)?\\b|CLASS[_\\s-]*9|NINTH")
    private val class10Regex = Regex("\\b10(TH)?\\b|CLASS[_\\s-]*10|TENTH")
    private val class11Regex = Regex("\\b11(TH)?\\b|CLASS[_\\s-]*11|ELEVENTH")
    private val class12Regex = Regex("\\b12(TH)?\\b|CLASS[_\\s-]*12|TWELFTH")

    val STANDARD_JAC_CLASSES = listOf(
        "Class 8",
        "Class 9",
        "Class 10",
        "Class 11",
        "Class 12"
    )

    /**
     * Normalizes any class name, class ID, or fallback string into its canonical display name:
     * "Class 8", "Class 9", "Class 10", "Class 11", "Class 12" (or specific course name).
     */
    fun normalizeClassName(className: String?, fallbackIdOrCourse: String? = null): String {
        val key = getCanonicalKey(className, fallbackIdOrCourse)
        return when (key) {
            "CLASS_8" -> "Class 8"
            "CLASS_9" -> "Class 9"
            "CLASS_10" -> "Class 10"
            "CLASS_11" -> "Class 11"
            "CLASS_12" -> "Class 12"
            "DMLT" -> "DMLT"
            "BMLT" -> "BMLT"
            "OT" -> "OT"
            "ANM" -> "ANM"
            "GNM" -> "GNM"
            "PARAMEDICAL" -> "Paramedical"
            else -> className?.trim()?.takeIf { it.isNotBlank() }
                ?: fallbackIdOrCourse?.trim()?.takeIf { it.isNotBlank() }
                ?: "Class 10"
        }
    }

    /**
     * Maps any class name or class ID to a standardized canonical key.
     * Prioritizes className strictly so a generic courseName (e.g. "JAC Board") never overrides the class.
     */
    fun getCanonicalKey(className: String?, courseName: String? = null): String {
        val fromClass = extractCanonicalFromText(className?.uppercase()?.trim().orEmpty())
        if (fromClass.isNotBlank()) return fromClass

        val fromCourse = extractCanonicalFromText(courseName?.uppercase()?.trim().orEmpty())
        if (fromCourse.isNotBlank()) return fromCourse

        return "CLASS_10"
    }

    private fun extractCanonicalFromText(upperText: String): String {
        if (upperText.isBlank()) return ""
        return when {
            upperText.contains("DMLT") -> "DMLT"
            upperText.contains("BMLT") -> "BMLT"
            otWordRegex.containsMatchIn(upperText) || upperText.contains("OPERATION THEATRE") -> "OT"
            upperText.contains("ANM") -> "ANM"
            upperText.contains("GNM") -> "GNM"
            upperText.contains("PARAMEDICAL") -> "PARAMEDICAL"
            class12Regex.containsMatchIn(upperText) -> "CLASS_12"
            class11Regex.containsMatchIn(upperText) -> "CLASS_11"
            class10Regex.containsMatchIn(upperText) -> "CLASS_10"
            class9Regex.containsMatchIn(upperText) -> "CLASS_9"
            class8Regex.containsMatchIn(upperText) -> "CLASS_8"
            upperText == "JAC BOARD" || upperText == "JAC" || upperText == "ALL COURSES" -> ""
            else -> {
                upperText.replace(" ", "_").replace(Regex("[^A-Z0-9_]"), "")
            }
        }
    }

    /**
     * Single-filter class comparison: checks strictly if two class identifiers belong to the same class
     * (e.g., Class 8, Class 9, Class 10, Class 11, Class 12).
     */
    fun isClassMatch(targetClass: String?, itemClass: String?): Boolean {
        val targetKey = getCanonicalKey(targetClass, null)
        val itemKey = getCanonicalKey(itemClass, null)
        return targetKey == itemKey
    }

    /**
     * Checks if two class combinations match using the single canonical class filter.
     * Ignores generic course labels (such as "JAC Board") so class leaderboards never disappear.
     */
    fun isMatch(
        targetClass: String?,
        targetCourse: String? = null,
        itemClass: String?,
        itemCourse: String? = null
    ): Boolean {
        val targetKey = getCanonicalKey(targetClass, targetCourse)
        val itemKey = getCanonicalKey(itemClass, itemCourse)
        return targetKey == itemKey
    }
}
