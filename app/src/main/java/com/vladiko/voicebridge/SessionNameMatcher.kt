package com.vladiko.voicebridge

import java.text.Normalizer
import java.util.Locale

/** Resolve spoken chat names without treating transport labels or IDs as names. */
object SessionNameMatcher {
    data class Candidate(val sid: String, val name: String)

    private val bridgePrefix = Regex(
        "^claude(?:\\s+(?:windows|wsl(?:\\s+.+?)?))?\\s+[-–—]\\s+",
        RegexOption.IGNORE_CASE
    )
    private val idSuffix = Regex("\\s*\\[[a-f0-9-]{8,36}]\\s*$", RegexOption.IGNORE_CASE)
    private val digits = Regex("\\d+")
    private val spokenNumbers = mapOf(
        "zero" to "0", "uno" to "1", "una" to "1", "due" to "2", "tre" to "3",
        "quattro" to "4", "cinque" to "5", "sei" to "6", "sette" to "7",
        "otto" to "8", "nove" to "9", "dieci" to "10", "undici" to "11",
        "dodici" to "12", "tredici" to "13", "quattordici" to "14",
        "quindici" to "15", "sedici" to "16", "diciassette" to "17",
        "diciotto" to "18", "diciannove" to "19", "venti" to "20"
    )
    // Preserve the upstream's ability to match Cyrillic pronunciations of names.
    private val cyrillic = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e",
        'ё' to "e", 'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "i", 'к' to "k",
        'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r",
        'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "c",
        'ч' to "ch", 'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y",
        'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya"
    )

    private fun words(value: String): String {
        val latin = buildString {
            for (c in value.lowercase(Locale.ROOT)) append(cyrillic[c] ?: c.toString())
        }
        return Normalizer.normalize(latin, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
            .replace(Regex("[^a-z0-9]+"), " ").trim().split(Regex("\\s+"))
            .joinToString(" ") { spokenNumbers[it] ?: it }
    }

    fun displayName(value: String): String =
        if (bridgePrefix.containsMatchIn(value.trim())) value.trim().replace(idSuffix, "").replace(bridgePrefix, "")
        else value.trim()

    fun hasOrigin(value: String): Boolean = bridgePrefix.containsMatchIn(value.trim())

    fun sameName(a: String, b: String): Boolean = compact(a) == compact(b)

    private fun compact(value: String) = words(value).replace(" ", "")
    private fun numbers(value: String) = digits.findAll(value).map { it.value }.toList()
    private data class Entry(val candidate: Candidate, val title: String, val aliases: Set<String>)

    fun match(spoken: String, candidates: List<Candidate>): Candidate? {
        val query = compact(spoken)
        if (query.isEmpty()) return null
        val entries = candidates.distinctBy { it.sid to it.name }.map { candidate ->
            val label = candidate.name.trim()
            val decorated = bridgePrefix.containsMatchIn(label)
            val withoutId = if (decorated) label.replace(idSuffix, "") else label
            val title = withoutId.replace(bridgePrefix, "")
            Entry(candidate, words(title), setOf(compact(label), compact(withoutId), compact(title)))
        }

        // Full names win over partial names, so Dashboard and Dashboard (fork)
        // remain distinct. Identical names in two sessions require clarification.
        val exact = entries.filter { query in it.aliases }
        if (exact.isNotEmpty()) return exact.distinctBy { it.candidate.sid }.singleOrNull()?.candidate

        val phrase = " ${words(spoken)} "
        val partial = entries.filter { " ${it.title} ".contains(phrase) }
        if (partial.isNotEmpty()) return partial.distinctBy { it.candidate.sid }.singleOrNull()?.candidate

        if (query.length < 4) return null
        val ranked = entries.mapNotNull { entry ->
            val title = entry.title.replace(" ", "")
            // Never turn "SECURDOC 2" into "SECURDOC 1" through typo tolerance.
            if (title.isEmpty() || numbers(query) != numbers(title)) null
            else entry to distance(query, title).toDouble() / maxOf(query.length, title.length)
        }.sortedBy { it.second }.distinctBy { it.first.candidate.sid }
        val best = ranked.firstOrNull() ?: return null
        if (best.second > 0.25) return null
        if (ranked.size > 1 && ranked[1].second - best.second < 0.15) return null
        return best.first.candidate
    }

    private fun distance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1,
                    previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            previous = current
        }
        return previous[b.length]
    }
}
