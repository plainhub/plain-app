package com.ismartcoding.plain.platform

import com.ismartcoding.plain.enums.AppFeatureType

/** Publishes the user-picked tools as launcher long-press shortcuts; no-op where unsupported. */
expect fun publishLauncherShortcuts(tools: List<AppFeatureType>)
