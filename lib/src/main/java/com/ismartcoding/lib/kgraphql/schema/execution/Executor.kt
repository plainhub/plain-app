package com.ismartcoding.lib.kgraphql.schema.execution

enum class Executor {
    Parallel,

    /**
     * **This is in experimental state**
     *
     * * Subscriptions are not supported
     * * Ordering of object fields are not guaranteed
     * * Some configuration options are not taken into account when using this executor
     */
    DataLoaderPrepared
}
