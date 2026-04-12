package com.ismartcoding.plain.features.dlna.common

/** Shared UPnP/SOAP namespaces and envelope builders used by both sender and receiver. */
object DlnaSoap {
    const val ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/"
    const val ENCODING_NS = "http://schemas.xmlsoap.org/soap/encoding/"
    const val AVT_SERVICE_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"

    fun requestEnvelope(body: String): String =
        """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope s:encodingStyle="$ENCODING_NS" xmlns:s="$ENVELOPE_NS">
    <s:Body>
        $body
    </s:Body>
</s:Envelope>""".trimIndent()

    fun responseEnvelope(action: String, elements: String = ""): String =
        """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="$ENVELOPE_NS" s:encodingStyle="$ENCODING_NS">
  <s:Body>
    <u:${action}Response xmlns:u="$AVT_SERVICE_TYPE">
      $elements
    </u:${action}Response>
  </s:Body>
</s:Envelope>"""
}
