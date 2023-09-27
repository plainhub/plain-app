package com.ismartcoding.lib.mustache

import com.ismartcoding.lib.mustache.Mustache.TemplateLoader
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.Throws

/** Compiles templates into executable form. See [Mustache].  */
class Compiler(
    /** Whether or not standards mode is enabled.  */
    val standardsMode: Boolean,
    /** Whether or not to throw an exception when a section resolves to a missing value. If
     * false, the section is simply omitted (or included in the case of inverse sections). If
     * true, a `MustacheException` is thrown.  */
    val strictSections: Boolean,
    /** A value to use when a variable resolves to null. If this value is null (which is the
     * default null value), an exception will be thrown. If [.missingIsNull] is also
     * true, this value will be used when a variable cannot be resolved.
     *
     *
     * If the nullValue contains a substring `{{name}}`, then this substring will be
     * replaced by name of the variable. For example, if nullValue is `?{{name}}?` and
     * the missing variable is `foo`, then string `?foo?` will be used.  */
    val nullValue: String?,
    /** If this value is true, missing variables will be treated like variables that return
     * null. [.nullValue] will be used in their place, assuming [.nullValue] is
     * configured to a non-null value.  */
    val missingIsNull: Boolean,
    /** If this value is true, empty string will be treated as a false value, as in JavaScript
     * mustache implementation. Default is false.  */
    val emptyStringIsFalse: Boolean,
    /** If this value is true, zero will be treated as a false value, as in JavaScript
     * mustache implementation. Default is false.  */
    val zeroIsFalse: Boolean,
    /** Handles converting objects to strings when rendering a template. The default formatter
     * uses [String.valueOf].  */
    val formatter: Mustache.Formatter,
    /** The template loader in use during this compilation.  */
    val loader: TemplateLoader,
    /** The collector used by templates compiled with this compiler.  */
    val collector: ICollector,
    /** The delimiters used by default in templates compiled with this compiler.  */
    val delims: Delims,
) {
    /** Compiles the supplied template into a repeatedly executable intermediate form.  */
    fun compile(template: String): Template {
        return compile(StringReader(template))
    }

    /** Compiles the supplied template into a repeatedly executable intermediate form.  */
    fun compile(source: Reader): Template {
        return Mustache.compile(source, this)
    }

    /** Returns a compiler that will use the given value for any variable that is missing, or
     * otherwise resolves to null. This is like [.nullValue] except that it returns the
     * supplied default for missing keys and existing keys that return null values.  */
    fun defaultValue(defaultValue: String?): Compiler {
        return Compiler(
            standardsMode, strictSections, defaultValue, true,
            emptyStringIsFalse, zeroIsFalse, formatter, loader, collector, delims,
        )
    }

    /** Returns a compiler that will use the given value for any variable that resolves to
     * null, but will still raise an exception for variables for which an accessor cannot be
     * found. This is like [.defaultValue] except that it differentiates between missing
     * accessors, and accessors that exist but return null.
     *
     *  * In the case of a Java object being used as a context, if no field or method can be
     * found for a variable, an exception will be raised.
     *  * In the case of a [Map] being used as a context, if the map does not contain
     * a mapping for a variable, an exception will be raised. If the map contains a mapping
     * which maps to `null`, then `nullValue` is used.
     *   */
    fun nullValue(nullValue: String?): Compiler {
        return Compiler(
            standardsMode, strictSections, nullValue, false,
            emptyStringIsFalse, zeroIsFalse, formatter, loader, collector, delims,
        )
    }

    /** Returns a compiler that will treat empty string as a false value if parameter is true.  */
    fun emptyStringIsFalse(emptyStringIsFalse: Boolean): Compiler {
        return Compiler(
            standardsMode, strictSections, nullValue,
            missingIsNull, emptyStringIsFalse, zeroIsFalse,
            formatter, loader, collector,
            delims,
        )
    }

    /** Returns a compiler that will treat zero as a false value if parameter is true.  */
    fun zeroIsFalse(zeroIsFalse: Boolean): Compiler {
        return Compiler(
            standardsMode, strictSections, nullValue,
            missingIsNull, emptyStringIsFalse, zeroIsFalse,
            formatter, loader, collector,
            delims,
        )
    }

    /** Returns the value to use in the template for the null-valued property `name`. See
     * [.nullValue] for more details.  */
    fun computeNullValue(name: String?): String? {
        return nullValue?.replace("{{name}}", name!!)
    }

    /** Returns true if the supplied value is "falsey". If [.emptyStringIsFalse] is true,
     * then empty strings are considered falsey. If [.zeroIsFalse] is true, then zero
     * values are considered falsey.  */
    fun isFalsey(value: Any): Boolean {
        return emptyStringIsFalse && "" == formatter.format(value) ||
            zeroIsFalse && value is Number && value.toLong() == 0L
    }

    /** Loads and compiles the template `name` using this compiler's configured template
     * loader. Note that this does no caching: the caller should cache the loaded template if
     * they expect to use it multiple times.
     * @return the compiled template.
     * @throw MustacheException if the template could not be loaded (due to I/O exception) or
     * compiled (due to syntax error, etc.).
     */
    @Throws(MustacheException::class)
    fun loadTemplate(name: String): Template {
        var tin: Reader? = null
        return try {
            tin = loader.getTemplate(name)
            compile(tin!!)
        } catch (e: Exception) {
            if (e is RuntimeException) {
                throw e
            } else {
                throw MustacheException("Unable to load template: $name", e)
            }
        } finally {
            if (tin != null) {
                try {
                    tin.close()
                } catch (ioe: IOException) {
                    throw RuntimeException(ioe)
                }
            }
        }
    }
}
