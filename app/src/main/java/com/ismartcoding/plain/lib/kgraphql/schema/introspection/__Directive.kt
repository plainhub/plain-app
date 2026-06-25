package com.ismartcoding.plain.lib.kgraphql.schema.introspection

import com.ismartcoding.plain.lib.kgraphql.schema.directive.DirectiveLocation


interface __Directive : __Described {

    val locations : List<DirectiveLocation>

    val args: List<__InputValue>
}