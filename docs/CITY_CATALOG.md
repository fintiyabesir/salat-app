# Offline city catalog

Salat's manual-location flow must remain fully functional without a backend or runtime geocoding API. The production catalog is generated from GeoNames at build time and bundled with the application.

## Source and license

GeoNames publishes downloadable UTF-8 gazetteer extracts under CC BY 4.0. Salat uses the city extracts plus `countryInfo.txt` and `admin1CodesASCII.txt`. Attribution is maintained in `THIRD_PARTY_NOTICES.md` and must also be exposed from the app's About / Licenses screen.

## Build pipeline

`scripts/process_geonames_cities.py` consumes already-downloaded GeoNames inputs and creates a deterministic `city_catalog.tsv` plus `city_catalog.tsv.gz` and a JSON benchmark report. Downloading is intentionally performed by the build workflow, keeping network code out of the application and the deterministic processor.

The compact catalog contains only:

1. GeoNames id
2. display name
3. ISO country code
4. country name
5. first-level region name
6. latitude
7. longitude
8. IANA timezone id
9. population used only for result ordering
10. a capped list of searchable aliases

The installed app never contacts GeoNames.

## Cutoff decision

Do not choose a population cutoff by intuition. `.github/workflows/city-catalog-benchmark.yml` measures these official GeoNames extracts:

- `cities15000`
- `cities5000`
- `cities1000`
- `cities500`

For every profile it records record count, compact raw/gzip size, initial in-memory search-index construction time, representative Unicode search timings, Android debug APK size, iOS simulator app size and compressed iOS app size. The selected production profile and measured values will be recorded here after the benchmark run.

## Runtime search strategy

The current benchmark intentionally starts with a simple in-memory normalized linear search. This provides a measurable baseline. If the selected profile is too slow or memory-heavy, the next implementation should add a prefix/token index before changing the dataset cutoff, because data coverage and indexing are separate decisions.

The existing small `StarterManualCityCatalog` remains as a development/fallback fixture until the production-generated resource loader is integrated and validated on both platforms.
