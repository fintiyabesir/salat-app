#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_RES = ROOT / "androidApp" / "src" / "main" / "res"
IOS_ROOT = ROOT / "iosApp" / "SalatApp"

ANDROID_LOCALES = {
    "values": "en",
    "values-tr": "tr",
    "values-ar": "ar",
    "values-fa": "fa",
    "values-ur": "ur",
    "values-bn": "bn",
    "values-ms": "ms",
    "values-b+zh+Hans": "zh-Hans",
    "values-b+zh+Hant": "zh-Hant",
}
IOS_LOCALES = ["en", "tr", "ar", "fa", "ur", "bn", "ms", "zh-Hans", "zh-Hant"]
RTL = {"ar", "fa", "ur"}


def android_keys(directory: str) -> set[str]:
    folder = ANDROID_RES / directory
    if not folder.exists():
        raise AssertionError(f"missing Android localization directory: {folder}")
    keys: set[str] = set()
    for path in sorted(folder.glob("*.xml")):
        root = ET.parse(path).getroot()
        keys.update(node.attrib["name"] for node in root.findall("string") if "name" in node.attrib)
    if not keys:
        raise AssertionError(f"no Android string resources found in {folder}")
    return keys


def ios_keys(locale: str) -> set[str]:
    folder = IOS_ROOT / f"{locale}.lproj"
    if not folder.exists():
        raise AssertionError(f"missing iOS localization directory: {folder}")
    keys: set[str] = set()
    for path in sorted(folder.glob("*.strings")):
        text = path.read_text(encoding="utf-8")
        keys.update(re.findall(r'^\s*"([^"]+)"\s*=', text, flags=re.MULTILINE))
    if not keys:
        raise AssertionError(f"no iOS localization strings found in {folder}")
    return keys


def assert_equal(reference: set[str], actual: set[str], label: str) -> None:
    missing = sorted(reference - actual)
    extra = sorted(actual - reference)
    if missing or extra:
        raise AssertionError(f"{label}: missing={missing}, extra={extra}")


def main() -> int:
    android_reference = android_keys("values")
    for directory, locale in ANDROID_LOCALES.items():
        assert_equal(android_reference, android_keys(directory), f"Android {locale}")

    ios_reference = ios_keys("en")
    for locale in IOS_LOCALES:
        assert_equal(ios_reference, ios_keys(locale), f"iOS {locale}")

    manifest = (ROOT / "androidApp" / "src" / "main" / "AndroidManifest.xml").read_text(encoding="utf-8")
    if 'android:supportsRtl="true"' not in manifest:
        raise AssertionError("Android manifest must enable supportsRtl=true")
    if 'android:localeConfig="@xml/locales_config"' not in manifest:
        raise AssertionError("Android manifest must declare localeConfig")

    missing_rtl_android = [locale for locale in RTL if f"values-{locale}" not in ANDROID_LOCALES]
    missing_rtl_ios = [locale for locale in RTL if locale not in IOS_LOCALES]
    if missing_rtl_android or missing_rtl_ios:
        raise AssertionError(f"RTL locale configuration incomplete: Android={missing_rtl_android}, iOS={missing_rtl_ios}")

    print(f"Localization validation passed: {len(android_reference)} Android keys, {len(ios_reference)} iOS keys, {len(IOS_LOCALES)} locale variants")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"Localization validation failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
