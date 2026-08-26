#!/usr/bin/env python3
"""Convert GeoNames city dump files into Salat's compact offline catalog.

Inputs are downloaded by the build workflow from GeoNames. GeoNames is licensed
under CC BY 4.0; the generated catalog must retain attribution in the app.
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import time
import unicodedata
import zipfile
from dataclasses import dataclass, replace
from pathlib import Path

FIELDS = ["id", "name", "ascii", "aliases", "lat", "lon", "fclass", "fcode", "cc", "cc2", "admin1", "admin2", "admin3", "admin4", "population", "elevation", "dem", "timezone", "modified"]
SUPPORTED_ALIAS_LANGUAGES = {"en", "zh", "ar", "tr", "bn", "ms", "ur", "fa"}
EXCLUDED_ALIAS_LANGUAGES = {"post", "iata", "icao", "faac", "link", "wkdt", "abbr"}

@dataclass(frozen=True)
class City:
    id: str
    name: str
    cc: str
    country: str
    region: str
    lat: str
    lon: str
    timezone: str
    population: int
    aliases: tuple[str, ...]


def clean(text: str) -> str:
    return " ".join(text.replace("\t", " ").replace("\r", " ").replace("\n", " ").split())


def norm(text: str) -> str:
    return " ".join(unicodedata.normalize("NFKC", text).casefold().split())


def fold(text: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFKD", text) if not unicodedata.combining(c)).casefold()


def countries(path: Path) -> dict[str, str]:
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        cols = line.split("\t")
        if len(cols) >= 5:
            result[cols[0]] = clean(cols[4])
    return result


def regions(path: Path) -> dict[str, str]:
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        cols = line.split("\t")
        if len(cols) >= 2:
            result[cols[0]] = clean(cols[1])
    return result


def base_aliases(name: str, ascii_name: str, alternate_names: str, limit: int) -> tuple[str, ...]:
    seen, result = {norm(name)}, []
    for value in [ascii_name, *alternate_names.split(",")]:
        value = clean(value).replace("|", " ")
        key = norm(value)
        if not value or len(value) > 100 or key in seen:
            continue
        seen.add(key)
        result.append(value)
        if len(result) >= limit:
            break
    return tuple(result)


def parse(zip_path: Path, country_path: Path, region_path: Path, alias_limit: int) -> list[City]:
    country_names, region_names = countries(country_path), regions(region_path)
    output: list[City] = []
    with zipfile.ZipFile(zip_path) as archive:
        member = next(name for name in archive.namelist() if name.endswith(".txt"))
        with archive.open(member) as source:
            for raw in source:
                cols = raw.decode("utf-8").rstrip("\n").split("\t")
                if len(cols) < len(FIELDS):
                    continue
                row = dict(zip(FIELDS, cols))
                cc = row["cc"].upper()
                if not cc or not row["timezone"]:
                    continue
                try:
                    population = int(row["population"] or 0)
                except ValueError:
                    population = 0
                output.append(City(
                    id=row["id"], name=clean(row["name"]), cc=cc,
                    country=country_names.get(cc, cc),
                    region=region_names.get(f"{cc}.{row['admin1']}", ""),
                    lat=row["lat"], lon=row["lon"], timezone=clean(row["timezone"]),
                    population=population,
                    aliases=base_aliases(row["name"], row["ascii"], row["aliases"], alias_limit),
                ))
    output.sort(key=lambda c: (-c.population, c.cc, norm(c.name), c.id))
    return output


def language_base(value: str) -> str:
    return value.lower().replace("_", "-").split("-", 1)[0]


def enrich_aliases(cities: list[City], zip_path: Path, alias_limit: int) -> list[City]:
    """Prefer current official names and the languages Salat supports at launch."""
    ids = {c.id for c in cities}
    preferred: dict[str, list[str]] = {}
    supported: dict[str, dict[str, list[str]]] = {}

    with zipfile.ZipFile(zip_path) as archive:
        member = next(name for name in archive.namelist() if name.endswith("alternateNamesV2.txt"))
        with archive.open(member) as source:
            for raw in source:
                cols = raw.decode("utf-8").rstrip("\n").split("\t")
                if len(cols) < 8:
                    continue
                geoname_id = cols[1]
                if geoname_id not in ids:
                    continue
                language = cols[2].lower()
                if language in EXCLUDED_ALIAS_LANGUAGES:
                    continue
                value = clean(cols[3]).replace("|", " ")
                if not value or len(value) > 100:
                    continue
                is_preferred = cols[4] == "1"
                is_historic = cols[7] == "1"
                to_period = cols[9] if len(cols) > 9 else ""
                current = not is_historic and not to_period

                if is_preferred and current:
                    preferred.setdefault(geoname_id, []).append(value)

                base = language_base(language)
                if base in SUPPORTED_ALIAS_LANGUAGES:
                    bucket = supported.setdefault(geoname_id, {}).setdefault(base, [])
                    if value not in bucket:
                        if is_preferred:
                            bucket.insert(0, value)
                        elif len(bucket) < 2:
                            bucket.append(value)

    result: list[City] = []
    for city in cities:
        seen = {norm(city.name)}
        merged: list[str] = []

        def add(value: str) -> None:
            key = norm(value)
            if value and key not in seen and len(merged) < alias_limit:
                seen.add(key)
                merged.append(value)

        for value in preferred.get(city.id, []):
            add(value)
        language_map = supported.get(city.id, {})
        for language in ("tr", "ar", "fa", "ur", "bn", "ms", "zh", "en"):
            for value in language_map.get(language, []):
                add(value)
        for value in city.aliases:
            add(value)
        result.append(replace(city, aliases=tuple(merged)))
    return result


def write(cities: list[City], output: Path, dataset: str) -> dict[str, object]:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="\n") as target:
        target.write(f"#salat-city-catalog-v1\tdataset={dataset}\tlicense=CC-BY-4.0\tsource=GeoNames\n")
        for c in cities:
            target.write("\t".join([c.id, c.name, c.cc, c.country, c.region, c.lat, c.lon, c.timezone, str(c.population), "|".join(c.aliases)]) + "\n")
    zipped = output.with_suffix(output.suffix + ".gz")
    with output.open("rb") as source, gzip.GzipFile(filename="", mode="wb", fileobj=zipped.open("wb"), compresslevel=9, mtime=0) as target:
        while chunk := source.read(1024 * 1024):
            target.write(chunk)
    return {"dataset": dataset, "records": len(cities), "raw_bytes": output.stat().st_size, "gzip_bytes": zipped.stat().st_size, "sha256": hashlib.sha256(output.read_bytes()).hexdigest()}


def benchmark(cities: list[City]) -> dict[str, object]:
    started = time.perf_counter()
    index = [(c, norm(" ".join((c.name, c.country, c.region, *c.aliases))), fold(" ".join((c.name, c.country, c.region, *c.aliases)))) for c in cities]
    build_ms = (time.perf_counter() - started) * 1000
    times = {}
    for query in ["Istanbul", "İstanbul", "Kuala", "Karachi", "کراچی", "القاهرة", "北京", "ঢাকা", "تهران", "São Paulo"]:
        q, f = norm(query), fold(query)
        started = time.perf_counter()
        for _ in range(10):
            found = [item[0] for item in index if q in item[1] or (f and f in item[2])]
            found.sort(key=lambda c: (-c.population, c.name))
            _ = found[:30]
        times[query] = round((time.perf_counter() - started) * 100, 2)
    return {"index_build_ms": round(build_ms, 2), "linear_search_ms": times}


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--dataset", required=True)
    p.add_argument("--cities-zip", type=Path, required=True)
    p.add_argument("--country-info", type=Path, required=True)
    p.add_argument("--admin1", type=Path, required=True)
    p.add_argument("--alternate-names-zip", type=Path)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--report", type=Path, required=True)
    p.add_argument("--max-aliases", type=int, default=24)
    a = p.parse_args()
    alias_limit = max(8, min(a.max_aliases, 40))
    city_list = parse(a.cities_zip, a.country_info, a.admin1, alias_limit)
    if a.alternate_names_zip:
        city_list = enrich_aliases(city_list, a.alternate_names_zip, alias_limit)
    report = write(city_list, a.output, a.dataset)
    report["language_aware_aliases"] = bool(a.alternate_names_zip)
    report.update(benchmark(city_list))
    a.report.parent.mkdir(parents=True, exist_ok=True)
    a.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
