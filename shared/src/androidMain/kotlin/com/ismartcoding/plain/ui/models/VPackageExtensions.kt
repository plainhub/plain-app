package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.data.DPackage

fun VPackage.Companion.from(data: DPackage): VPackage {
    return VPackage(
        data.id,
        data.name,
        data.type,
        data.version,
        data.path,
        data.size,
        data.certs,
        data.installedAt,
        data.updatedAt,
    )
}
