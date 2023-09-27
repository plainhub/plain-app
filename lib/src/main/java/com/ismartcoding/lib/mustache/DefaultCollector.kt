package com.ismartcoding.lib.mustache

import com.ismartcoding.lib.mustache.Mustache.VariableFetcher
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * The default collector used by JMustache.
 */
class DefaultCollector
    @JvmOverloads
    constructor(private val _allowAccessCoercion: Boolean = true) : BasicCollector() {
        override fun createFetcher(
            ctx: Any,
            name: String,
        ): VariableFetcher? {
            val fetcher = super.createFetcher(ctx, name)
            if (fetcher != null) return fetcher

            // first check for a getter which provides the value
            val cclass = ctx.javaClass
            val m = getMethod(cclass, name)
            if (m != null) {
                return object : VariableFetcher {
                    override fun get(
                        ctx: Any,
                        name: String,
                    ): Any {
                        return m.invoke(ctx)!!
                    }
                }
            }

            // next check for a getter which provides the value
            val f = getField(cclass, name)
            if (f != null) {
                return object : VariableFetcher {
                    override fun get(
                        ctx: Any,
                        name: String,
                    ): Any {
                        return f[ctx]!!
                    }
                }
            }

            // finally check for a default interface method which provides the value (this is left to
            // last because it's much more expensive and hopefully something already matched above)
            val im = getIfaceMethod(cclass, name)
            return if (im != null) {
                object : VariableFetcher {
                    override fun get(
                        ctx: Any,
                        name: String,
                    ): Any {
                        return im.invoke(ctx)!!
                    }
                }
            } else {
                null
            }
        }

        override fun <K, V> createFetcherCache(): MutableMap<K, V> {
            return ConcurrentHashMap()
        }

        protected fun getMethod(
            clazz: Class<*>,
            name: String,
        ): Method? {
            if (_allowAccessCoercion) {
                // first check up the superclass chain
                var cc: Class<*>? = clazz
                while (cc != null && cc != Any::class.java) {
                    val m = getMethodOn(cc, name)
                    if (m != null) return m
                    cc = cc.superclass
                }
            } else {
                // if we only allow access to accessible methods, then we can just let the JVM handle
                // searching superclasses for the method
                try {
                    return clazz.getMethod(name)
                } catch (e: Exception) {
                    // fall through
                }
            }
            return null
        }

        protected fun getIfaceMethod(
            clazz: Class<*>?,
            name: String,
        ): Method? {
            // enumerate the transitive closure of all interfaces implemented by clazz
            val ifaces: MutableSet<Class<*>> = LinkedHashSet()
            var cc = clazz
            while (cc != null && cc != Any::class.java) {
                addIfaces(ifaces, cc, false)
                cc = cc.superclass
            }
            // now search those in the order that we found them
            for (iface in ifaces) {
                val m = getMethodOn(iface, name)
                if (m != null) return m
            }
            return null
        }

        private fun addIfaces(
            ifaces: MutableSet<Class<*>>,
            clazz: Class<*>,
            isIface: Boolean,
        ) {
            if (isIface) ifaces.add(clazz)
            for (iface in clazz.interfaces) addIfaces(ifaces, iface, true)
        }

        private fun getMethodOn(
            clazz: Class<*>,
            name: String,
        ): Method? {
            var m: Method
            try {
                m = clazz.getDeclaredMethod(name)
                if (m.returnType != Void.TYPE) return makeAccessible(m)
            } catch (e: Exception) {
                // fall through
            }
            val upperName = name[0].uppercaseChar().toString() + name.substring(1)
            try {
                m = clazz.getDeclaredMethod("get$upperName")
                if (m.returnType != Void.TYPE) return makeAccessible(m)
            } catch (e: Exception) {
                // fall through
            }
            try {
                m = clazz.getDeclaredMethod("is$upperName")
                if (m.returnType == Boolean::class.javaPrimitiveType || m.returnType == Boolean::class.java) return makeAccessible(m)
            } catch (e: Exception) {
                // fall through
            }
            return null
        }

        private fun makeAccessible(m: Method): Method? {
            if (m.isAccessible) {
                return m
            } else if (!_allowAccessCoercion) {
                return null
            }
            m.isAccessible = true
            return m
        }

        private fun getField(
            clazz: Class<*>,
            name: String,
        ): Field? {
            if (!_allowAccessCoercion) {
                return try {
                    clazz.getField(name)
                } catch (e: Exception) {
                    null
                }
            }
            val f: Field
            try {
                f = clazz.getDeclaredField(name)
                if (!f.isAccessible) {
                    f.isAccessible = true
                }
                return f
            } catch (e: Exception) {
                // fall through
            }
            val sclass = clazz.superclass
            return if (sclass != Any::class.java && sclass != null) {
                getField(clazz.superclass, name)
            } else {
                null
            }
        }
    }
