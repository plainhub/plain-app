package com.ismartcoding.lib.kgraphql.schema.directive

import com.ismartcoding.lib.kgraphql.schema.model.FunctionWrapper


class DirectiveExecution(val function: FunctionWrapper<DirectiveResult>) : FunctionWrapper<DirectiveResult> by function