package app.salat.verification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfficialSourceReferenceTest {
    @Test fun malaysia_stays_reference_only_until_reuse_permission_is_confirmed() {
        val source = OfficialSourceReferenceResolver.resolve("MY")
        assertEquals("JAKIM e-Solat", source?.displayName)
        assertEquals(OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED, source?.status)
    }

    @Test fun singapore_exposes_the_open_data_adapter_without_claiming_today_is_verified() {
        val source = OfficialSourceReferenceResolver.resolve("SG")
        assertEquals("MUIS", source?.displayName)
        assertEquals(OfficialSourceIntegrationStatus.ADAPTER_AVAILABLE, source?.status)
    }

    @Test fun turkey_is_reference_configured_but_not_live_adapter() {
        val source = OfficialSourceReferenceResolver.resolve("TR")
        assertEquals(OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED, source?.status)
    }

    @Test fun unknown_country_has_no_official_reference_yet() {
        assertNull(OfficialSourceReferenceResolver.resolve("ZZ"))
    }
}
