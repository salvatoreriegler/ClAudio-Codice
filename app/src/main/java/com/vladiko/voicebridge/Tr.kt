package com.vladiko.voicebridge

import java.util.Locale

object Lang {
    val locale: Locale = Locale.ITALIAN
    val ttsLocale: Locale = Locale.ITALY
    val recognizerTag: String = "it-IT"
}

// Keep upstream translations at the call site to simplify future merges.
// Technical logs and the unpublished hub retain English until localized.
@Suppress("UNUSED_PARAMETER")
fun tr(ru: String, en: String, it: String = en): String = it
