# Privacy design

Awqat is designed so its core functions do not require an account or project backend.

## Intended v1 behavior
- Location is used locally to calculate prayer times and Qibla.
- No Awqat-owned server receives the user's location.
- No account/profile is required.
- No advertising SDK or analytics SDK is planned for v1.
- Official prayer-time verification, when enabled for a region, may make a direct request from the device to the named authority's public service. The UI/privacy disclosure must identify this before release where required.
- Verification responses are cached on-device to reduce network use.

Platform privacy manifests and store disclosures must be reviewed immediately before release because OS/store requirements can change.
