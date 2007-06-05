<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
   Licensed to the Apache Software Foundation (ASF) under one
   or more contributor license agreements.  See the NOTICE file
   distributed with this work for additional information
   regarding copyright ownership.  The ASF licenses this file
   to you under the Apache License, Version 2.0 (the
   "License"); you may not use this file except in compliance
   with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing,
   software distributed under the License is distributed on an
   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
   KIND, either express or implied.  See the License for the
   specific language governing permissions and limitations
   under the License.    
-->
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
       <xsl:element name="node">
         <xsl:attribute name="id"><xsl:value-of select="info/@organisation"/>-<xsl:value-of select="info/@module"/></xsl:attribute>
          <data key="d0" >
            <y:ShapeNode>
              <y:Fill color="#CCCCFF"  transparent="false"/>
              <y:BorderStyle type="line" width="1.0" color="#000000" />
              <y:NodeLabel visible="true" alignment="center" fontFamily="Dialog" fontSize="12" fontStyle="plain" textColor="#000000" modelName="internal" modelPosition="c" autoSizePolicy="center">
                <xsl:value-of select="info/@module"/>
              </y:NodeLabel>
              <y:Shape type="roundrectangle"/>
            </y:ShapeNode>
          </data>
       </xsl:element>
      <xsl:for-each select="dependencies/module">
         <xsl:element name="node">
           <xsl:attribute name="id"><xsl:value-of select="@organisation"/>-<xsl:value-of select="@name"/></xsl:attribute>
            <data key="d0" >
              <y:ShapeNode>
                <y:Fill color="#FFFFCC"  transparent="false"/>
                <y:BorderStyle type="line" width="1.0" color="#000000" />
                <y:NodeLabel visible="true" alignment="center" fontFamily="Dialog" fontSize="12" fontStyle="plain" textColor="#000000" modelName="internal" modelPosition="c" autoSizePolicy="center">
                  <xsl:value-of select="@name"/>
                  <xsl:for-each select="revision">
                  <xsl:text>
</xsl:text>
                    <xsl:value-of select="@name"/><xsl:if test="@error"> (error)</xsl:if><xsl:if test="@evicted"> (evicted)</xsl:if>
                  </xsl:for-each>
                </y:NodeLabel>
                <y:Shape type="roundrectangle"/>
              </y:ShapeNode>
            </data>
         </xsl:element>
      </xsl:for-each>
      <xsl:for-each select="dependencies/module/revision[not(@evicted)]/caller">
         <xsl:element name="edge">
           <xsl:attribute name="id"><xsl:value-of select="@organisation"/>-<xsl:value-of select="@name"/>-<xsl:value-of select="../../@organisation"/>-<xsl:value-of select="../../@name"/></xsl:attribute>
           <xsl:attribute name="source"><xsl:value-of select="@organisation"/>-<xsl:value-of select="@name"/></xsl:attribute>
           <xsl:attribute name="target"><xsl:value-of select="../../@organisation"/>-<xsl:value-of select="../../@name"/></xsl:attribute>
            <data key="d1">
              <y:PolyLineEdge>
                <y:LineStyle type="line" width="1.0" color="#000000" />
                <y:Arrows source="none" target="standard"/>
                <y:EdgeLabel visible="true" alignment="center" fontFamily="Dialog" fontSize="12" fontStyle="plain" textColor="#000000" modelName="free" modelPosition="anywhere" preferredPlacement="target" distance="2.0" ratio="0.5">
                  <xsl:value-of select="@rev"/>
                </y:EdgeLabel>
                <y:BendStyle smoothed="false"/>
              </y:PolyLineEdge>
            </data>
         </xsl:element>
      </xsl:for-each>
    </graph>
  </graphml>         
</xsl:template>

</xsl:stylesheet>
