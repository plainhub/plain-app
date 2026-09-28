package com.ismartcoding.plain.lib.kgraphql.schema.directive

import com.ismartcoding.plain.lib.kgraphql.schema.model.FunctionWrapper


class DirectiveExecution(val function: FunctionWrapper<DirectiveResult>) : FunctionWrapper<DirectiveResult> by function