#!/usr/bin/env python3
"""Convert scripts/feeds-catalog-seed.opml to the bundled
shared/src/commonMain/composeResources/files/feeds_catalog.json.

Folder outlines map to category ids; display names live in strings_feeds.xml
so they translate. Validates: known folder, unique absolute http(s) urls.

Usage: python3 scripts/opml_to_catalog.py [in.opml] [out.json]
"""
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SRC = ROOT / "scripts/feeds-catalog-seed.opml"
DEFAULT_DST = ROOT / "shared/src/commonMain/composeResources/files/feeds_catalog.json"

FOLDER_TO_ID = {
    "科技 · 中文": "tech_cn",
    "技术 · 中文": "dev_cn",
    "科技 · 英文": "tech_en",
    "新闻 · 英文": "news_en",
    "阅读 · 科学": "reads",
}


def site_of(url: str) -> str:
    host = urlparse(url).netloc
    return host.removeprefix("www.")


def outline_name(o: ET.Element) -> str:
    return o.get("text") or o.get("title") or ""


def convert(src: Path, dst: Path) -> None:
    body = ET.parse(src).getroot().find("body")
    if body is None:
        raise SystemExit(f"{src}: no <body>")
    categories = []
    seen = set()
    for folder in body.findall("outline"):
        folder_name = outline_name(folder)
        if folder_name not in FOLDER_TO_ID:
            raise SystemExit(f"{src}: unknown folder: {folder_name!r}")
        feeds = []
        for o in folder.findall("outline"):
            url = o.get("xmlUrl") or ""
            if not url.startswith(("http://", "https://")):
                raise SystemExit(f"{src}: bad xmlUrl: {url!r}")
            if url in seen:
                raise SystemExit(f"{src}: duplicate url: {url}")
            seen.add(url)
            html = o.get("htmlUrl") or url
            feeds.append(
                {"name": outline_name(o), "url": url, "site": site_of(html)}
            )
        categories.append({"id": FOLDER_TO_ID[folder_name], "feeds": feeds})
    if not categories:
        raise SystemExit(f"{src}: no categories")

    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(
        json.dumps(categories, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    total = sum(len(c["feeds"]) for c in categories)
    print(f"{dst.relative_to(ROOT)}: {total} feeds in {len(categories)} categories")


if __name__ == "__main__":
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SRC
    dst = Path(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_DST
    convert(src, dst)
