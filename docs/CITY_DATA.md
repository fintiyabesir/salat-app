# Offline global city data

Manual city selection works without an Awqat backend or a runtime GeoNames API call.

## Source and license

Awqat uses a reduced GeoNames city dump. GeoNames identifies the gazetteer as CC BY 4.0. The application exposes GeoNames / CC BY 4.0 attribution in Settings and the repository carries the detailed notice in `THIRD_PARTY_NOTICES.md`.

Build inputs:
- `cities5000.zip`
- `alternateNamesV2.zip`
- `countryInfo.txt`
- `admin1CodesASCII.txt`

These inputs are processed at build/release preparation time only. They are never downloaded by an installed app.

## Dataset selection

A four-profile benchmark compared GeoNames `cities15000`, `cities5000`, `cities1000`, and `cities500`. The initial benchmark used the same compact catalog representation for each candidate and measured representative Unicode search plus real Android and iOS packaging.

| GeoNames profile | Records | Gzip catalog | Python reference linear search | Android debug APK |
| --- | ---: | ---: | ---: | ---: |
| cities15000 | 34,114 | 2.30 MB | ~6 ms | 14.63 MB |
| **cities5000** | **69,653** | **4.45 MB** | **~16-17 ms** | **17.03 MB** |
| cities1000 | 170,828 | 9.58 MB | ~40 ms | 22.82 MB |
| cities500 | 235,503 | 12.38 MB | ~46 ms | 26.00 MB |

For `cities5000`, the real iOS Simulator build measured 12.42 MB uncompressed `.app` and 6.45 MB zipped. The dataset contained entries across 245 country/territory codes in the measured dump.

`cities5000` is the production choice for v1. It approximately doubles the place coverage of `cities15000` while keeping package and search costs modest. Moving to `cities1000` more than doubles the record count again and gives a materially worse size/search trade-off for a prayer-time city picker.

## Multilingual aliases

The convenience `alternatenames` column in the city dump is not sufficient by itself for Awqat's launch languages. A quality check found that a small arbitrary alias limit could miss native-script searches such as `İstanbul`, `北京`, `ঢাকা`, `تهران`, and `کراچی`.

The production generator therefore also streams `alternateNamesV2.zip` and gives priority to:
1. current preferred/official GeoNames names;
2. names tagged for Awqat's launch languages: English, Chinese, Arabic, Turkish, Bengali, Malay, Urdu, and Persian;
3. remaining useful city-dump aliases.

Pseudo aliases such as postal codes, IATA/ICAO codes, links, and Wikidata ids are excluded. The generation workflow explicitly fails if representative launch-language native-script aliases are absent.

## Runtime format

The generated `salat-city-catalog-v2` TSV is sorted by population/relevance at build time, but population itself is not retained in the app bundle.

Runtime fields are limited to:
- stable GeoNames id;
- primary display name and selected aliases;
- ISO country code and country display name;
- admin/region label for disambiguation;
- latitude / longitude;
- IANA timezone id.

The bundled order supplies default relevance ranking, so mobile search can stop after the first result limit without carrying population data.

## Runtime behavior

- Search is local/offline.
- Android loads the packaged `city_catalog.tsv` asset on an IO dispatcher and precomputes normalized search keys once.
- iOS loads the same TSV from the application bundle off the main actor and precomputes normalized search keys once.
- Unicode/diacritic-insensitive matching supports both Latin transliterations and native-script aliases.
- Selected rows become the platform prayer location with the row's IANA timezone and coordinates.
- Coordinates feed prayer calculation and Qibla; country code selects regional calculation and official-source policy.
- No GeoNames web-service account or runtime request exists.

## Reproducibility / release gate

`scripts/process_geonames_cities.py` performs the deterministic reduction. `.github/workflows/generate-city-catalog.yml` downloads the permitted source dumps and emits the final artifact. Before a future dataset refresh, re-check GeoNames licensing, regenerate the artifact, verify multilingual aliases, record the new report/hash, and run the full Android + iOS CI builds.

Do not confuse the main gazetteer license with separate GeoNames postal-code datasets, which can carry different notices and are not used by Awqat.
