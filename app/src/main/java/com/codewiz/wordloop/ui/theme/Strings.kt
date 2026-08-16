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
    return if (args.isEmpty()) raw else raw.format(*args)
}
