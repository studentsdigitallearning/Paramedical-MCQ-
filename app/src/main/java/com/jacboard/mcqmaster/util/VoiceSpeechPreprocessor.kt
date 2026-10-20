package com.jacboard.mcqmaster.util

/**
 * Dedicated speech-normalization layer for educational MCQs.
 * Coordinates ChemistrySpeechFormatter and MathSpeechFormatter so that
 * questions and options are spoken clearly and accurately in Indian English or Indian Hindi
 * while reading ONLY what the user wrote.
 */
object VoiceSpeechPreprocessor {

    /**
     * Main entry point to normalize text for Text-To-Speech.
     * @param text The input question or option text
     * @param isHindi Whether the target language is Hindi
     */
    fun preprocessForTts(text: String, isHindi: Boolean): String {
        if (text.isBlank()) return ""

        var processed = text.trim()

        // 1. Format chemical formulas, subscripts, ions, and reactions first
        processed = ChemistrySpeechFormatter.format(processed, isHindi)

        // 2. Format mathematical expressions, powers, roots, fractions, brackets, and equations
        processed = MathSpeechFormatter.format(processed, isHindi)

        // 3. Format common scientific units attached to numbers (e.g., 37°C, 100 km/h, 50%)
        processed = if (isHindi) {
            normalizeUnitsHindi(processed)
        } else {
            normalizeUnitsEnglish(processed)
        }

        // Clean up excess whitespaces
        processed = processed.replace(Regex("\\s+"), " ").trim()
        return processed
    }

    private fun normalizeUnitsEnglish(input: String): String {
        var text = input
        text = text.replace(Regex("(\\d+)\\s*mmHg\\b", RegexOption.IGNORE_CASE), "$1 millimeters of mercury")
        text = text.replace(Regex("(\\d+)\\s*°C\\b"), "$1 degrees Celsius")
        text = text.replace(Regex("(\\d+)\\s*°F\\b"), "$1 degrees Fahrenheit")
        text = text.replace(Regex("(\\d+)\\s*°"), "$1 degrees")
        text = text.replace(Regex("(\\d+)\\s*km/h\\b", RegexOption.IGNORE_CASE), "$1 kilometers per hour")
        text = text.replace(Regex("(\\d+)\\s*m/s\\b", RegexOption.IGNORE_CASE), "$1 meters per second")
        text = text.replace(Regex("(\\d+)\\s*%"), "$1 percent")
        return text
    }

    private fun normalizeUnitsHindi(input: String): String {
        var text = input
        text = text.replace(Regex("(\\d+)\\s*mmHg\\b", RegexOption.IGNORE_CASE), "$1 मिलीमीटर ऑफ मरकरी")
        text = text.replace(Regex("(\\d+)\\s*°C\\b"), "$1 डिग्री सेल्सियस")
        text = text.replace(Regex("(\\d+)\\s*°F\\b"), "$1 डिग्री फ़ारेनहाइट")
        text = text.replace(Regex("(\\d+)\\s*°"), "$1 डिग्री")
        text = text.replace(Regex("(\\d+)\\s*km/h\\b", RegexOption.IGNORE_CASE), "$1 किलोमीटर प्रति घंटा")
        text = text.replace(Regex("(\\d+)\\s*m/s\\b", RegexOption.IGNORE_CASE), "$1 मीटर प्रति सेकंड")
        text = text.replace(Regex("(\\d+)\\s*%"), "$1 प्रतिशत")
        return text
    }
}
