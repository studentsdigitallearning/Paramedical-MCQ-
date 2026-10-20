package com.jacboard.mcqmaster.util

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class QuizTtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var isInitialized = false

    var isSpeaking by mutableStateOf(false)
        private set

    var isPaused by mutableStateOf(false)
        private set

    // Current cached payload for resume support
    private var currentQuestionText: String = ""
    private var currentOptions: List<String> = emptyList()
    private var currentIsHindi: Boolean = false
    private var currentGender: String = "FEMALE"
    private var lastFinalUtterancePrefix: String = "opt_d_"

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                isPaused = false
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId != null &&
                    (utteranceId.startsWith(lastFinalUtterancePrefix) ||
                        utteranceId.startsWith("opt_d_") ||
                        utteranceId.startsWith("preview_"))
                ) {
                    isSpeaking = false
                    isPaused = false
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                isPaused = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking = false
                isPaused = false
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
        }
    }

    /**
     * Finds and configures the highest-quality installed voice available matching
     * Indian English (`en-IN`) or Indian Hindi (`hi-IN`) and the selected Male/Female gender.
     * Never downloads large voice packages automatically and handles unavailable voices gracefully.
     */
    private fun configureVoice(isHindi: Boolean, gender: String) {
        val targetLocale = Locale.forLanguageTag(if (isHindi) "hi-IN" else "en-IN")
        val isMale = gender.equals("MALE", ignoreCase = true)

        try {
            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallbackLocale = if (isHindi) Locale.forLanguageTag("hi") else Locale.UK
                val fbResult = tts?.setLanguage(fallbackLocale)
                if (fbResult == TextToSpeech.LANG_MISSING_DATA || fbResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
            }

            // Natural educational speech pace
            tts?.setSpeechRate(0.94f)

            // Subtle natural pitch distinction when engine shares base voice
            val pitch = if (isMale) 0.90f else 1.03f
            tts?.setPitch(pitch)

            val allVoices = tts?.voices
            if (!allVoices.isNullOrEmpty()) {
                val matchedVoice = findBestVoice(allVoices, isHindi, isMale)
                if (matchedVoice != null) {
                    tts?.voice = matchedVoice
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore and rely on standard default without crashing
        }
    }

    private fun isVoiceMatchingGender(voice: Voice, isMale: Boolean): Boolean {
        val name = voice.name.lowercase(Locale.ROOT)
        val features = voice.features?.map { it.lowercase(Locale.ROOT) } ?: emptyList()

        if (isMale) {
            if (features.any { it.contains("male") && !it.contains("female") }) return true
            return name.contains("#male") ||
                (name.contains("male") && !name.contains("female")) ||
                name.contains("-enc") ||
                name.contains("-end") ||
                name.contains("-enm") ||
                name.contains("-hid") ||
                name.contains("-hie") ||
                name.contains("-tpd") ||
                name.contains("-iom") ||
                name.contains("-man")
        } else {
            if (features.any { it.contains("female") }) return true
            return name.contains("#female") ||
                name.contains("female") ||
                name.contains("-ena") ||
                name.contains("-enb") ||
                name.contains("-ene") ||
                name.contains("-cfl") ||
                name.contains("-cfn") ||
                name.contains("-hia") ||
                name.contains("-hib") ||
                name.contains("-hic") ||
                name.contains("-sfg") ||
                name.contains("-fem")
        }
    }

    private fun findBestVoice(voices: Set<Voice>, isHindi: Boolean, isMale: Boolean): Voice? {
        val langCode = if (isHindi) "hi" else "en"

        // Filter to installed voices for the target language (do not trigger network downloads of missing packs)
        val installedLangVoices = voices.filter { voice ->
            voice.locale.language.equals(langCode, ignoreCase = true) &&
                (voice.features == null || !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
        }.sortedWith(
            compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
                .thenBy { it.latency }
        )

        if (installedLangVoices.isEmpty()) {
            return voices.firstOrNull { it.locale.language.equals(langCode, ignoreCase = true) }
        }

        // Tier 1: Indian locale (IN) + matching Male/Female gender, highest quality first
        val tier1 = installedLangVoices.firstOrNull { voice ->
            voice.locale.country.equals("IN", ignoreCase = true) && isVoiceMatchingGender(voice, isMale)
        }
        if (tier1 != null) return tier1

        // Tier 2: Any Indian locale (IN) voice with highest quality
        val tier2GenderAnyCountry = installedLangVoices.firstOrNull { voice ->
            isVoiceMatchingGender(voice, isMale)
        }
        if (tier2GenderAnyCountry != null && !isHindi) {
            // Prefer Indian English if available, otherwise matching gender
            val indianVoice = installedLangVoices.firstOrNull { it.locale.country.equals("IN", ignoreCase = true) }
            if (indianVoice != null && indianVoice.quality >= tier2GenderAnyCountry.quality) {
                return indianVoice
            }
            return tier2GenderAnyCountry
        }

        // Tier 3: Best installed Indian locale (IN) voice
        val tier3 = installedLangVoices.firstOrNull { voice ->
            voice.locale.country.equals("IN", ignoreCase = true)
        }
        if (tier3 != null) return tier3

        // Tier 4: Best installed voice for target language
        return installedLangVoices.firstOrNull()
    }

    /**
     * Speaks ONLY Question + pause + Options A, B, C, D separately.
     * NEVER reads the answer key, explanation, or scores during an active exam.
     * Prevents overlapping speech by flushing any active utterance first.
     */
    fun speakQuestionAndOptions(
        questionText: String,
        options: List<String>,
        isHindi: Boolean,
        gender: String = "FEMALE"
    ) {
        if (!isInitialized) return

        // Always stop any ongoing speech to prevent overlapping or duplicate reading
        stop()

        // Detect whether the actual question/option text contains Hindi (Devanagari) script
        val hasDevanagari = questionText.any { it in '\u0900'..'\u097F' } ||
            options.any { opt -> opt.any { it in '\u0900'..'\u097F' } }
        val hasEnglishWords = Regex("[A-Za-z]{3,}").containsMatchIn(questionText)
        val effectiveIsHindi = when {
            hasDevanagari -> true
            hasEnglishWords -> false
            else -> isHindi
        }

        currentQuestionText = questionText
        currentOptions = options
        currentIsHindi = effectiveIsHindi
        currentGender = gender

        configureVoice(effectiveIsHindi, gender)

        // Strip any leading "Q1.", "Q.1", "1." prefix from question if present so only the written question is read
        val cleanQuestionRaw = questionText.trim()
        val normalizedQuestion = VoiceSpeechPreprocessor.preprocessForTts(cleanQuestionRaw, effectiveIsHindi)
        val uniqueTime = System.currentTimeMillis()
        val params = Bundle()

        val cleanedOptions = options.take(4)
        lastFinalUtterancePrefix = when (cleanedOptions.lastIndex) {
            0 -> "opt_a_"
            1 -> "opt_b_"
            2 -> "opt_c_"
            else -> "opt_d_"
        }

        // 1. Read the question first
        tts?.speak(normalizedQuestion, TextToSpeech.QUEUE_FLUSH, params, "q_$uniqueTime")

        // 2. Pause briefly after the question
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.playSilentUtterance(400L, TextToSpeech.QUEUE_ADD, "pause_q_$uniqueTime")
        }

        // 3. Read options A, B, C, and D separately
        cleanedOptions.forEachIndexed { index, rawOption ->
            val letter = ('A' + index).toString()
            val optionPrefix = if (effectiveIsHindi) "विकल्प $letter. " else "Option $letter. "
            // Strip redundant leading "A)", "(A)", "A." if already written in option text
            val strippedOption = rawOption.trim()
                .replaceFirst(Regex("^\\(?[A-Da-d1-4][).:-]\\s*"), "")
            val normalizedOption = VoiceSpeechPreprocessor.preprocessForTts(strippedOption, effectiveIsHindi)
            val fullOptionText = "$optionPrefix$normalizedOption"

            val optKey = when (index) {
                0 -> "opt_a_$uniqueTime"
                1 -> "opt_b_$uniqueTime"
                2 -> "opt_c_$uniqueTime"
                else -> "opt_d_$uniqueTime"
            }

            tts?.speak(fullOptionText, TextToSpeech.QUEUE_ADD, params, optKey)

            if (index < cleanedOptions.lastIndex && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.playSilentUtterance(320L, TextToSpeech.QUEUE_ADD, "pause_${index}_$uniqueTime")
            }
        }

        if (cleanedOptions.isEmpty()) {
            lastFinalUtterancePrefix = "q_"
        }

        isSpeaking = true
        isPaused = false
    }

    fun pause() {
        if (isSpeaking) {
            tts?.stop()
            isSpeaking = false
            isPaused = true
        }
    }

    fun resume() {
        if (isPaused && currentQuestionText.isNotBlank()) {
            speakQuestionAndOptions(currentQuestionText, currentOptions, currentIsHindi, currentGender)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        isSpeaking = false
        isPaused = false
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isSpeaking = false
        isPaused = false
    }
}
