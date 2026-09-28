/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.config


/**
 * Loads an application configuration.
 * An implementation of this interface should return [ApplicationConfig] if applicable configuration is found
 * or `null` otherwise.
 *
 */
public interface ConfigLoader {
    /**
     * Tries loading an application configuration from the specified [path].
     *
     *
     *
     * @return configuration or null if the path is not found or configuration format is not supported.
     */
    public fun load(path: String?): ApplicationConfig?

    public companion object {
        /**
         * Loads application configurations from the specified configuration paths.
         *
         * If no paths are provided, a default configuration is loaded.
         * If a single path is provided, the configuration from the given path is loaded.
         * If multiple paths are provided, the configurations are merged in sequence.
         *
         * @param configPaths A variable number of configuration file paths to load.
         * @return An [ApplicationConfig] instance representing the loaded configuration(s).
         *
         */
        public fun loadAll(vararg configPaths: String): ApplicationConfig =
            when (configPaths.size) {
                0 -> load()
                1 -> load(configPaths.single())
                else -> configPaths.map(::load).reduce(ApplicationConfig::mergeWith)
            }

        /**
         * Find and load a configuration file to [ApplicationConfig].
         *
         */
        public fun load(path: String? = null): ApplicationConfig {
            if (path == null) {
                val default = loadDefault()
                if (default != null) return default
            }

            for (loader in configLoaders) {
                val config = loader.load(path)
                if (config != null) return config
            }

            return MapApplicationConfig()
        }

        private fun loadDefault(): ApplicationConfig? {
            for (defaultPath in CONFIG_PATH) {
                for (loader in configLoaders) {
                    val config = loader.load(defaultPath)
                    if (config != null) return config
                }
            }

            return null
        }
    }
}

/**
 * List of all registered [ConfigLoader] implementations.
 *
 */
