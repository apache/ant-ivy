<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/ivy-report">
  <graphml xmlns="http://graphml.graphdrawing.org/xmlns/graphml"  
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns/graphml http://www.yworks.com/xml/schema/graphml/1.0/ygraphml.xsd" 
           xmlns:y="http://www.yworks.com/xml/graphml">
    <key id="d0" for="node" yfiles.type="nodegraphics"/>
    <key id="d1" for="edge" yfiles.type="edgegraphics"/>
    <graph id="G" edgedefault="directed">
      <xsl:for-each select="dependencies/module">
         <xsl:element name="node">
           <xsl:attribute name="id"><xsl:value-of select="@organisation"/>-<xsl:value-of select="@name"/></xsl:attribute>
            <data key="d0" >
              <y:ShapeNode>
                <y:Fill color="#FFFFCC"  transparent="false"/>
                <y:BorderStyle type="line" width="1.0" color="#000000" />
                <y:NodeLabel visible="true" alignment="center" fontFamily="Dialog" fontSize="12" fontStyle="plain" textColor="#000000" modelName="internal" modelPosition="c" autoSizePolicy="center">
                  <xsl:value-of select="@name"/>
                </y:NodeLabel>
                <y:Shape type="roundrectangle"/>
              </y:ShapeNode>
            </data>
         </xsl:element>
      </xsl:for-each>
      <xsl:for-each select="dependencies/module/revision[not(@evicted)]/caller[@organisation!='caller']">
         <xsl:element name="edge">
           <xsl:attribute name="id"><xsl:value-of select="@organisation"/>-<xsl:value-of select="@name"/>-<xsl:value-of select="../../@organisation"/>-<xsl:value-of select="../../@name"/></xsl:attribute>
           <xsl:attribute name="source"><xsl:value-of select="@organisation"/>-<xsl:value-of select="@name"/></xsl:attribute>
           <xsl:attribute name="target"><xsl:value-of select="../../@organisation"/>-<xsl:value-of select="../../@name"/></xsl:attribute>
            <data key="d1">
              <y:PolyLineEdge>
                <y:LineStyle type="line" width="1.0" color="#000000" />
                <y:Arrows source="none" target="standard"/>
                <y:BendStyle smoothed="false"/>
              </y:PolyLineEdge>
            </data>
         </xsl:element>
      </xsl:for-each>
    </graph>
  </graphml>         
</xsl:template>

</xsl:stylesheet>
