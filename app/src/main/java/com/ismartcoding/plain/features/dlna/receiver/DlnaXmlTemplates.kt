package com.ismartcoding.plain.features.dlna.receiver

import android.os.Build
import com.ismartcoding.plain.features.dlna.common.DlnaSoap

/** Static XML templates for the DLNA receiver's HTTP server. */
object DlnaXmlTemplates {

    fun deviceDescription(ip: String, port: Int, uuid: String): String = """<?xml version="1.0"?>
<root xmlns="urn:schemas-upnp-org:device-1-0">
  <specVersion><major>1</major><minor>0</minor></specVersion>
  <URLBase>http://$ip:$port</URLBase>
  <device>
    <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>
    <friendlyName>Plain (${Build.MODEL})</friendlyName>
    <manufacturer>PlainApp</manufacturer>
    <modelName>PlainApp MediaRenderer</modelName>
    <modelNumber>1</modelNumber>
    <UDN>uuid:$uuid</UDN>
    <serviceList>
      <service>
        <serviceType>${DlnaSoap.AVT_SERVICE_TYPE}</serviceType>
        <serviceId>urn:upnp-org:serviceId:AVTransport</serviceId>
        <controlURL>/AVTransport/control</controlURL>
        <eventSubURL>/AVTransport/event</eventSubURL>
        <SCPDURL>/AVTransport/scpd.xml</SCPDURL>
      </service>
      <service>
        <serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType>
        <serviceId>urn:upnp-org:serviceId:RenderingControl</serviceId>
        <controlURL>/RenderingControl/control</controlURL>
        <eventSubURL>/RenderingControl/event</eventSubURL>
        <SCPDURL>/RenderingControl/scpd.xml</SCPDURL>
      </service>
    </serviceList>
  </device>
</root>"""

    val scpdXml: String = """<?xml version="1.0"?>
<scpd xmlns="urn:schemas-upnp-org:service-1-0">
  <specVersion><major>1</major><minor>0</minor></specVersion>
  <actionList></actionList>
  <serviceStateTable></serviceStateTable>
</scpd>"""
}
