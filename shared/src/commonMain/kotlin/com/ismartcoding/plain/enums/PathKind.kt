package com.ismartcoding.plain.enums

/**
 * Kind of a filesystem path. The `pathKind` query serves null when the
 * path does not exist, so the enum itself is a closed FILE/DIR set.
 */
enum class PathKind {
    FILE,
    DIR,
}
