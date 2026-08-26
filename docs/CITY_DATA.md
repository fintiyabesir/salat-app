# Offline global city data

Manual city selection must work without a Salat backend.

## Proposed source

GeoNames main gazetteer is the leading v1 candidate because it is globally downloadable and the GeoNames site currently identifies the work as CC BY 4.0.

## Build-time reduction

Do not ship the entire gazetteer blindly. Generate a compact app asset containing only fields needed by Salat:

- stable GeoNames id
- primary display name
- selected alternate/localized names where size permits
- ISO country code
- admin/region label
- latitude / longitude
- IANA timezone id
- population or feature rank for search ordering

Initial sizing experiment should compare population thresholds (for example 1k / 5k / 15k) and ensure important administrative centers are retained even below threshold.

## Runtime behavior

- Search is local/offline.
- Selected row becomes a `ManualCity` / `ResolvedLocation`.
- City timezone comes from the bundled row rather than the device timezone.
- Coordinates feed both prayer calculation and Qibla.
- Country code selects regional calculation/official-source policy.
- No GeoNames web-service account or runtime network request is required.

## Attribution / release gate

Before shipping the generated asset:
- re-check the exact downloaded dump license and any component-specific notices,
- preserve required attribution in Settings/About,
- record dump date/version in generated metadata,
- document the transformation script and reproducible source URL,
- verify app-size and multilingual-search behavior.

Do not confuse the main gazetteer license with separate GeoNames postal-code datasets, which can carry different notices.
