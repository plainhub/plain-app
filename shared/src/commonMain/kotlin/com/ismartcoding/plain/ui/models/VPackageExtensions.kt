package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.platform.DPackageInfo

fun VPackage.Companion.from(data: DPackageInfo): VPackage {
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
