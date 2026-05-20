#!/usr/bin/env python3
"""
Migrate Android string resources to Compose Multiplatform (KMP) composeResources format.

Transformations applied:
  - Copies only string/plurals XML files (skips colors.xml, themes.xml, drawables, etc.)
  - Removes xmlns:tools namespace declaration (not supported by Compose MP Resources)
  - Removes tools:* attributes
  - Removes translatable="false" attributes
  - Replaces %,d with %d  (Java-only locale formatting; not cross-platform on K/Native)

Source:      app/src/main/res/values*/
Destination: shared/src/commonMain/composeResources/values*/
"""

import os
import re
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
SRC_RES = PROJECT_ROOT / "app/src/main/res"
DST_RES = PROJECT_ROOT / "shared/src/commonMain/composeResources"

# Only migrate string/plural XML files
STRING_FILE_PREFIXES = ("strings_", "plurals")

# Dirs to skip entirely
SKIP_DIRS = {"values-night"}


def transform_xml(content: str) -> str:
    # 1. Remove xmlns:tools declaration from <resources> opening tag
    content = re.sub(r'\s+xmlns:tools="[^"]*"', "", content)

    # 2. Remove tools:* attributes (tools:ignore, etc.)
    content = re.sub(r'\s+tools:\w+="[^"]*"', "", content)

    # 3. Remove translatable="false" — Compose MP Resources has no concept of it
    content = re.sub(r'\s+translatable="false"', "", content)

    # 4. Replace %,d (Java locale-aware int format) with %d for cross-platform compat
    content = content.replace("%,d", "%d")

    return content


def main() -> None:
    created = 0
    for src_dir in sorted(SRC_RES.iterdir()):
        if not src_dir.is_dir():
            continue
        if not src_dir.name.startswith("values"):
            continue
        if src_dir.name in SKIP_DIRS:
            continue

        dst_dir_name = src_dir.name
        dst_dir = DST_RES / dst_dir_name
        dst_dir.mkdir(parents=True, exist_ok=True)

        for xml_file in sorted(src_dir.glob("*.xml")):
            if not any(xml_file.stem.startswith(p) for p in STRING_FILE_PREFIXES):
                continue

            raw = xml_file.read_text(encoding="utf-8")
            transformed = transform_xml(raw)
            dst_file = dst_dir / xml_file.name
            dst_file.write_text(transformed, encoding="utf-8")
            print(f"  {dst_dir_name}/{xml_file.name}")
            created += 1

    print(f"\n✓ Migrated {created} files → {DST_RES.relative_to(PROJECT_ROOT)}")


if __name__ == "__main__":
    main()
