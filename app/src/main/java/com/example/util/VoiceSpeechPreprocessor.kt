package com.example.util

import java.util.Locale

/**
 * Dedicated speech-normalization layer for educational MCQs.
 * Normalizes mathematical formulas, scientific notations, chemical reactions,
 * units, medical/paramedical terminology, and abbreviations into natural, clear speech.
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

        if (isHindi) {
            processed = normalizeHindiMixedContent(processed)
            processed = normalizeChemicalFormulasHindi(processed)
            processed = normalizeMathHindi(processed)
            processed = normalizeUnitsHindi(processed)
            processed = normalizeAbbreviationsHindi(processed)
        } else {
            processed = normalizeChemicalReactions(processed)
            processed = normalizeChemicalFormulasEnglish(processed)
            processed = normalizeMathEnglish(processed)
            processed = normalizeUnitsEnglish(processed)
            processed = normalizeMedicalTermsEnglish(processed)
            processed = normalizeAbbreviationsEnglish(processed)
        }

        // Clean up excess whitespaces and trailing symbols
        processed = processed.replace(Regex("\\s+"), " ").trim()
        return processed
    }

    // =========================================================================
    // ENGLISH NORMALIZATIONS
    // =========================================================================

    private fun normalizeChemicalReactions(input: String): String {
        var text = input

        // Chemical reaction arrows
        text = text.replace("⇌", " is in equilibrium with ")
        text = text.replace("⇄", " is in equilibrium with ")
        text = text.replace("→", " produces ")
        text = text.replace("->", " produces ")

        // Common full reactions
        text = text.replace("2H₂ + O₂ → 2H₂O", "Two H two plus O two produces two H two O")
        text = text.replace("H₂ + O₂ → H₂O", "H two plus O two produces water")
        text = text.replace("CaCO₃ → CaO + CO₂", "C a C O three produces C a O plus C O two")
        text = text.replace("CH₄ + 2O₂ → CO₂ + 2H₂O", "C H four plus two O two produces C O two plus two H two O")

        return text
    }

    private fun normalizeChemicalFormulasEnglish(input: String): String {
        var text = input

        // Specific chemical compounds
        val compounds = listOf(
            "H₂SO₄" to "H two S O four",
            "H₂O" to "H two O",
            "CO₂" to "C O two",
            "NaCl" to "N a C l",
            "NaOH" to "N a O H",
            "CaCO₃" to "C a C O three",
            "CaO" to "C a O",
            "HCl" to "H C l",
            "HNO₃" to "H N O three",
            "CH₄" to "C H four",
            "CuSO₄" to "C u S O four",
            "FeSO₄" to "F e S O four",
            "KMnO₄" to "K M n O four",
            "AgNO₃" to "A g N O three",
            "NaHCO₃" to "N a H C O three",
            "Na₂CO₃" to "N a two C O three",
            "KOH" to "K O H",
            "KCl" to "K C l",
            "O₂" to "O two",
            "H₂" to "H two",
            "N₂" to "N two",
            "Cl₂" to "C l two",
            "CO" to "C O",
            "NH₃" to "N H three"
        )

        for ((formula, spoken) in compounds) {
            text = text.replace(formula, spoken)
        }

        return text
    }

    private fun normalizeMathEnglish(input: String): String {
        var text = input

        // Scientific notation: e.g. 5 × 10⁻³ -> "five times ten to the power of minus three"
        text = text.replace(Regex("(\\d+)\\s*[×*]\\s*10⁻(\\d+)")) { match ->
            val base = match.groupValues[1]
            val power = match.groupValues[2]
            "$base times ten to the power of minus $power"
        }
        text = text.replace(Regex("(\\d+)\\s*[×*]\\s*10\\^([+-]?\\d+)")) { match ->
            val base = match.groupValues[1]
            val power = match.groupValues[2].replace("+", "").replace("-", "minus ")
            "$base times ten to the power of $power"
        }

        // Composite power expressions (common in algebra questions)
        text = text.replace("x³y²", "x cubed y squared")
        text = text.replace("x²y²", "x squared y squared")
        text = text.replace("xy³", "x y cubed")
        text = text.replace("x³y³", "x cubed y cubed")

        // Superscript exponents
        text = text.replace("²", " squared")
        text = text.replace("³", " cubed")
        text = text.replace("⁴", " to the power 4")
        text = text.replace("⁵", " to the power 5")
        text = text.replace("ⁿ", " to the power n")
        text = text.replace("⁻¹", " inverse")
        text = text.replace("⁻²", " to the power minus 2")
        text = text.replace("⁻³", " to the power minus 3")

        // Square roots & radicals
        text = text.replace("√25", "square root of twenty-five")
        text = text.replace("√16", "square root of sixteen")
        text = text.replace("√9", "square root of nine")
        text = text.replace("√4", "square root of four")
        text = text.replace("√3/2", "square root of three divided by two")
        text = text.replace("√3", "square root of three")
        text = text.replace("√2", "square root of two")
        text = text.replace("√", "square root of ")

        // Fractions
        text = text.replace(Regex("\\b1/2\\b"), "one-half")
        text = text.replace(Regex("\\b1/3\\b"), "one-third")
        text = text.replace(Regex("\\b2/3\\b"), "two-thirds")
        text = text.replace(Regex("\\b1/4\\b"), "one-fourth")
        text = text.replace(Regex("\\b3/4\\b"), "three-fourths")
        text = text.replace(Regex("\\ba/b\\b"), "a divided by b")
        text = text.replace(Regex("\\bx/y\\b"), "x divided by y")

        // Common math equations like 2 + 2 = 4
        text = text.replace(Regex("(\\d+)\\s*\\+\\s*(\\d+)\\s*=\\s*(\\d+)")) { match ->
            "${match.groupValues[1]} plus ${match.groupValues[2]} equals ${match.groupValues[3]}"
        }

        // Trigonometry
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
        text = text.replace("cos(A + B)", "cosine of A plus B")
        text = text.replace("sin(A + B)", "sine of A plus B")

        // Geometry symbols
        text = text.replace("△ABC", "triangle A B C")
        text = text.replace("△", "triangle ")
        text = text.replace("∠ABC", "angle A B C")
        text = text.replace("∠A", "angle A")
        text = text.replace("∠B", "angle B")
        text = text.replace("∠C", "angle C")
        text = text.replace("∠", "angle ")

        // Mathematical symbols & relations
        text = text.replace("±", " plus or minus ")
        text = text.replace("≠", " is not equal to ")
        text = text.replace("≤", " is less than or equal to ")
        text = text.replace("≥", " is greater than or equal to ")
        text = text.replace("×", " times ")
        text = text.replace("÷", " divided by ")
        text = text.replace("π", " pi ")
        text = text.replace("θ", " theta ")
        text = text.replace("α", " alpha ")
        text = text.replace("β", " beta ")
        text = text.replace("λ", " lambda ")
        text = text.replace("μ", " micro ")
        text = text.replace("∞", " infinity ")

        return text
    }

    private fun normalizeUnitsEnglish(input: String): String {
        var text = input

        // Pressure & Medical units
        text = text.replace(Regex("(\\d+)\\s*mmHg\\b", RegexOption.IGNORE_CASE), "$1 millimeters of mercury")

        // Temperature
        text = text.replace(Regex("(\\d+)\\s*°C\\b"), "$1 degrees Celsius")
        text = text.replace(Regex("(\\d+)\\s*°F\\b"), "$1 degrees Fahrenheit")
        text = text.replace(Regex("(\\d+)\\s*°\\b"), "$1 degrees")

        // Mass & Volume
        text = text.replace(Regex("(\\d+)\\s*mg\\b", RegexOption.IGNORE_CASE), "$1 milligrams")
        text = text.replace(Regex("(\\d+)\\s*mL\\b"), "$1 milliliters")
        text = text.replace(Regex("(\\d+)\\s*ml\\b"), "$1 milliliters")
        text = text.replace(Regex("(\\d+)\\s*kg\\b", RegexOption.IGNORE_CASE), "$1 kilograms")
        text = text.replace(Regex("(\\d+)\\s*g\\b"), "$1 grams")
        text = text.replace(Regex("(\\d+)\\s*μg\\b"), "$1 micrograms")
        text = text.replace(Regex("(\\d+)\\s*dL\\b", RegexOption.IGNORE_CASE), "$1 deciliters")

        // Length & Speed
        text = text.replace(Regex("(\\d+)\\s*km/h\\b", RegexOption.IGNORE_CASE), "$1 kilometers per hour")
        text = text.replace(Regex("(\\d+)\\s*m/s\\b", RegexOption.IGNORE_CASE), "$1 meters per second")
        text = text.replace(Regex("(\\d+)\\s*km\\b", RegexOption.IGNORE_CASE), "$1 kilometers")
        text = text.replace(Regex("(\\d+)\\s*cm\\b", RegexOption.IGNORE_CASE), "$1 centimeters")
        text = text.replace(Regex("(\\d+)\\s*mm\\b", RegexOption.IGNORE_CASE), "$1 millimeters")
        text = text.replace(Regex("(\\d+)\\s*m\\b"), "$1 meters")

        // Percentage
        text = text.replace(Regex("(\\d+)\\s*%"), "$1 percent")

        return text
    }

    private fun normalizeMedicalTermsEnglish(input: String): String {
        var text = input
        // Ensure proper capitalization and spacing for medical & paramedical terms
        val medicalPronunciations = mapOf(
            "Westergren" to "Westergren",
            "Hemoglobin" to "Hemoglobin",
            "Erythrocyte" to "Erythrocyte",
            "Leukocyte" to "Leukocyte",
            "Platelet" to "Platelet",
            "Thrombosis" to "Thrombosis",
            "Bilirubin" to "Bilirubin",
            "Microbiology" to "Microbiology",
            "Pathology" to "Pathology",
            "Hematology" to "Hematology",
            "Biochemistry" to "Biochemistry"
        )
        for ((term, pron) in medicalPronunciations) {
            text = text.replace(Regex("\\b$term\\b", RegexOption.IGNORE_CASE), pron)
        }
        return text
    }

    private fun normalizeAbbreviationsEnglish(input: String): String {
        var text = input

        val abbreviations = mapOf(
            "DMLT" to "D M L T",
            "BMLT" to "B M L T",
            "OT" to "O T",
            "ESR" to "E S R",
            "CBC" to "C B C",
            "RBC" to "R B C",
            "WBC" to "W B C",
            "DNA" to "D N A",
            "RNA" to "R N A",
            "ATP" to "A T P",
            "ECG" to "E C G",
            "EEG" to "E E G",
            "ICU" to "I C U",
            "PCR" to "P C R",
            "ELISA" to "E L I S A",
            "WHO" to "W H O",
            "HCF" to "H C F",
            "LCM" to "L C M"
        )

        for ((abbr, spoken) in abbreviations) {
            text = text.replace(Regex("\\b$abbr\\b"), spoken)
        }

        return text
    }

    // =========================================================================
    // HINDI NORMALIZATIONS & MIXED CONTENT
    // =========================================================================

    private fun normalizeHindiMixedContent(input: String): String {
        var text = input

        // Common English medical/scientific words in Hindi questions:
        // Convert to smooth Hindi phonetic words so Hindi TTS speaks them naturally
        val phoneticMap = mapOf(
            "Hemoglobin" to "हीमोग्लोबिन",
            "hemoglobin" to "हीमोग्लोबिन",
            "Erythrocyte" to "इरिथ्रोसाइट",
            "Leukocyte" to "ल्यूकोसाइट",
            "Platelet" to "प्लेटलेट",
            "Platelets" to "प्लेटलेट्स",
            "Thrombosis" to "थ्रॉम्बोसिस",
            "Bilirubin" to "बिलीरुबिन",
            "Westergren" to "वेस्टेरग्रेन",
            "Microbiology" to "माइक्रोबायोलॉजी",
            "Pathology" to "पैथोलॉजी",
            "Hematology" to "हेमेटोलॉजी",
            "Biochemistry" to "बायोकेमिस्ट्री",
            "DMLT" to "डी एम एल टी",
            "BMLT" to "बी एम एल टी",
            "OT" to "ओ टी",
            "CBC" to "सी बी सी",
            "RBC" to "आर बी सी",
            "WBC" to "डब्ल्यू बी सी",
            "ESR" to "ई एस आर",
            "DNA" to "डी एन ए",
            "RNA" to "आर एन ए",
            "ATP" to "ए टी पी",
            "ECG" to "ई सी जी",
            "ICU" to "आई सी यू",
            "PCR" to "पी सी आर",
            "HCF" to "एच सी एफ",
            "LCM" to "एल सी एम"
        )

        for ((word, hindiPhonetic) in phoneticMap) {
            text = text.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), hindiPhonetic)
        }

        return text
    }

    private fun normalizeChemicalFormulasHindi(input: String): String {
        var text = input

        text = text.replace("2H₂ + O₂ → 2H₂O", "टू एच टू प्लस ओ टू बनाता है टू एच टू ओ")
        text = text.replace("H₂ + O₂ → H₂O", "एच टू प्लस ओ टू बनाता है जल")
        text = text.replace("CaCO₃ → CaO + CO₂", "सी ए सी ओ थ्री बनाता है सी ए ओ प्लस सी ओ टू")
        text = text.replace("→", " बनाता है ")
        text = text.replace("⇌", " साम्यावस्था में है ")

        text = text.replace("H₂SO₄", "एच टू एस ओ फोर")
        text = text.replace("H₂O", "एच टू ओ")
        text = text.replace("CO₂", "सी ओ टू")
        text = text.replace("NaCl", "एन ए सी एल")
        text = text.replace("NaOH", "एन ए ओ एच")
        text = text.replace("CaCO₃", "सी ए सी ओ थ्री")
        text = text.replace("CaO", "सी ए ओ")
        text = text.replace("HCl", "एच सी एल")
        text = text.replace("O₂", "ओ टू")
        text = text.replace("H₂", "एच टू")
        text = text.replace("N₂", "एन टू")
        text = text.replace("Cl₂", "सी एल टू")

        return text
    }

    private fun normalizeMathHindi(input: String): String {
        var text = input

        text = text.replace("x³y²", "x का घन y का वर्ग")
        text = text.replace("x²y²", "x का वर्ग y का वर्ग")
        text = text.replace("xy³", "x y का घन")
        text = text.replace("x³y³", "x का घन y का घन")
        text = text.replace("x²", "x का वर्ग")
        text = text.replace("y²", "y का वर्ग")
        text = text.replace("z²", "z का वर्ग")
        text = text.replace("a²", "a का वर्ग")
        text = text.replace("b²", "b का वर्ग")
        text = text.replace("c²", "c का वर्ग")
        text = text.replace("r²", "r का वर्ग")
        text = text.replace("x³", "x का घन")
        text = text.replace("y³", "y का घन")
        text = text.replace("x⁴", "x की घात 4")

        text = text.replace("²", " का वर्ग")
        text = text.replace("³", " का घन")
        text = text.replace("⁴", " की घात 4")

        text = text.replace("√25", "25 का वर्गमूल")
        text = text.replace("√3/2", "वर्गमूल 3 बटे 2")
        text = text.replace("√3", "वर्गमूल 3")
        text = text.replace("√2", "वर्गमूल 2")
        text = text.replace("√", "वर्गमूल ")

        text = text.replace(Regex("\\b1/2\\b"), "एक बटे दो")
        text = text.replace(Regex("\\b1/3\\b"), "एक बटे तीन")
        text = text.replace(Regex("\\b2/3\\b"), "दो बटे तीन")
        text = text.replace(Regex("\\b1/4\\b"), "एक बटे चार")
        text = text.replace(Regex("\\b3/4\\b"), "तीन बटे चार")
        text = text.replace(Regex("\\ba/b\\b"), "a बटे b")

        text = text.replace("△ABC", "त्रिभुज A B C")
        text = text.replace("△", "त्रिभुज ")
        text = text.replace("∠ABC", "कोण A B C")
        text = text.replace("∠", "कोण ")

        text = text.replace("±", " प्लस या माइनस ")
        text = text.replace("≠", " बराबर नहीं है ")
        text = text.replace("≤", " छोटा या बराबर है ")
        text = text.replace("≥", " बड़ा या बराबर है ")
        text = text.replace("°", " डिग्री ")
        text = text.replace("π", " पाई ")
        text = text.replace("θ", " थीटा ")
        text = text.replace("α", " अल्फा ")
        text = text.replace("β", " बीटा ")

        return text
    }

    private fun normalizeUnitsHindi(input: String): String {
        var text = input

        text = text.replace(Regex("(\\d+)\\s*mmHg\\b", RegexOption.IGNORE_CASE), "$1 मिलीमीटर ऑफ मरकरी")
        text = text.replace(Regex("(\\d+)\\s*°C\\b"), "$1 डिग्री सेल्सियस")
        text = text.replace(Regex("(\\d+)\\s*°F\\b"), "$1 डिग्री फ़ारेनहाइट")
        text = text.replace(Regex("(\\d+)\\s*mg\\b", RegexOption.IGNORE_CASE), "$1 मिलीग्राम")
        text = text.replace(Regex("(\\d+)\\s*mL\\b", RegexOption.IGNORE_CASE), "$1 मिलीलीटर")
        text = text.replace(Regex("(\\d+)\\s*kg\\b", RegexOption.IGNORE_CASE), "$1 किलोग्राम")
        text = text.replace(Regex("(\\d+)\\s*cm\\b", RegexOption.IGNORE_CASE), "$1 सेंटीमीटर")
        text = text.replace(Regex("(\\d+)\\s*mm\\b", RegexOption.IGNORE_CASE), "$1 मिलीमीटर")
        text = text.replace(Regex("(\\d+)\\s*km\\b", RegexOption.IGNORE_CASE), "$1 किलोमीटर")
        text = text.replace(Regex("(\\d+)\\s*%"), "$1 प्रतिशत")

        return text
    }

    private fun normalizeAbbreviationsHindi(input: String): String {
        var text = input

        text = text.replace("HCF", "महत्तम समापवर्तक")
        text = text.replace("LCM", "लघुत्तम समापवर्त्य")

        return text
    }
}
