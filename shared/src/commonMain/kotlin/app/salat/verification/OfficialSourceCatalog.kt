package app.salat.verification

enum class OfficialSourceDecision {
    ENABLED_OPEN_DATA,
    PERMISSION_REQUIRED,
    NO_STABLE_MACHINE_SOURCE
}

data class OfficialSourceReview(
    val countryCode: String,
    val sourceId: String,
    val authorityName: String,
    val publicationUrl: String,
    val sourceShape: String,
    val termsUrl: String?,
    val decision: OfficialSourceDecision,
    val practicalCacheRange: String,
    val reviewNote: String,
    val reviewedOn: String = "2026-08-29"
)

/**
 * Release-gate catalogue for candidate official sources. Only entries explicitly marked
 * ENABLED_OPEN_DATA may be selected by the runtime adapter layer.
 */
object OfficialSourceCatalog {
    val reviews: List<OfficialSourceReview> = listOf(
        OfficialSourceReview(
            countryCode = "MY",
            sourceId = "jakim-esolat",
            authorityName = "Jabatan Kemajuan Islam Malaysia (JAKIM)",
            publicationUrl = "https://www.e-solat.gov.my/",
            sourceShape = "Daily RSS/XML by JAKIM zone",
            termsUrl = null,
            decision = OfficialSourceDecision.PERMISSION_REQUIRED,
            practicalCacheRange = "One day per zone",
            reviewNote = "A current licence, cache permission and published rate limit were not found. The parser remains regression-only."
        ),
        OfficialSourceReview(
            countryCode = "SG",
            sourceId = "muis-open-data",
            authorityName = "Majlis Ugama Islam Singapura (MUIS)",
            publicationUrl = MuisOpenDataAdapter.DATASET_URL,
            sourceShape = "Consolidated CSV/Datastore API",
            termsUrl = "https://data.gov.sg/open-data-licence",
            decision = OfficialSourceDecision.ENABLED_OPEN_DATA,
            practicalCacheRange = "All published years in one annual fetch",
            reviewNote = "Commercial reuse, adaptation and redistribution are allowed with conspicuous attribution under the Singapore Open Data Licence v1.0."
        ),
        OfficialSourceReview(
            countryCode = "BN",
            sourceId = "brunei-mora",
            authorityName = "Brunei Ministry of Religious Affairs (KHEU)",
            publicationUrl = "https://www.kheu.gov.bn/",
            sourceShape = "Official daily HTML display",
            termsUrl = null,
            decision = OfficialSourceDecision.NO_STABLE_MACHINE_SOURCE,
            practicalCacheRange = "Local calculation only",
            reviewNote = "No current machine-readable timetable or explicit reuse/cache licence was confirmed."
        ),
        OfficialSourceReview(
            countryCode = "OM",
            sourceId = "oman-mara",
            authorityName = "Oman Ministry of Endowments and Religious Affairs",
            publicationUrl = "https://www.mara.gov.om/calendar.html",
            sourceShape = "Official daily/monthly web calendar",
            termsUrl = "https://www.mara.gov.om/Pages.aspx?ID=25",
            decision = OfficialSourceDecision.PERMISSION_REQUIRED,
            practicalCacheRange = "Monthly after permission",
            reviewNote = "The ministry site states all rights reserved; no prayer-time open dataset was confirmed."
        ),
        OfficialSourceReview(
            countryCode = "JO",
            sourceId = "jordan-awqaf",
            authorityName = "Jordan Ministry of Awqaf, Islamic Affairs and Holy Places",
            publicationUrl = "https://www.awqaf.gov.jo/",
            sourceShape = "Official annual PDF calendar",
            termsUrl = "https://www.awqaf.gov.jo/AR/Pages/%D8%AD%D9%82%D9%88%D9%82___%D8%A7%D9%84%D9%86%D8%B4%D8%B1",
            decision = OfficialSourceDecision.PERMISSION_REQUIRED,
            practicalCacheRange = "One year after permission",
            reviewNote = "Terms allow limited unchanged excerpts with attribution; broader product reuse requires contacting the ministry."
        ),
        OfficialSourceReview(
            countryCode = "EG",
            sourceId = "egypt-survey",
            authorityName = "Egyptian General Authority of Survey",
            publicationUrl = "https://www.esa.gov.eg/praytimes.aspx",
            sourceShape = "Official daily/monthly HTML tables",
            termsUrl = "https://www.esa.gov.eg/TERMSOFUSE.aspx",
            decision = OfficialSourceDecision.PERMISSION_REQUIRED,
            practicalCacheRange = "Monthly after permission",
            reviewNote = "The authority prohibits copying, republishing, downloading, transforming or using site content without permission."
        ),
        OfficialSourceReview(
            countryCode = "MA",
            sourceId = "morocco-habous",
            authorityName = "Morocco Ministry of Habous and Islamic Affairs",
            publicationUrl = "https://www.habous.gov.ma/prieres/",
            sourceShape = "Official monthly web timetable",
            termsUrl = null,
            decision = OfficialSourceDecision.PERMISSION_REQUIRED,
            practicalCacheRange = "Monthly after permission",
            reviewNote = "The publication is official, but an explicit machine-use and redistribution licence was not confirmed."
        ),
        OfficialSourceReview(
            countryCode = "QA",
            sourceId = "qatar-moi",
            authorityName = "Qatar Ministry of Interior",
            publicationUrl = "https://portal.moi.gov.qa/MoiPortalRestServices/rest/prayertimings/today/en",
            sourceShape = "Official daily REST-style response",
            termsUrl = "https://portal.moi.gov.qa/wps/portal/en/MOIInternet/termsofuse",
            decision = OfficialSourceDecision.PERMISSION_REQUIRED,
            practicalCacheRange = "One day after permission",
            reviewNote = "Terms limit downloads to personal non-commercial use and require written permission for other use."
        )
    )

    fun review(countryCode: String?): OfficialSourceReview? =
        reviews.firstOrNull { it.countryCode.equals(countryCode, ignoreCase = true) }
}
