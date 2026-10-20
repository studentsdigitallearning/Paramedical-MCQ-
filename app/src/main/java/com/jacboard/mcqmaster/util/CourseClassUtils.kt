package com.jacboard.mcqmaster.util

/**
 * Utility for course and class matching, normalization, and filtering.
 * Ensures strict filtering across Class 8, 9, 10, 11, 12, DMLT, BMLT, OT, etc.
 */
object CourseClassUtils {

    /**
     * Maps any course name, class name, or class ID to a standardized canonical key.
     * Guaranteed to return distinct keys for different classes (Class 8, 9, 10, 11, 12)
     * and different professional courses (DMLT, BMLT, OT, ANM, GNM, Paramedical).
     */
    fun getCanonicalKey(className: String?, courseName: String? = null): String {
        val combined = "${courseName.orEmpty()} ${className.orEmpty()}".uppercase().trim()

        return when {
            combined.contains("DMLT") -> "DMLT"
            combined.contains("BMLT") -> "BMLT"
            combined.contains("OT") || combined.contains("OPERATION THEATRE") -> "OT"
            combined.contains("ANM") -> "ANM"
            combined.contains("GNM") -> "GNM"
            combined.contains("PARAMEDICAL") -> "PARAMEDICAL"
            combined.contains("12") || combined.contains("TWELFTH") -> "CLASS_12"
            combined.contains("11") || combined.contains("ELEVENTH") -> "CLASS_11"
            combined.contains("10") || combined.contains("TENTH") -> "CLASS_10"
            combined.contains("9") || combined.contains("NINTH") -> "CLASS_9"
            combined.contains("8") || combined.contains("EIGHTH") -> "CLASS_8"
            else -> {
                val clean = combined.replace(" ", "_").replace(Regex("[^A-Z0-9_]"), "")
                if (clean.isNotBlank()) clean else "CLASS_10"
            }
        }
    }

    /**
     * Checks if two course/class combinations match strictly.
     */
    fun isMatch(
        targetClass: String?,
        targetCourse: String?,
        itemClass: String?,
        itemCourse: String?
    ): Boolean {
        val targetKey = getCanonicalKey(targetClass, targetCourse)
        val itemKey = getCanonicalKey(itemClass, itemCourse)
        return targetKey == itemKey
    }
}
