# Security policy

## Reporting a vulnerability

Report security issues privately through GitHub's
[private vulnerability reporting](https://github.com/fintiyabesir/salat-app/security/advisories/new).
Please do not open a public issue for a security problem.

Expect an acknowledgement within seven days.

## Scope

Awqat has no backend, no account system and no analytics. The realistic security
surface is therefore narrow, and reports in these areas are the most useful:

  - anything that causes location data to leave the device;
  - the release and signing pipeline (`.github/workflows/`), including workflow
    injection and secret exposure;
  - the bundled city catalog and its build-time generation;
  - the official-source verification path, once it makes network requests.

## Out of scope

  - Prayer time values that differ from a local authority. These are a
    calculation-method question, not a vulnerability — open a normal issue.
  - Findings that require a rooted or jailbroken device with physical access.
