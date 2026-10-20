package com.jacboard.mcqmaster.util

/**
 * Dedicated Chemistry Speech Formatter for Android Text-to-Speech.
 *
 * Converts chemical formulas, subscripts, superscripts, ionic charges,
 * stoichiometric coefficients, states of matter, and reaction equations
 * into natural spoken English or Hindi without altering normal text.
 *
 * Examples:
 * - "H₂O" -> "H two O"
 * - "CO₂" -> "C O two"
 * - "NaCl" -> "N A C L"
 * - "H₂SO₄" -> "H two S O four"
 * - "CaCO₃" -> "C A C O three"
 * - "2H₂ + O₂ → 2H₂O" -> "Two H two plus O two produces two H two O"
 */
object ChemistrySpeechFormatter {

    // All valid IUPAC Periodic Table element symbols
    private val VALID_ELEMENTS = setOf(
        "H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne",
        "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca",
        "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn",
        "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr",
        "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn",
        "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd",
        "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb",
        "Lu", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg",
        "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Th",
        "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm",
        "Md", "No", "Lr", "Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds",
        "Rg", "Cn", "Nh", "Fl", "Mc", "Lv", "Ts", "Og"
    )

    // Single-element English words that should NOT be treated as chemical symbols unless they have subscripts/digits/charges
    private val ORDINARY_ENGLISH_WORDS = setOf(
        "He", "In", "As", "At", "No", "Am", "Be", "If", "On", "Or",
        "To", "Up", "Us", "We", "By", "Go", "Do", "Is", "It", "Me",
        "My", "Of", "So", "An", "Ah", "Oh", "Hi", "La", "Pa", "Ma"
    )

    private val SUBSCRIPT_MAP = mapOf(
        '₀' to '0', '₁' to '1', '₂' to '2', '₃' to '3', '₄' to '4',
        '₅' to '5', '₆' to '6', '₇' to '7', '₈' to '8', '₉' to '9'
    )

    private val SUPERSCRIPT_DIGIT_MAP = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '₇' to '7', '⁸' to '8', '⁹' to '9'
    )

    private val LETTER_HINDI_MAP = mapOf(
        'A' to "ए", 'B' to "बी", 'C' to "सी", 'D' to "डी", 'E' to "ई",
        'F' to "एफ", 'G' to "जी", 'H' to "एच", 'I' to "आई", 'J' to "जे",
        'K' to "के", 'L' to "एल", 'M' to "एम", 'N' to "एन", 'O' to "ओ",
        'P' to "पी", 'Q' to "क्यू", 'R' to "आर", 'S' to "एस", 'T' to "टी",
        'U' to "यू", 'V' to "वी", 'W' to "डब्ल्यू", 'X' to "एक्स", 'Y' to "वाई",
        'Z' to "ज़ेड"
    )

    fun format(input: String, isHindi: Boolean): String {
        if (input.isBlank()) return input
        var text = input

        // 1. States of matter in chemical equations: (aq), (s), (l), (g)
        if (isHindi) {
            text = text.replace(Regex("\\(aq\\)", RegexOption.IGNORE_CASE), " जलीय ")
            text = text.replace(Regex("\\(s\\)"), " ठोस ")
            text = text.replace(Regex("\\(l\\)"), " द्रव ")
            text = text.replace(Regex("\\(g\\)"), " गैस ")
        } else {
            text = text.replace(Regex("\\(aq\\)", RegexOption.IGNORE_CASE), " aqueous ")
            text = text.replace(Regex("\\(s\\)"), " solid ")
            text = text.replace(Regex("\\(l\\)"), " liquid ")
            text = text.replace(Regex("\\(g\\)"), " gas ")
        }

        // 2. Check if the text contains a chemical reaction arrow or chemical formulas around '+'
        val hasReactionArrow = text.contains("→") || text.contains("->") ||
            text.contains("⟶") || text.contains("⇒") ||
            text.contains("⇌") || text.contains("⇄") || text.contains("<->")

        // 3. Replace reaction arrows and symbols
        if (isHindi) {
            text = text.replace("⇌", " उत्क्रमणीय अभिक्रिया ")
            text = text.replace("⇄", " उत्क्रमणीय अभिक्रिया ")
            text = text.replace("<->", " उत्क्रमणीय अभिक्रिया ")
            text = text.replace("→", " से बनता है ")
            text = text.replace("⟶", " से बनता है ")
            text = text.replace("⇒", " से बनता है ")
            text = text.replace("->", " से बनता है ")
            text = text.replace("↑", " गैस निकलती है ")
            text = text.replace("↓", " अवक्षेप बनता है ")
        } else {
            text = text.replace("⇌", " is in equilibrium with ")
            text = text.replace("⇄", " is in equilibrium with ")
            text = text.replace("<->", " is in equilibrium with ")
            text = text.replace("→", " produces ")
            text = text.replace("⟶", " produces ")
            text = text.replace("⇒", " produces ")
            text = text.replace("->", " produces ")
            text = text.replace("↑", " gas evolved ")
            text = text.replace("↓", " precipitate ")
        }

        // 4. Token-level chemical formula recognition and expansion
        // Matches potential chemical tokens (optional leading stoichiometric coefficient + element/group/subscript/charge sequence)
        val formulaPattern = Regex("""(?<![A-Za-z])(\d*([A-Z][a-z]?|\([A-Za-z0-9₀-₉]+\))[0-9₀-₉]*)+([⁰¹²³⁴⁵⁶⁷⁸⁹]*[⁺⁻])?(?![A-Za-z])""")

        text = formulaPattern.replace(text) { matchResult ->
            val token = matchResult.value
            if (shouldFormatAsChemicalFormula(token, hasReactionArrow)) {
                val expanded = expandChemicalToken(token, isHindi)
                // Capitalize the very first word if this token is at the very start of the string
                if (matchResult.range.first == 0 && !isHindi && expanded.isNotEmpty()) {
                    expanded.replaceFirstChar { it.uppercase() }
                } else {
                    expanded
                }
            } else {
                token
            }
        }

        // 5. If this is a chemical reaction (e.g. "2H₂ + O₂ → 2H₂O"), convert '+' between chemical species to "plus" / "प्लस"
        if (hasReactionArrow) {
            text = if (isHindi) {
                text.replace(Regex("\\s*\\+\\s*"), " प्लस ")
            } else {
                text.replace(Regex("\\s*\\+\\s*"), " plus ")
            }
        }

        return text.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Determines whether a matched token is genuinely a chemical formula (e.g., H₂O, H2O, NaCl, CaCO₃, 2H₂, SO₄²⁻, Fe2O3)
     * rather than a normal word or a math variable like x² or 2A.
     */
    private fun shouldFormatAsChemicalFormula(token: String, inReactionContext: Boolean): Boolean {
        if (token.length < 2) return false
        if (token in ORDINARY_ENGLISH_WORDS) return false

        val hasSubscript = token.any { it in SUBSCRIPT_MAP }
        val hasIonicCharge = token.any { it == '⁺' || it == '⁻' }
        val hasParenthesesGroup = token.contains('(') && token.contains(')')

        // Strip leading coefficient and trailing ionic charge to inspect core elements
        var core = token.replaceFirst(Regex("^\\d+"), "")
        core = core.replace(Regex("[⁰¹²³⁴⁵⁶⁷⁸⁹]*[⁺⁻]$"), "")
        if (core.isEmpty()) return false

        // Parse core into element tokens and numbers
        val elements = mutableListOf<String>()
        var hasInternalOrTrailingNumber = false
        var idx = 0
        while (idx < core.length) {
            val ch = core[idx]
            when {
                ch == '(' || ch == ')' -> {
                    idx++
                }
                ch in 'A'..'Z' -> {
                    val elem = if (idx + 1 < core.length && core[idx + 1] in 'a'..'z') {
                        core.substring(idx, idx + 2).also { idx += 2 }
                    } else {
                        ch.toString().also { idx += 1 }
                    }
                    if (elem !in VALID_ELEMENTS) return false
                    elements.add(elem)
                }
                ch.isDigit() || ch in SUBSCRIPT_MAP -> {
                    hasInternalOrTrailingNumber = true
                    idx++
                }
                else -> return false
            }
        }

        if (elements.isEmpty()) return false

        // Case 1: Contains explicit Unicode subscript or ionic charge (e.g. H₂O, CO₂, H₂SO₄, CaCO₃, Na⁺, SO₄²⁻, 2H₂)
        if (hasSubscript || hasIonicCharge) return true

        // Case 2: Multi-element compound with >= 2 valid elements (e.g. NaCl, NaOH, HCl, CaO, MgO, KMnO4, H2SO4, CaCO3, CO2, H2O)
        if (elements.size >= 2) {
            // Avoid 2-letter all-caps abbreviations that are not chemical compounds unless in reaction or with numbers
            // Wait: "CO" in general text vs "CO" in chemistry; "NO" in English ("NO") vs chemistry
            if (!hasInternalOrTrailingNumber && token == "NO") return false
            if (!hasInternalOrTrailingNumber && token == "IN") return false
            if (!hasInternalOrTrailingNumber && token == "ON") return false
            if (!hasInternalOrTrailingNumber && token == "SO") return false
            if (!hasInternalOrTrailingNumber && token == "AS") return false
            if (!hasInternalOrTrailingNumber && token == "AT") return false
            if (!hasInternalOrTrailingNumber && token == "BE") return false
            if (!hasInternalOrTrailingNumber && token == "HE") return false
            if (!hasInternalOrTrailingNumber && token == "AM") return false
            if (!hasInternalOrTrailingNumber && token == "OR") return false
            if (!hasInternalOrTrailingNumber && token == "UP") return false
            if (!hasInternalOrTrailingNumber && token == "US") return false
            if (!hasInternalOrTrailingNumber && token == "BY") return false
            if (!hasInternalOrTrailingNumber && token == "IF") return false
            if (!hasInternalOrTrailingNumber && token == "IS") return false
            if (!hasInternalOrTrailingNumber && token == "IT") return false
            return true
        }

        // Case 3: Single element with subscript/number (e.g. O2, H2, N2, Cl2, O3, S8, 2H2, 2O2)
        if (elements.size == 1 && hasInternalOrTrailingNumber) {
            val elem = elements.first()
            if (elem in setOf("H", "O", "N", "F", "Cl", "Br", "I", "P", "S", "C") || inReactionContext) {
                return true
            }
        }

        // Case 4: Single element with leading coefficient in a reaction context (e.g. "2Na", "2Mg", "2Cu", "2Fe")
        if (elements.size == 1 && inReactionContext && token.first().isDigit()) {
            return true
        }

        return hasParenthesesGroup && hasInternalOrTrailingNumber
    }

    /**
     * Converts a verified chemical token (e.g. "2H₂O", "NaCl", "CaCO₃", "H₂SO₄", "SO₄²⁻")
     * into letter-by-letter + number speech.
     */
    private fun expandChemicalToken(token: String, isHindi: Boolean): String {
        val parts = mutableListOf<String>()
        var i = 0

        // 1. Optional leading stoichiometric coefficient (e.g. "2" in "2H₂O")
        val coeffBuilder = StringBuilder()
        while (i < token.length && token[i].isDigit()) {
            coeffBuilder.append(token[i])
            i++
        }
        if (coeffBuilder.isNotEmpty()) {
            parts.add(numberToWord(coeffBuilder.toString(), isHindi))
        }

        // 2. Main formula body
        while (i < token.length) {
            val ch = token[i]
            when {
                ch in 'A'..'Z' -> {
                    val symbol = if (i + 1 < token.length && token[i + 1] in 'a'..'z') {
                        token.substring(i, i + 2).also { i += 2 }
                    } else {
                        ch.toString().also { i += 1 }
                    }
                    // Spell each letter of the element symbol individually in uppercase ("Na" -> "N A", "Cl" -> "C L")
                    for (letter in symbol.uppercase()) {
                        if (isHindi) {
                            parts.add(LETTER_HINDI_MAP[letter] ?: letter.toString())
                        } else {
                            parts.add(letter.toString())
                        }
                    }
                }
                ch in SUBSCRIPT_MAP || ch.isDigit() -> {
                    val numBuilder = StringBuilder()
                    while (i < token.length && (token[i] in SUBSCRIPT_MAP || token[i].isDigit())) {
                        val mapped = SUBSCRIPT_MAP[token[i]] ?: token[i]
                        numBuilder.append(mapped)
                        i++
                    }
                    if (numBuilder.isNotEmpty()) {
                        parts.add(numberToWord(numBuilder.toString(), isHindi))
                    }
                }
                ch == '(' -> {
                    i++
                }
                ch == ')' -> {
                    i++
                    // Check if followed by subscript/multiplier e.g. Al₂(SO₄)₃ -> "whole thrice" / "three"
                    val multBuilder = StringBuilder()
                    while (i < token.length && (token[i] in SUBSCRIPT_MAP || token[i].isDigit())) {
                        val mapped = SUBSCRIPT_MAP[token[i]] ?: token[i]
                        multBuilder.append(mapped)
                        i++
                    }
                    if (multBuilder.isNotEmpty()) {
                        parts.add(numberToWord(multBuilder.toString(), isHindi))
                    }
                }
                ch in SUPERSCRIPT_DIGIT_MAP || ch == '⁺' || ch == '⁻' -> {
                    val chargeMagBuilder = StringBuilder()
                    while (i < token.length && token[i] in SUPERSCRIPT_DIGIT_MAP) {
                        chargeMagBuilder.append(SUPERSCRIPT_DIGIT_MAP[token[i]])
                        i++
                    }
                    var signChar: Char? = null
                    if (i < token.length && (token[i] == '⁺' || token[i] == '⁻')) {
                        signChar = token[i]
                        i++
                    }
                    if (chargeMagBuilder.isNotEmpty()) {
                        parts.add(numberToWord(chargeMagBuilder.toString(), isHindi))
                    }
                    if (signChar == '⁺') {
                        parts.add(if (isHindi) "प्लस आयन" else "plus")
                    } else if (signChar == '⁻') {
                        parts.add(if (isHindi) "माइनस आयन" else "minus")
                    }
                }
                else -> {
                    i++
                }
            }
        }

        return parts.joinToString(" ")
    }

    private fun numberToWord(numStr: String, isHindi: Boolean): String {
        val n = numStr.toIntOrNull() ?: return numStr
        return if (isHindi) {
            when (n) {
                1 -> "वन"
                2 -> "टू"
                3 -> "थ्री"
                4 -> "फोर"
                5 -> "फाइव"
                6 -> "सिक्स"
                7 -> "सेवन"
                8 -> "एट"
                9 -> "नाइन"
                10 -> "टेन"
                11 -> "इलेवन"
                12 -> "ट्वेल्व"
                else -> numStr
            }
        } else {
            when (n) {
                1 -> "one"
                2 -> "two"
                3 -> "three"
                4 -> "four"
                5 -> "five"
                6 -> "six"
                7 -> "seven"
                8 -> "eight"
                9 -> "nine"
                10 -> "ten"
                11 -> "eleven"
                12 -> "twelve"
                else -> numStr
            }
        }
    }
}
