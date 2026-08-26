package app.salat.domain

import app.salat.model.CalculationMethodId
import kotlin.test.Test
import kotlin.test.assertEquals

class RegionalCalculationProfileResolverTest {
    @Test fun known_regions_select_expected_fallbacks() {
        assertEquals(CalculationMethodId.TURKEY, RegionalCalculationProfileResolver.resolve("TR").method)
        assertEquals(CalculationMethodId.MALAYSIA, RegionalCalculationProfileResolver.resolve("MY").method)
        assertEquals(CalculationMethodId.SINGAPORE, RegionalCalculationProfileResolver.resolve("SG").method)
        assertEquals(CalculationMethodId.EGYPTIAN, RegionalCalculationProfileResolver.resolve("EG").method)
        assertEquals(CalculationMethodId.UMM_AL_QURA, RegionalCalculationProfileResolver.resolve("SA").method)
    }

    @Test fun unknown_region_uses_mwl_as_safe_generic_fallback() {
        assertEquals(CalculationMethodId.MUSLIM_WORLD_LEAGUE, RegionalCalculationProfileResolver.resolve("ZZ").method)
    }
}
