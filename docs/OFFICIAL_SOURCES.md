# Official prayer-time source strategy

The app maps **location -> country/region authority**. UI language never determines the religious authority.

Legal/source review date: **2026-08-29**. This is an engineering release gate, not legal advice. Terms can change and must be checked again before release.

## Runtime rule

Only a source with an explicit machine-use licence that permits the intended product use, redistribution and on-device caching may be marked `ENABLED`. `PrayerRepository` ignores every adapter marked `PERMISSION_REQUIRED`, so an unfinished parser cannot accidentally make a network request. Local astronomical calculation always remains available.

## Candidate decisions

| Region | Official source | Published format | Terms and cache decision | Runtime decision |
|---|---|---|---|---|
| Singapore | [MUIS consolidated timetable on data.gov.sg](https://data.gov.sg/datasets/d_a6a206cba471fe04b62dd886ef5eaf22/view) | CSV + Datastore API; currently 2024-2026 | [Singapore Open Data Licence v1.0](https://data.gov.sg/open-data-licence) allows commercial use, adaptation, redistribution and caching with conspicuous attribution. Anonymous Datastore Search limit is 4 calls per 10 seconds. | **Enabled:** fetch the full published dataset and cache with an annual refresh horizon. |
| Malaysia | [JAKIM e-Solat](https://www.e-solat.gov.my/) | Daily RSS/XML by zone | No current reuse/cache licence or published rate limit was confirmed. The archived 2021 open-data export is licensed but stale. | **Permission required:** XML parser is regression-only and cannot be selected at runtime. |
| Brunei | [Ministry of Religious Affairs (KHEU)](https://www.kheu.gov.bn/) | Daily HTML display | No current machine-readable timetable or explicit reuse/cache licence confirmed. | **Local only** until a stable licensed publication is found. |
| Oman | [Ministry of Endowments and Religious Affairs calendar](https://www.mara.gov.om/calendar.html) | Daily/monthly web calendar | Site states all rights reserved; no prayer-time open dataset confirmed. | **Permission required.** |
| Jordan | [Ministry of Awqaf](https://www.awqaf.gov.jo/) | Annual PDF calendar | [Copyright policy](https://www.awqaf.gov.jo/AR/Pages/%D8%AD%D9%82%D9%88%D9%82___%D8%A7%D9%84%D9%86%D8%B4%D8%B1) permits limited unchanged excerpts with attribution; broader product reuse requires contact with the ministry. | **Permission required.** |
| Egypt | [Egyptian General Authority of Survey](https://www.esa.gov.eg/praytimes.aspx) | Daily/monthly HTML tables | [Terms of use](https://www.esa.gov.eg/TERMSOFUSE.aspx) prohibit copying, republishing, downloading, transforming or using site content without permission. | **Permission required.** |
| Morocco | [Ministry of Habous prayer timetable](https://www.habous.gov.ma/prieres/) | Monthly web timetable | Official publication confirmed, but no explicit machine-use/redistribution licence confirmed. | **Permission required.** |
| Qatar | [Ministry of Interior prayer-time service](https://portal.moi.gov.qa/MoiPortalRestServices/rest/prayertimings/today/en) | Daily REST-style response | [Terms of use](https://portal.moi.gov.qa/wps/portal/en/MOIInternet/termsofuse) limit downloads to personal non-commercial use and require written permission for other use. | **Permission required.** |

The same decisions are encoded in `OfficialSourceCatalog`, so documentation and runtime policy can be regression-tested.

## Enabled adapter: MUIS open data

- Dataset ID: `d_a6a206cba471fe04b62dd886ef5eaf22`.
- Bulk endpoint: `https://data.gov.sg/api/action/datastore_search?resource_id=...&limit=5000`.
- No private credential is required at the documented anonymous rate limit.
- A production API key is optional for higher limits and must never be embedded in a client binary; use a secret-backed proxy if one becomes necessary.
- The adapter downloads the largest practical range (all published years) once per annual refresh and lets the existing `PrayerCache` persist every returned day.
- The parser accepts the legacy 12-hour records and the 24-hour format used from 2026 onward.
- Product attribution must remain conspicuous and link to the current Singapore Open Data Licence.

## Adapter priority

1. Public official JSON/XML/CSV API or asset with suitable reuse terms.
2. Official static machine-readable asset with suitable terms.
3. Official monthly/yearly timetable only after permission and parser stability review.
4. Stable official HTML only after permission, rate-limit and parser review.
5. Local calculation only.

Every adapter must fail open: verification failure must never make prayer times unavailable.
