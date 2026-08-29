package app.salat.verification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfficialSourceCatalogTest {
    @Test
    fun candidate_set_has_an_explicit_release_decision() {
        assertEquals(
            setOf("MY", "SG", "BN", "OM", "JO", "EG", "MA", "QA"),
            OfficialSourceCatalog.reviews.map { it.countryCode }.toSet()
        )
        assertEquals(
            listOf("SG"),
            OfficialSourceCatalog.reviews
                .filter { it.decision == OfficialSourceDecision.ENABLED_OPEN_DATA }
                .map { it.countryCode }
        )
        assertNull(OfficialSourceCatalog.review("ZZ"))
    }
}
