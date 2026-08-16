package com.codewiz.wordloop.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.codewiz.wordloop.util.AppUiLanguage
import com.codewiz.wordloop.util.IosStringKeys

val LocalAppLanguageCode = staticCompositionLocalOf { "en" }

@Composable
fun tr(english: String, vararg args: Any): String {
    val languageCode = LocalAppLanguageCode.current
    val context = LocalContext.current
    val resources = remember(languageCode, context) {
        val ui = AppUiLanguage.from(languageCode) ?: AppUiLanguage.fromSystemPreferred()
        val config = Configuration(context.resources.configuration)
        config.setLocale(ui.locale)
        context.createConfigurationContext(config).resources
    }
    val name = IosStringKeys.byEnglish[english]
    val resId = if (name != null) {
        resources.getIdentifier(name, "string", context.packageName)
    } else {
        0
    }
    val raw = if (resId != 0) resources.getString(resId) else english
    val normalized = normalizeIosFormat(raw)
    return if (args.isEmpty()) normalized else runCatching {
        normalized.format(*args)
    }.getOrDefault(normalized)
}

fun normalizeIosFormat(template: String): String =
    template.replace("%@", "%s").replace("%lld", "%d")
