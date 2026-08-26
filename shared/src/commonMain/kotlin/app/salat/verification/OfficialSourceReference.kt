package app.salat.verification

enum class OfficialSourceIntegrationStatus {
    ADAPTER_AVAILABLE,
    REFERENCE_CONFIGURED,
    LOCAL_ONLY
}

data class OfficialSourceReference(
    val sourceId: String,
    val displayName: String,
    val status: OfficialSourceIntegrationStatus
)

/**
 * Product-facing reference metadata only. This object never claims that today's
 * values were verified. Actual Verified/Different state must come from PrayerRepository.
 */
object OfficialSourceReferenceResolver {
    fun resolve(countryCode: String?): OfficialSourceReference? = when (countryCode?.uppercase()) {
        "TR" -> OfficialSourceReference("diyanet", "Diyanet İşleri Başkanlığı", OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED)
        "MY" -> OfficialSourceReference("jakim", "JAKIM e-Solat", OfficialSourceIntegrationStatus.ADAPTER_AVAILABLE)
        "SG" -> OfficialSourceReference("muis", "MUIS", OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED)
        "ID" -> OfficialSourceReference("kemenag", "Kementerian Agama Republik Indonesia", OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED)
        "QA" -> OfficialSourceReference("qatar-moi", "Qatar Ministry of Interior", OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED)
        "BN" -> OfficialSourceReference("brunei-mora", "Brunei Ministry of Religious Affairs", OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED)
        "OM" -> OfficialSourceReference("oman-awqaf", "Oman Ministry of Endowments and Religious Affairs", OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED)
        "JO" -> OfficialSourceReference("jordan-awqaf", "Jordan Ministry of Awqaf", OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED)
        "EG" -> OfficialSourceReference("egypt-survey", "Egyptian General Authority of Survey", OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED)
        "MA" -> OfficialSourceReference("morocco-habous", "Morocco Ministry of Habous and Islamic Affairs", OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED)
        else -> null
    }
}
