<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:import href="http://docbook.sourceforge.net/release/xsl/current/epub/docbook.xsl"/>
    <xsl:import href="http://docbook.sourceforge.net/release/xsl/current/epub/highlight.xsl"/>

    <xsl:param name="highlight.source" select="1"/>

    <xsl:param name="highlight.xslthl.config">
        http://docbook.sourceforge.net/release/xsl/current/highlighting/xslthl-config.xml
    </xsl:param>

</xsl:stylesheet>