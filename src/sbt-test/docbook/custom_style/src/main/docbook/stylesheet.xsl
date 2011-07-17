<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  
  <!-- import FO stylesheet -->
  <xsl:import href="http://docbook.sourceforge.net/release/xsl/current/fo/docbook.xsl"/>
  
  <!-- set paper size to A4 -->
  <xsl:param name="paper.type">A4</xsl:param>
  
  <!-- do not generate a ToC -->
  <xsl:param name="generate.toc"></xsl:param>
  
</xsl:stylesheet>