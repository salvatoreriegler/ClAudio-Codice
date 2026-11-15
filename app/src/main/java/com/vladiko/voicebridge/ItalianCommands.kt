package com.vladiko.voicebridge

import java.text.Normalizer
import java.util.Locale

/** Pure parser shared by Android recognition and the unit-test command corpus. */
object ItalianCommands {
    data class Command(val cmd: String, val n: Int = 0, val name: String = "")
    val sendPhrases = setOf("invia", "invia ora", "invia pure", "invia per favore")
    val cancelPhrases = setOf("annulla", "annulla messaggio")
    private val numbers = mapOf(
        "uno" to 1, "una" to 1, "primo" to 1, "prima" to 1,
        "due" to 2, "secondo" to 2, "seconda" to 2,
        "tre" to 3, "terzo" to 3, "terza" to 3,
        "quattro" to 4, "quarto" to 4, "quarta" to 4,
        "cinque" to 5, "quinto" to 5, "quinta" to 5,
        "sei" to 6, "sesto" to 6, "sesta" to 6,
        "sette" to 7, "settimo" to 7, "settima" to 7,
        "otto" to 8, "ottavo" to 8, "ottava" to 8,
        "nove" to 9, "nono" to 9, "nona" to 9
    )
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    fun parse(text: String): Command? {
        if (Regex("[.!?]").containsMatchIn(text.trim().replace(Regex("[.!?]+$"), ""))) return null
        val s = normalize(text)
        if (s in setOf("elenca le sessioni", "quali sessioni", "elenca i canali", "quali canali")) return Command("list")
        if (s in setOf("torna automatico", "torna in automatico", "rispondi all ultima sessione")) return Command("unpin")
        if (s in setOf("fine comunicazione", "spegni il microfono")) return Command("sleep")
        val target = Regex("^(?:passa|vai) (?:a|al|alla) (.+)$").matchEntire(s)?.groupValues?.get(1) ?: return null
        val numeric = target.replace(Regex("^(?:sessione|canale) "), "").replace(Regex(" (?:sessione|canale)$"), "")
        val n = numbers[numeric] ?: numeric.takeIf { it.matches(Regex("[1-9]")) }?.toInt()
        if (n != null) return Command("switch", n)
        if (numeric.matches(Regex("\\d+"))) return null
        val name = Regex("^(?:sessione|canale) ([a-z0-9]+(?: [a-z0-9]+){0,3})$")
            .matchEntire(target)?.groupValues?.get(1) ?: return null
        if (Regex("\\b(?:e|poi|quindi)\\b").containsMatchIn(name)) return null
        return Command("switch", name = name)
    }
}
