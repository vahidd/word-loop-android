package com.codewiz.wordloop.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.codewiz.wordloop.util.IosStringKeys

val LocalAppLanguageCode = staticCompositionLocalOf { "en" }

@Composable
fun tr(english: String, vararg args: Any): String {
    LocalAppLanguageCode.current
    val context = LocalContext.current
    val name = IosStringKeys.byEnglish[english]
    val resId = if (name != null) {
        context.resources.getIdentifier(name, "string", context.packageName)
    } else {
        0
    }
    val raw = if (resId != 0) context.getString(resId) else english
    val normalized = normalizeIosFormat(raw)
    return if (args.isEmpty()) normalized else runCatching {
        normalized.format(*args)
    }.getOrDefault(normalized)
}

fun normalizeIosFormat(template: String): String =
    template.replace("%@", "%s").replace("%lld", "%d")
