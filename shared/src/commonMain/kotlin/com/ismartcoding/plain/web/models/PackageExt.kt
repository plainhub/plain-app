package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.platform.DPackageInfo

fun DPackageInfo.toModel(): Package {
    return Package(
        ID(id), name, type, version, path, size,
        certs.map { Certificate(it.issuer, it.subject, it.serialNumber, it.validFrom, it.validTo) },
        installedAt, updatedAt,
    )
}
