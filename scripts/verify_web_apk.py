#!/usr/bin/env python3
"""Verify every bundled web resource survives APK packaging unchanged."""

import argparse
from pathlib import Path
from zipfile import ZipFile


def verify(apk, resources):
    files = sorted(path for path in resources.rglob("*") if path.is_file())
    if not (resources / "index.html").is_file():
        raise ValueError("Web resource directory is missing index.html")
    with ZipFile(apk) as archive:
        entries = set(archive.namelist())
        failures = []
        for path in files:
            name = "web/" + path.relative_to(resources).as_posix()
            if name not in entries:
                failures.append(f"Missing from APK: {name}")
            elif archive.read(name) != path.read_bytes():
                failures.append(f"APK contents differ: {name}")
        if failures:
            raise ValueError("\n".join(failures))
    return len(files)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--resources", type=Path, default=Path(__file__).resolve().parents[1] / "app/src/main/resources/web")
    args = parser.parse_args()
    try:
        print(f"Verified {verify(args.apk, args.resources)} web resources in APK")
    except (ValueError, OSError) as error:
        parser.exit(1, f"{error}\n")
