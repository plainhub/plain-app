#!/usr/bin/env python3
"""
Deduplicate import blocks in lib/ Kotlin sources.

Context: a bulk merge dropped identical blocks of `import` lines into the
same file, so a line like

    import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ASTNode

ends up appearing 2+ times. Kotlin fails to compile with a "Conflicting
imports" error. This script removes the duplicates.

Strategy (conservative, deterministic, order-preserving):
  1. Walk every consecutive block of `import` lines in a file.
  2. Drop a block whose content (line-for-line, order preserved) matches a
     previously seen block in the same file.
  3. Within a single block, drop any line that already appeared earlier in
     the same block.
  4. Everything outside import blocks (package, @file, blank lines, class
     bodies) is emitted verbatim.

Non-goals:
  - We do NOT reorder imports across blocks (kgraphql split by `kotlin.*`
    etc. must stay where they are).
  - We do NOT collapse two non-identical blocks that share lines (those are
    legit different imports).
  - We do NOT touch files outside app/src/main/java/com/ismartcoding/plain/lib.
"""

import argparse
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
LIB_DIR = PROJECT_ROOT / "app/src/main/java/com/ismartcoding/plain/lib"


def dedup_imports(lines: list[str]) -> tuple[list[str], int]:
    """Return (new_lines, removed_count)."""
    out: list[str] = []
    removed = 0
    seen_block_sigs: set[tuple[str, ...]] = set()
    i = 0
    n = len(lines)

    while i < n:
        line = lines[i]
        if line.startswith("import "):
            j = i
            block: list[str] = []
            while j < n and lines[j].startswith("import "):
                block.append(lines[j])
                j += 1

            sig = tuple(b.rstrip("\n") for b in block)
            if sig in seen_block_sigs:
                removed += len(block)
                i = j
                continue

            seen_in_block: set[str] = set()
            new_block: list[str] = []
            for b in block:
                key = b.rstrip("\n")
                if key in seen_in_block:
                    removed += 1
                else:
                    seen_in_block.add(key)
                    new_block.append(b)

            seen_block_sigs.add(sig)
            out.extend(new_block)
            i = j
        else:
            out.append(line)
            i += 1

    return out, removed


def iter_kt_files(root: Path):
    for path in sorted(root.rglob("*.kt")):
        yield path


def process(root: Path, apply: bool) -> tuple[int, int, int]:
    """Return (files_changed, lines_removed, files_scanned)."""
    files_changed = 0
    lines_removed = 0
    files_scanned = 0

    for path in iter_kt_files(root):
        files_scanned += 1
        original = path.read_text(encoding="utf-8").splitlines(keepends=True)
        new_lines, removed = dedup_imports(original)
        if removed == 0:
            continue

        rel = path.relative_to(PROJECT_ROOT)
        print(f"{rel}: remove {removed} duplicate import line(s)")
        lines_removed += removed
        files_changed += 1

        if apply:
            path.write_text("".join(new_lines), encoding="utf-8")

    return files_changed, lines_removed, files_scanned


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[1])
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Write changes to disk (default: dry-run, only prints what would change).",
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=LIB_DIR,
        help=f"Root dir to scan (default: {LIB_DIR}).",
    )
    args = parser.parse_args()

    if not args.root.is_dir():
        print(f"error: {args.root} is not a directory", file=sys.stderr)
        return 1

    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"[{mode}] scanning {args.root}")
    files_changed, lines_removed, files_scanned = process(args.root, args.apply)
    print(
        f"[{mode}] scanned {files_scanned} files, "
        f"changed {files_changed}, removed {lines_removed} duplicate import line(s)"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
