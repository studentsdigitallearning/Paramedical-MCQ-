package com.jacboard.mcqmaster.util

import java.util.Locale

/**
 * Dedicated Mathematics Speech Formatter for Android Text-to-Speech.
 *
 * Examples:
 * - "2 + 3 = 5" -> "Two plus three equals five"
 * - "x²" -> "X squared"
 * - "x³" -> "X cubed"
 * - "√25" -> "Square root of twenty-five"
 * - "1/2" -> "One-half"
 * - "π" -> "Pi"
 * - "a > b" -> "A is greater than B"
 * - "(a+b)²" -> "Open bracket A plus B close bracket whole squared"
 */
object MathSpeechFormatter {

    private val SUPERSCRIPT_MAP = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '₇' to '7', '⁸' to '8', '⁹' to '9',
        '⁺' to '+', '⁻' to '-', 'ⁿ' to 'n'
    )

    fun format(input: String, isHindi: Boolean): String {
        if (input.isBlank()) return input
        return if (isHindi) {
            formatHindi(input)
        } else {
            formatEnglish(input)
        }
    }

    // =========================================================================
    // ENGLISH MATH FORMATTER
    // =========================================================================

    private fun formatEnglish(input: String): String {
        var text = input

        // 1. Scientific notation: e.g. 5 × 10⁻³ or 6.022 × 10²³ or 3 * 10^-8
        text = text.replace(Regex("(\\d+(?:\\.\\d+)?)\\s*[×*xX]\\s*10([⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻]+)")) { match ->
            val coeff = match.groupValues[1]
            val expRaw = match.groupValues[2].map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
            val expSpoken = formatSignedExponentEnglish(expRaw)
            "$coeff times ten to the power of $expSpoken"
        }
        text = text.replace(Regex("(\\d+(?:\\.\\d+)?)\\s*[×*xX]\\s*10\\^([+-]?\\d+)")) { match ->
            val coeff = match.groupValues[1]
            val expSpoken = formatSignedExponentEnglish(match.groupValues[2])
            "$coeff times ten to the power of $expSpoken"
        }
        text = text.replace(Regex("\\b10([⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻]+)")) { match ->
            val expRaw = match.groupValues[1].map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
            val expSpoken = formatSignedExponentEnglish(expRaw)
            "ten to the power of $expSpoken"
        }
        text = text.replace(Regex("\\b10\\^([+-]?\\d+)")) { match ->
            val expSpoken = formatSignedExponentEnglish(match.groupValues[1])
            "ten to the power of $expSpoken"
        }

        // 2. Bracketed expressions with powers: e.g. "(a+b)²" -> "Open bracket A plus B close bracket whole squared"
        text = text.replace(Regex("\\(([^()]+)\\)\\s*(?:²|\\^2)")) { match ->
            val inner = formatInnerBracketExpressionEnglish(match.groupValues[1])
            "Open bracket $inner close bracket whole squared"
        }
        text = text.replace(Regex("\\(([^()]+)\\)\\s*(?:³|\\^3)")) { match ->
            val inner = formatInnerBracketExpressionEnglish(match.groupValues[1])
            "Open bracket $inner close bracket whole cubed"
        }
        text = text.replace(Regex("\\(([^()]+)\\)\\s*(?:([⁰¹²³⁴⁵⁶⁷⁸⁹ⁿ]+)|\\^([0-9a-zA-Z+-]+))")) { match ->
            val inner = formatInnerBracketExpressionEnglish(match.groupValues[1])
            val sup = match.groupValues[2].ifEmpty { match.groupValues[3] }
                .map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
            "Open bracket $inner close bracket whole to the power of ${formatSignedExponentEnglish(sup)}"
        }

        // 3. Remaining algebraic brackets containing math operators: e.g. "(a + b)" or "(x - y)"
        text = text.replace(Regex("\\(([^()]*[+\\-×÷=<>²³√θπ][^()]*)\\)")) { match ->
            val inner = formatInnerBracketExpressionEnglish(match.groupValues[1])
            "open bracket $inner close bracket"
        }

        // 4. Trigonometry before general exponents
        text = text.replace("sin²θ", "sine squared theta")
        text = text.replace("cos²θ", "cosine squared theta")
        text = text.replace("tan²θ", "tan squared theta")
        text = text.replace("sec²θ", "secant squared theta")
        text = text.replace("cosec²θ", "cosecant squared theta")
        text = text.replace("cot²θ", "cotangent squared theta")
        text = text.replace("sinθ", "sine theta")
        text = text.replace("cosθ", "cosine theta")
        text = text.replace("tanθ", "tan theta")
        text = text.replace("secθ", "secant theta")
        text = text.replace("cosecθ", "cosecant theta")
        text = text.replace("cotθ", "cotangent theta")

        // 5. Variable & number powers: "x²" -> "X squared", "x³" -> "X cubed", "x^2" -> "X squared"
        text = text.replace(Regex("\\b([a-zA-Z])²")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} squared"
        }
        text = text.replace(Regex("\\b([a-zA-Z])³")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} cubed"
        }
        text = text.replace(Regex("\\b([a-zA-Z])\\^2\\b")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} squared"
        }
        text = text.replace(Regex("\\b([a-zA-Z])\\^3\\b")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} cubed"
        }
        text = text.replace(Regex("\\b([a-zA-Z])\\^([0-9a-zA-Z+-]+)")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} to the power of ${formatSignedExponentEnglish(m.groupValues[2])}"
        }
        text = text.replace(Regex("(\\d+)²")) { m ->
            "${numberToWordsEnglish(m.groupValues[1])} squared"
        }
        text = text.replace(Regex("(\\d+)³")) { m ->
            "${numberToWordsEnglish(m.groupValues[1])} cubed"
        }

        // Composite powers like x³y² or a²b²
        text = text.replace(Regex("([a-zA-Z])([²³⁴⁵⁶⁷⁸⁹ⁿ])")) { m ->
            val v = m.groupValues[1].uppercase(Locale.ROOT)
            val p = when (m.groupValues[2]) {
                "²" -> "squared"
                "³" -> "cubed"
                "ⁿ" -> "to the power n"
                else -> "to the power ${m.groupValues[2].map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")}"
            }
            " $v $p "
        }

        // Standalone superscripts
        text = text.replace("⁻¹", " inverse")
        text = text.replace("⁻²", " to the power minus two")
        text = text.replace("⁻³", " to the power minus three")
        text = text.replace("²", " squared")
        text = text.replace("³", " cubed")
        text = text.replace("⁴", " to the power four")
        text = text.replace("⁵", " to the power five")
        text = text.replace("ⁿ", " to the power n")

        // 6. Roots: "√25" -> "Square root of twenty-five", "∛27" -> "Cube root of twenty-seven"
        text = text.replace(Regex("∛\\(([^()]+)\\)")) { m ->
            "cube root of ${formatInnerBracketExpressionEnglish(m.groupValues[1])}"
        }
        text = text.replace(Regex("∛(\\d+)")) { m ->
            "cube root of ${numberToWordsEnglish(m.groupValues[1])}"
        }
        text = text.replace(Regex("√\\(([^()]+)\\)")) { m ->
            "square root of ${formatInnerBracketExpressionEnglish(m.groupValues[1])}"
        }
        text = text.replace(Regex("√(\\d+)/(\\d+)")) { m ->
            "square root of ${numberToWordsEnglish(m.groupValues[1])} divided by ${numberToWordsEnglish(m.groupValues[2])}"
        }
        text = text.replace(Regex("√(\\d+)")) { m ->
            "square root of ${numberToWordsEnglish(m.groupValues[1])}"
        }
        text = text.replace(Regex("√([a-zA-Z])\\b")) { m ->
            "square root of ${m.groupValues[1].uppercase(Locale.ROOT)}"
        }
        text = text.replace("∛", "cube root of ")
        text = text.replace("√", "square root of ")

        // 7. Fractions: "1/2" -> "One-half", "1/3" -> "one-third", "3/4" -> "three-fourths", general m/n
        text = text.replace(Regex("(?<![\\d/])1/2(?![\\d/])"), "one-half")
        text = text.replace(Regex("(?<![\\d/])1/3(?![\\d/])"), "one-third")
        text = text.replace(Regex("(?<![\\d/])2/3(?![\\d/])"), "two-thirds")
        text = text.replace(Regex("(?<![\\d/])1/4(?![\\d/])"), "one-fourth")
        text = text.replace(Regex("(?<![\\d/])3/4(?![\\d/])"), "three-fourths")
        text = text.replace(Regex("(?<![\\d/])1/5(?![\\d/])"), "one-fifth")
        text = text.replace(Regex("\\b(\\d+)/(\\d+)\\b")) { m ->
            "${numberToWordsEnglish(m.groupValues[1])} divided by ${numberToWordsEnglish(m.groupValues[2])}"
        }
        text = text.replace(Regex("\\b([a-zA-Z])/([a-zA-Z])\\b")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} divided by ${m.groupValues[2].uppercase(Locale.ROOT)}"
        }

        // 8. Comparisons & Relations: "a > b" -> "A is greater than B", "a < b" -> "A is less than B"
        text = text.replace(Regex("\\b([a-zA-Z])\\s*>\\s*([a-zA-Z])\\b")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} is greater than ${m.groupValues[2].uppercase(Locale.ROOT)}"
        }
        text = text.replace(Regex("\\b([a-zA-Z])\\s*<\\s*([a-zA-Z])\\b")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} is less than ${m.groupValues[2].uppercase(Locale.ROOT)}"
        }
        text = text.replace(Regex("\\b([a-zA-Z])\\s*=\\s*([a-zA-Z])\\b")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} equals ${m.groupValues[2].uppercase(Locale.ROOT)}"
        }
        text = text.replace(Regex("(\\d+)\\s*>\\s*(\\d+)")) { m ->
            "${numberToWordsEnglish(m.groupValues[1])} is greater than ${numberToWordsEnglish(m.groupValues[2])}"
        }
        text = text.replace(Regex("(\\d+)\\s*<\\s*(\\d+)")) { m ->
            "${numberToWordsEnglish(m.groupValues[1])} is less than ${numberToWordsEnglish(m.groupValues[2])}"
        }

        // 9. Arithmetic equations like "2 + 3 = 5" -> "Two plus three equals five"
        text = text.replace(Regex("\\b(\\d+)\\s*\\+\\s*(\\d+)\\s*=\\s*(\\d+)\\b")) { m ->
            "${numberToWordsEnglish(m.groupValues[1])} plus ${numberToWordsEnglish(m.groupValues[2])} equals ${numberToWordsEnglish(m.groupValues[3])}"
        }
        text = text.replace(Regex("\\b(\\d+)\\s*-\\s*(\\d+)\\s*=\\s*(\\d+)\\b")) { m ->
            "${numberToWordsEnglish(m.groupValues[1])} minus ${numberToWordsEnglish(m.groupValues[2])} equals ${numberToWordsEnglish(m.groupValues[3])}"
        }

        // 10. Geometry & Greek Symbols
        text = text.replace("△ABC", "triangle A B C")
        text = text.replace("△PQR", "triangle P Q R")
        text = text.replace("△", "triangle ")
        text = text.replace("∠ABC", "angle A B C")
        text = text.replace("∠A", "angle A")
        text = text.replace("∠B", "angle B")
        text = text.replace("∠C", "angle C")
        text = text.replace("∠", "angle ")
        text = text.replace("⊥", " is perpendicular to ")
        text = text.replace("∥", " is parallel to ")

        text = text.replace("π", " pi ")
        text = text.replace("θ", " theta ")
        text = text.replace("α", " alpha ")
        text = text.replace("β", " beta ")
        text = text.replace("γ", " gamma ")
        text = text.replace("λ", " lambda ")
        text = text.replace("∞", " infinity ")

        // 11. General mathematical operators
        text = text.replace("±", " plus or minus ")
        text = text.replace("≠", " is not equal to ")
        text = text.replace("≤", " is less than or equal to ")
        text = text.replace("≥", " is greater than or equal to ")
        text = text.replace("<=", " is less than or equal to ")
        text = text.replace(">=", " is greater than or equal to ")
        text = text.replace("!=", " is not equal to ")
        text = text.replace("×", " times ")
        text = text.replace("÷", " divided by ")
        text = text.replace("=", " equals ")
        text = text.replace("+", " plus ")
        text = text.replace(Regex("(?<=\\S)\\s*>\\s*(?=\\S)"), " is greater than ")
        text = text.replace(Regex("(?<=\\S)\\s*<\\s*(?=\\S)"), " is less than ")
        // Binary minus between numbers/variables (e.g. "x - y" or "5 - 2") without breaking hyphenated words like "one-half" or "twenty-five"
        text = text.replace(Regex("(?<=[0-9a-zA-Z²³])\\s+-\\s+(?=[0-9a-zA-Z])"), " minus ")
        text = text.replace(Regex("(?<=\\b[xXyYzZaAbBcC])-(?=[xXyYzZaAbBcC0-9]\\b)"), " minus ")

        text = text.replace(Regex("\\s+"), " ").trim()
        if (text.isNotEmpty() && input.first().let { it.isDigit() || it == '√' || it == '(' || it in 'a'..'z' }) {
            text = text.replaceFirstChar { it.uppercase(Locale.ROOT) }
        }
        return text
    }

    private fun formatInnerBracketExpressionEnglish(inner: String): String {
        var s = inner.trim()
        // Uppercase single-letter variables inside algebraic brackets so "(a+b)" -> "A plus B"
        s = s.replace(Regex("\\b([a-zA-Z])\\b")) { m ->
            m.groupValues[1].uppercase(Locale.ROOT)
        }
        s = s.replace("+", " plus ")
        s = s.replace("-", " minus ")
        s = s.replace("×", " times ")
        s = s.replace("*", " times ")
        s = s.replace("÷", " divided by ")
        s = s.replace("/", " divided by ")
        s = s.replace("²", " squared ")
        s = s.replace("³", " cubed ")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    private fun formatSignedExponentEnglish(exp: String): String {
        val trimmed = exp.trim()
        return when {
            trimmed.startsWith("-") -> "minus " + numberToWordsEnglish(trimmed.removePrefix("-"))
            trimmed.startsWith("+") -> numberToWordsEnglish(trimmed.removePrefix("+"))
            else -> numberToWordsEnglish(trimmed)
        }
    }

    private fun numberToWordsEnglish(numStr: String): String {
        val n = numStr.toIntOrNull() ?: return numStr
        val units = arrayOf(
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen"
        )
        val tens = arrayOf(
            "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
        )
        return when {
            n in 0..19 -> units[n]
            n in 20..99 -> {
                val t = tens[n / 10]
                val u = n % 10
                if (u == 0) t else "$t-${units[u]}"
            }
            n == 100 -> "one hundred"
            else -> numStr
        }
    }

    // =========================================================================
    // HINDI MATH FORMATTER
    // =========================================================================

    private fun formatHindi(input: String): String {
        var text = input

        // 1. Scientific notation
        text = text.replace(Regex("(\\d+(?:\\.\\d+)?)\\s*[×*xX]\\s*10([⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻]+)")) { match ->
            val coeff = match.groupValues[1]
            val expRaw = match.groupValues[2].map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
            val expSpoken = expRaw.replace("-", "माइनस ").replace("+", "")
            "$coeff गुणा 10 की घात $expSpoken"
        }
        text = text.replace(Regex("(\\d+(?:\\.\\d+)?)\\s*[×*xX]\\s*10\\^([+-]?\\d+)")) { match ->
            val coeff = match.groupValues[1]
            val expSpoken = match.groupValues[2].replace("-", "माइनस ").replace("+", "")
            "$coeff गुणा 10 की घात $expSpoken"
        }

        // 2. Bracketed expressions with powers: "(a+b)²" -> "ब्रैकेट शुरू A प्लस B ब्रैकेट बंद का होल स्क्वायर"
        text = text.replace(Regex("\\(([^()]+)\\)\\s*(?:²|\\^2)")) { match ->
            val inner = formatInnerBracketExpressionHindi(match.groupValues[1])
            "ब्रैकेट शुरू $inner ब्रैकेट बंद का होल वर्ग"
        }
        text = text.replace(Regex("\\(([^()]+)\\)\\s*(?:³|\\^3)")) { match ->
            val inner = formatInnerBracketExpressionHindi(match.groupValues[1])
            "ब्रैकेट शुरू $inner ब्रैकेट बंद का होल घन"
        }

        // 3. Remaining algebraic brackets containing math operators
        text = text.replace(Regex("\\(([^()]*[+\\-×÷=<>²³√θπ][^()]*)\\)")) { match ->
            val inner = formatInnerBracketExpressionHindi(match.groupValues[1])
            "ब्रैकेट शुरू $inner ब्रैकेट बंद"
        }

        // 4. Trigonometry
        text = text.replace("sin²θ", "साइन स्क्वायर थीटा")
        text = text.replace("cos²θ", "कॉस स्क्वायर थीटा")
        text = text.replace("tan²θ", "टैन स्क्वायर थीटा")
        text = text.replace("sec²θ", "सेक स्क्वायर थीटा")
        text = text.replace("cosec²θ", "कोसेक स्क्वायर थीटा")
        text = text.replace("cot²θ", "कॉट स्क्वायर थीटा")
        text = text.replace("sinθ", "साइन थीटा")
        text = text.replace("cosθ", "कॉस थीटा")
        text = text.replace("tanθ", "टैन थीटा")
        text = text.replace("secθ", "सेक थीटा")
        text = text.replace("cosecθ", "कोसेक थीटा")
        text = text.replace("cotθ", "कॉट थीटा")

        // 5. Powers & Exponents
        text = text.replace(Regex("\\b([a-zA-Z])²")) { m -> "${m.groupValues[1].uppercase(Locale.ROOT)} का वर्ग" }
        text = text.replace(Regex("\\b([a-zA-Z])³")) { m -> "${m.groupValues[1].uppercase(Locale.ROOT)} का घन" }
        text = text.replace(Regex("\\b([a-zA-Z])\\^2\\b")) { m -> "${m.groupValues[1].uppercase(Locale.ROOT)} का वर्ग" }
        text = text.replace(Regex("\\b([a-zA-Z])\\^3\\b")) { m -> "${m.groupValues[1].uppercase(Locale.ROOT)} का घन" }
        text = text.replace("⁻¹", " इनवर्स")
        text = text.replace("⁻²", " की घात माइनस 2")
        text = text.replace("⁻³", " की घात माइनस 3")
        text = text.replace("²", " का वर्ग")
        text = text.replace("³", " का घन")
        text = text.replace("⁴", " की घात 4")
        text = text.replace("⁵", " की घात 5")
        text = text.replace("ⁿ", " की घात n")

        // 6. Roots
        text = text.replace(Regex("∛(\\d+)")) { m -> "घनमूल ${m.groupValues[1]}" }
        text = text.replace(Regex("√(\\d+)/(\\d+)")) { m -> "वर्गमूल ${m.groupValues[1]} बटा ${m.groupValues[2]}" }
        text = text.replace(Regex("√(\\d+)")) { m -> "वर्गमूल ${m.groupValues[1]}" }
        text = text.replace("∛", "घनमूल ")
        text = text.replace("√", "वर्गमूल ")

        // 7. Fractions
        text = text.replace(Regex("(?<![\\d/])1/2(?![\\d/])"), "एक बटा दो")
        text = text.replace(Regex("(?<![\\d/])1/3(?![\\d/])"), "एक बटा तीन")
        text = text.replace(Regex("(?<![\\d/])2/3(?![\\d/])"), "दो बटा तीन")
        text = text.replace(Regex("(?<![\\d/])1/4(?![\\d/])"), "एक बटा चार")
        text = text.replace(Regex("(?<![\\d/])3/4(?![\\d/])"), "तीन बटा चार")
        text = text.replace(Regex("\\b(\\d+)/(\\d+)\\b")) { m -> "${m.groupValues[1]} बटा ${m.groupValues[2]}" }
        text = text.replace(Regex("\\b([a-zA-Z])/([a-zA-Z])\\b")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} बटा ${m.groupValues[2].uppercase(Locale.ROOT)}"
        }

        // 8. Comparisons
        text = text.replace(Regex("\\b([a-zA-Z])\\s*>\\s*([a-zA-Z])\\b")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} बड़ा है ${m.groupValues[2].uppercase(Locale.ROOT)} से"
        }
        text = text.replace(Regex("\\b([a-zA-Z])\\s*<\\s*([a-zA-Z])\\b")) { m ->
            "${m.groupValues[1].uppercase(Locale.ROOT)} छोटा है ${m.groupValues[2].uppercase(Locale.ROOT)} से"
        }

        // 9. Geometry & Greek Symbols
        text = text.replace("△ABC", "त्रिभुज A B C")
        text = text.replace("△", "त्रिभुज ")
        text = text.replace("∠ABC", "कोण A B C")
        text = text.replace("∠", "कोण ")
        text = text.replace("⊥", " लंबवत है ")
        text = text.replace("∥", " समांतर है ")
        text = text.replace("π", " पाई ")
        text = text.replace("θ", " थीटा ")
        text = text.replace("α", " अल्फा ")
        text = text.replace("β", " बीटा ")
        text = text.replace("γ", " गामा ")
        text = text.replace("λ", " लैम्ब्डा ")
        text = text.replace("∞", " अनंत ")

        // 10. Operators
        text = text.replace("±", " प्लस या माइनस ")
        text = text.replace("≠", " बराबर नहीं है ")
        text = text.replace("≤", " छोटा या बराबर है ")
        text = text.replace("≥", " बड़ा या बराबर है ")
        text = text.replace("×", " गुणा ")
        text = text.replace("÷", " भाग ")
        text = text.replace("=", " बराबर ")
        text = text.replace("+", " प्लस ")
        text = text.replace(Regex("(?<=\\S)\\s*>\\s*(?=\\S)"), " बड़ा है ")
        text = text.replace(Regex("(?<=\\S)\\s*<\\s*(?=\\S)"), " छोटा है ")
        text = text.replace(Regex("(?<=[0-9a-zA-Z²³])\\s+-\\s+(?=[0-9a-zA-Z])"), " माइनस ")
        text = text.replace(Regex("(?<=\\b[xXyYzZaAbBcC])-(?=[xXyYzZaAbBcC0-9]\\b)"), " माइनस ")

        return text.replace(Regex("\\s+"), " ").trim()
    }

    private fun formatInnerBracketExpressionHindi(inner: String): String {
        var s = inner.trim()
        s = s.replace(Regex("\\b([a-zA-Z])\\b")) { m -> m.groupValues[1].uppercase(Locale.ROOT) }
        s = s.replace("+", " प्लस ")
        s = s.replace("-", " माइनस ")
        s = s.replace("×", " गुणा ")
        s = s.replace("*", " गुणा ")
        s = s.replace("÷", " भाग ")
        s = s.replace("/", " बटा ")
        s = s.replace("²", " का वर्ग ")
        s = s.replace("³", " का घन ")
        return s.replace(Regex("\\s+"), " ").trim()
    }
}
