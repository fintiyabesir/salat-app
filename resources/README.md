# Bundled city catalog

`city_catalog.tsv` is Salat's canonical offline city resource. It is generated from GeoNames at build/maintenance time; the mobile apps do not call GeoNames at runtime.

Production profile: `cities5000` + `alternateNamesV2`, format `salat-city-catalog-v2`.

Latest generated resource:

- records: 69,653
- raw size: 11,292,983 bytes
- gzip size: 5,452,543 bytes
- SHA-256: `1e9c87ab12104b1ab2526134b09a61ed5b1fe602c68c09dea18ba17315220930`
- language-aware aliases: enabled

The transformation is reproducible through `scripts/process_geonames_cities.py` and `.github/workflows/generate-city-catalog.yml`. GeoNames attribution and license details are recorded in `THIRD_PARTY_NOTICES.md` and surfaced in application Settings.

Do not hand-edit `city_catalog.tsv`; regenerate it through the production catalog workflow.
