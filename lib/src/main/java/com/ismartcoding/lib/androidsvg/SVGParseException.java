package com.ismartcoding.lib.androidsvg;

import org.xml.sax.SAXException;

public class SVGParseException extends SAXException {
    public SVGParseException(String msg) {
        super(msg);
    }

    public SVGParseException(String msg, Exception cause) {
        super(msg, cause);
    }
}
