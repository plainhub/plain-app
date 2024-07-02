package com.ismartcoding.lib.androidsvg.utils;

import com.ismartcoding.lib.androidsvg.SVG;
import com.ismartcoding.lib.androidsvg.SVGExternalFileResolver;
import com.ismartcoding.lib.androidsvg.SVGParseException;

import java.io.InputStream;

interface SVGParser
{
    /**
     * Try to parse the stream contents to an {@link SVG} instance.
     */
    SVGBase parseStream(InputStream is) throws SVGParseException;

    /**
     * Tells the parser whether to allow the expansion of internal entities.
     * An example of a document containing an internal entities is:
     */
    SVGParser setInternalEntitiesEnabled(boolean enable);

    /**
     * Register an {@link SVGExternalFileResolver} instance that the parser should use when resolving
     * external references such as images, fonts, and CSS stylesheets.
     */
    SVGParser setExternalFileResolver(SVGExternalFileResolver fileResolver);
}