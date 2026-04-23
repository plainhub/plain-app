package com.ismartcoding.lib.kgraphql.schema.dsl.types

import com.ismartcoding.lib.kgraphql.schema.dsl.DepreciableItemDSL


class EnumValueDSL<T : Enum<T>>(val value: T) : DepreciableItemDSL()