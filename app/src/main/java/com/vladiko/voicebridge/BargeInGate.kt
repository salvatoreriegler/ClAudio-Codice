package com.vladiko.voicebridge

// AEC handles the audio; this second guard rejects recognizable playback leakage.
// No chat submission occurs here: accepted words enter the normal visible draft.
class BargeInGate {
    private var candidate = ""
    private var candidateAt = 0L

    fun accept(text: String, spoken: String, now: Long, final: Boolean): String? {
        val remaining = trimEcho(text, spoken)
        val words = tokens(remaining)
        if (words.isEmpty()) { candidate = ""; return null }
        val explicit = words.first() in interrupts
        if (!explicit && (words.size < 2 || words.joinToString("").length < 6)) return null
        val first = words.take(2).joinToString(" ")
        if (explicit || final || (first == candidate && now - candidateAt >= 180)) return remaining
        candidate = first; candidateAt = now
        return null
    }

    companion object {
        private val interrupts = setOf("stop", "aspetta", "fermati", "basta", "interrompi")
        private fun tokens(text: String) = ItalianCommands.normalize(text).split(" ").filter { it.isNotEmpty() }

        @JvmStatic fun trimEcho(text: String, spoken: String): String {
            val raw = text.trim().split(Regex("\\s+"))
            val heard = tokens(text)
            val echo = tokens(spoken)
            if (heard.isEmpty() || echo.isEmpty()) return text.trim()
            if (echo.windowed(heard.size).any { it == heard }) return ""
            // Minor ASR variations of our own spoken sentence are still echo.
            if (heard.size >= 5 && echo.windowed(heard.size).any { window ->
                window.zip(heard).count { (a, b) -> a == b } >= kotlin.math.ceil(heard.size * 0.8).toInt()
            }) return ""
            // Strip a leading run of playback words if the recognizer combined echo
            // and the user's interruption in one segment. Preserve the user's suffix.
            var prefix = 0
            for (start in echo.indices) {
                var n = 0
                while (n < heard.size && start + n < echo.size && heard[n] == echo[start + n]) n++
                prefix = maxOf(prefix, n)
            }
            if (prefix >= 3 && raw.size == heard.size) return raw.drop(prefix).joinToString(" ")
            return text.trim()
        }

        @JvmStatic fun withoutInterruptCommand(text: String): String {
            val first = tokens(text).firstOrNull() ?: return ""
            return if (first in interrupts) text.trim().substringAfter(' ', "").trimStart(',', '.', '!', ':', ' ')
                else text.trim()
        }
    }
}
