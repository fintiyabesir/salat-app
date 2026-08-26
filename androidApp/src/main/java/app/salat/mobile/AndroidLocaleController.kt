package app.salat.mobile

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AndroidLocaleController {
    fun apply(context: Context, languageTag: String?) {
        val tag = languageTag.orEmpty()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(tag)
            return
        }

        val locale = if (tag.isBlank()) Locale.getDefault() else Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}
