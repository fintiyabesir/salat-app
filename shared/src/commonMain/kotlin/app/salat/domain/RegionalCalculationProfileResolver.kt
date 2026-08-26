package app.salat.domain

import app.salat.model.CalculationMethodId
import app.salat.model.CalculationProfile

/**
 * Conservative fallback mapping for local astronomical calculation.
 * Official-source verification remains separate and can override/validate these values.
 */
object RegionalCalculationProfileResolver {
    fun resolve(countryCode: String): CalculationProfile {
        val cc = countryCode.uppercase()
        val method = when (cc) {
            "TR" -> CalculationMethodId.TURKEY
            "MY" -> CalculationMethodId.MALAYSIA
            "SG" -> CalculationMethodId.SINGAPORE
            "EG" -> CalculationMethodId.EGYPTIAN
            "PK" -> CalculationMethodId.KARACHI
            "SA" -> CalculationMethodId.UMM_AL_QURA
            "AE" -> CalculationMethodId.DUBAI
            "QA" -> CalculationMethodId.QATAR
            "KW" -> CalculationMethodId.KUWAIT
            else -> CalculationMethodId.MUSLIM_WORLD_LEAGUE
        }
        return CalculationProfile(id = "auto-${cc.lowercase()}-${method.name.lowercase()}", method = method)
    }
}
