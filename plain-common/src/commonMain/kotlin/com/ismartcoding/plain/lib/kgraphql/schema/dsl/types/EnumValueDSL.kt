package com.ismartcoding.plain.lib.kgraphql.schema.dsl.types

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.DepreciableItemDSL


class EnumValueDSL<T : Enum<T>>(val value: T) : DepreciableItemDSL()