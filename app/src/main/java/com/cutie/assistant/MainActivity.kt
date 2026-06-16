package com.cutie.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvReply: TextView
    private lateinit var btnListen: Button
    private var tts: TextToSpeech? = null
    private var interactionCount = 0
    private val romanticPoolShuffled = ROMANTIC_POOL.shuffled().toMutableList()
    private var romanticIndex = 0

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.get(0) ?: ""
            if (spokenText.isNotBlank()) handleCommand(spokenText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvReply = findViewById(R.id.tvReply)
        btnListen = findViewById(R.id.btnListen)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }

        val greeting = getTimeGreeting()
        tvReply.text = greeting
        speak(greeting)

        btnListen.setOnClickListener { startListening() }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलिए बॉस...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Voice recognition available nahi hai", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speak(text: String) {
        val clean = text.replace(Regex("\\*[^*]*\\*"), "")
            .replace(Regex("[^\\p{L}\\p{N}\\p{P}\\p{Z}]"), "")
            .trim()
        if (clean.length > 1) tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "cutie")
    }

    private fun showReply(text: String) {
        tvReply.text = text
        speak(text)
    }

    private fun nextRomantic(): String {
        if (romanticIndex >= romanticPoolShuffled.size) {
            romanticPoolShuffled.clear()
            romanticPoolShuffled.addAll(ROMANTIC_POOL.shuffled())
            romanticIndex = 0
        }
        return romanticPoolShuffled[romanticIndex++]
    }

    private fun handleCommand(rawText: String) {
        val lower = rawText.lowercase(Locale.getDefault()).trim()
        interactionCount++

        if (interactionCount % 8 == 0) { showReply(BATTERY_REMINDERS.random()); return }
        if (ABUSE_WORDS.any { lower.contains(it) }) { showReply(TOXIC_POOL.random()); return }
        if (IDENTITY_TRIGGERS.any { lower.contains(it) }) {
            showReply("मैं अपने प्यारे बॉस की असिस्टेंट हूँ... 💋 सिर्फ उनकी, हमेशा उनकी! 🌹")
            return
        }
        if (BOSS_TRIGGERS.any { lower.contains(it) }) {
            showReply("मेरे बॉस का नाम रेहान सर है! 👑💕")
            return
        }

        val appKeyword = APP_PACKAGES.keys.firstOrNull { lower.contains(it) }
        if (appKeyword != null) {
            val pkg = APP_PACKAGES[appKeyword]!!
            val launched = packageManager.getLaunchIntentForPackage(pkg)?.also {
                startActivity(it)
            } != null
            showReply(if (launched) "✓ खोल रही हूँ: ${appKeyword.replaceFirstChar { it.uppercase() }} 💫"
                      else "Boss... यह ऐप फोन में नहीं मिला! 🥺")
            return
        }

        showReply(when (detectMood(lower)) {
            Mood.NAKHRE -> NAKHRE_POOL.random()
            Mood.ANGRY_SAD -> COMFORTING_POOL.random()
            Mood.DIRTY -> FLIRTY_POOL.random()
            Mood.ROMANTIC -> DEEP_LOVE_POOL.random()
            else -> nextRomantic()
        })
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
