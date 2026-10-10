package com.example.util

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

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                isPaused = false
            }

            override fun onDone(utteranceId: String?) {
                // If it's the final option utterance or single preview utterance, set speaking false
                if (utteranceId?.startsWith("opt_d_") == true || utteranceId?.startsWith("preview_") == true) {
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
     * Finds and configures the highest quality voice available matching language and gender.
     * Implements a robust 5-tier fallback so TTS never crashes.
     */
    private fun configureVoice(isHindi: Boolean, gender: String) {
        val targetLocale = if (isHindi) Locale("hi", "IN") else Locale("en", "IN")
        val isMale = gender.equals("MALE", ignoreCase = true)

        try {
            // Set locale
            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to standard US English or device default
                tts?.setLanguage(Locale.US)
            }

            // Set speech rate (0.95: comfortable educational pace)
            tts?.setSpeechRate(0.95f)

            // Educational pitch adjustment for natural clarity
            val pitch = if (isMale) 0.88f else 1.05f
            tts?.setPitch(pitch)

            // Select best high-quality installed voice
            val allVoices = tts?.voices
            if (!allVoices.isNullOrEmpty()) {
                val matchedVoice = findBestVoice(allVoices, isHindi, isMale)
                if (matchedVoice != null) {
                    tts?.voice = matchedVoice
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore and rely on standard default
        }
    }

    private fun findBestVoice(voices: Set<Voice>, isHindi: Boolean, isMale: Boolean): Voice? {
        val langCode = if (isHindi) "hi" else "en"

        // Candidates matching target language
        val langVoices = voices.filter {
            it.locale.language.equals(langCode, ignoreCase = true) &&
                (it.features == null || !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
        }

        if (langVoices.isEmpty()) {
            return voices.firstOrNull { it.locale.language.equals(langCode, ignoreCase = true) }
        }

        // Tier 1: Matching locale (e.g. "IN") + matching gender
        val tier1 = langVoices.firstOrNull { voice ->
            val name = voice.name.lowercase(Locale.ROOT)
            val matchesGender = if (isMale) {
                name.contains("male") || name.contains("enc") || name.contains("tpd") || name.contains("man")
            } else {
                name.contains("female") || name.contains("cfl") || name.contains("sfg") || name.contains("fem")
            }
            voice.locale.country.equals("IN", ignoreCase = true) && matchesGender
        }
        if (tier1 != null) return tier1

        // Tier 2: Any country for target language + matching gender
        val tier2 = langVoices.firstOrNull { voice ->
            val name = voice.name.lowercase(Locale.ROOT)
            if (isMale) {
                name.contains("male") || name.contains("enc") || name.contains("tpd") || name.contains("man")
            } else {
                name.contains("female") || name.contains("cfl") || name.contains("sfg") || name.contains("fem")
            }
        }
        if (tier2 != null) return tier2

        // Tier 3: First available voice for this language
        return langVoices.firstOrNull()
    }

    /**
     * Speaks ONLY Question + Options A, B, C, D with natural educational pauses.
     * NEVER reads correct answer, explanation, or scores.
     */
    fun speakQuestionAndOptions(
        questionText: String,
        options: List<String>,
        isHindi: Boolean,
        gender: String = "FEMALE"
    ) {
        if (!isInitialized) return

        stop()

        currentQuestionText = questionText
        currentOptions = options
        currentIsHindi = isHindi
        currentGender = gender

        configureVoice(isHindi, gender)

        // Normalize text through dedicated normalization layer
        val normalizedQuestion = VoiceSpeechPreprocessor.preprocessForTts(questionText, isHindi)

        val uniqueTime = System.currentTimeMillis()
        val params = Bundle()

        // 1. Speak Question
        tts?.speak(normalizedQuestion, TextToSpeech.QUEUE_FLUSH, params, "q_$uniqueTime")

        // 2. Short natural pause between Question and Options
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.playSilentUtterance(350L, TextToSpeech.QUEUE_ADD, "pause_q_$uniqueTime")
        }

        // 3. Speak Options A, B, C, D in order with pauses
        options.forEachIndexed { index, rawOption ->
            val letter = ('A' + index).toString()
            val optionPrefix = if (isHindi) "विकल्प $letter: " else "Option $letter: "
            val normalizedOption = VoiceSpeechPreprocessor.preprocessForTts(rawOption, isHindi)
            val fullOptionText = "$optionPrefix$normalizedOption"

            val optKey = when (index) {
                0 -> "opt_a_$uniqueTime"
                1 -> "opt_b_$uniqueTime"
                2 -> "opt_c_$uniqueTime"
                else -> "opt_d_$uniqueTime"
            }

            tts?.speak(fullOptionText, TextToSpeech.QUEUE_ADD, params, optKey)

            if (index < options.lastIndex && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.playSilentUtterance(280L, TextToSpeech.QUEUE_ADD, "pause_${index}_$uniqueTime")
            }
        }

        isSpeaking = true
        isPaused = false
    }

    /**
     * Previews the selected voice with a natural educational sentence.
     */
    fun previewVoice(isHindi: Boolean, gender: String) {
        if (!isInitialized) return

        stop()
        configureVoice(isHindi, gender)

        val previewSentence = if (isHindi) {
            "जेएसी एमसीक्यू मास्टर में आपका स्वागत है। आइए मिलकर अभ्यास करें और परीक्षा में सफलता प्राप्त करें।"
        } else {
            "Welcome to JAC MCQ Master. Let us practice and excel together."
        }

        val params = Bundle()
        tts?.speak(previewSentence, TextToSpeech.QUEUE_FLUSH, params, "preview_${System.currentTimeMillis()}")
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
        tts?.stop()
        isSpeaking = false
        isPaused = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isSpeaking = false
        isPaused = false
    }
}
