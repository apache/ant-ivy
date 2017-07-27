<?xml version="1.0" encoding="UTF-8"?>
<!--
   Licensed to the Apache Software Foundation (ASF) under one
   or more contributor license agreements.  See the NOTICE file
   distributed with this work for additional information
   regarding copyright ownership.  The ASF licenses this file
   to you under the Apache License, Version 2.0 (the
   "License"); you may not use this file except in compliance
   with the License.  You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing,
   software distributed under the License is distributed on an
   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
   KIND, either express or implied.  See the License for the
   specific language governing permissions and limitations
   under the License.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:param name="confs" select="/ivy-report/info/@confs"/>
<xsl:param name="extension" select="'xml'"/>

<xsl:variable name="myorg" select="/ivy-report/info/@organisation"/>
<xsl:variable name="mymod" select="/ivy-report/info/@module"/>
<xsl:variable name="myconf" select="/ivy-report/info/@conf"/>

<xsl:variable name="modules" select="/ivy-report/dependencies/module"/>
<xsl:variable name="conflicts" select="$modules[count(revision) > 1]"/>

<xsl:variable name="revisions" select="$modules/revision"/>
<xsl:variable name="evicteds" select="$revisions[@evicted]"/>
<xsl:variable name="downloadeds" select="$revisions[@downloaded='true']"/>
<xsl:variable name="searcheds" select="$revisions[@searched='true']"/>
<xsl:variable name="errors" select="$revisions[@error]"/>

<xsl:variable name="artifacts" select="$revisions/artifacts/artifact"/>
<xsl:variable name="cacheartifacts" select="$artifacts[@status='no']"/>
<xsl:variable name="dlartifacts" select="$artifacts[@status='successful']"/>
<xsl:variable name="faileds" select="$artifacts[@status='failed']"/>
<xsl:variable name="artifactsok" select="$artifacts[@status!='failed']"/>

<xsl:template name="calling">
    <xsl:param name="org"/>
    <xsl:param name="mod"/>
    <xsl:param name="rev"/>
    <xsl:if test="count($modules/revision/caller[(@organisation=$org and @name=$mod) and @callerrev=$rev]) = 0">
    <table><tr><td>
    No dependency
    </td></tr></table>
    </xsl:if>
    <xsl:if test="count($modules/revision/caller[(@organisation=$org and @name=$mod) and @callerrev=$rev]) > 0">
    <table class="deps">
      <thead>
      <tr>
        <th>Module</th>
        <th>Revision</th>
        <th>Status</th>
        <th>Resolver</th>
        <th>Default</th>
        <th>Licenses</th>
        <th>Size</th>
        <th></th>
      </tr>
      </thead>
      <tbody>
      <xsl:for-each select="$modules/revision/caller[(@organisation=$org and @name=$mod) and @callerrev=$rev]">
          <xsl:call-template name="called">
              <xsl:with-param name="callstack" select="concat($org, string('/'), $mod)"/>
              <xsl:with-param name="indent"    select="string('')"/>
              <xsl:with-param name="revision"  select=".."/>
          </xsl:call-template>
      </xsl:for-each>
      </tbody>
    </table>
    </xsl:if>
</xsl:template>

<xsl:template name="called">
    <xsl:param name="callstack"/>
    <xsl:param name="indent"/>
    <xsl:param name="revision"/>

    <xsl:param name="organisation" select="$revision/../@organisation"/>
    <xsl:param name="module" select="$revision/../@name"/>
    <xsl:param name="rev" select="$revision/@name"/>
    <xsl:param name="resolver" select="$revision/@resolver"/>
    <xsl:param name="isdefault" select="$revision/@default"/>
    <xsl:param name="status" select="$revision/@status"/>
    <tr>
    <td>
    <xsl:element name="a">
        <xsl:attribute name="href">#<xsl:value-of select="$organisation"/>-<xsl:value-of select="$module"/></xsl:attribute>
        <xsl:value-of select="concat($indent, ' ')"/>
        <xsl:value-of select="$module"/>
        by
        <xsl:value-of select="$organisation"/>
    </xsl:element>
    </td>
    <td>
    <xsl:element name="a">
        <xsl:attribute name="href">#<xsl:value-of select="$organisation"/>-<xsl:value-of select="$module"/>-<xsl:value-of select="$rev"/></xsl:attribute>
        <xsl:value-of select="$rev"/>
    </xsl:element>
    </td>
    <td align="center">
        <xsl:value-of select="$status"/>
    </td>
    <td align="center">
        <xsl:value-of select="$resolver"/>
    </td>
    <td align="center">
        <xsl:value-of select="$isdefault"/>
    </td>
    <td align="center">
    <xsl:call-template name="licenses">
        <xsl:with-param name="revision" select="$revision"/>
    </xsl:call-template>
    </td>
    <td align="center">
        <xsl:value-of select="round(sum($revision/artifacts/artifact/@size) div 1024)"/> kB
    </td>
    <td align="center">
    <xsl:call-template name="icons">
        <xsl:with-param name="revision" select="$revision"/>
    </xsl:call-template>
    </td>
    </tr>
    <xsl:if test="not($revision/@evicted)">
        <xsl:if test="not(contains($callstack, concat($organisation, string('/'), $module)))">
            <xsl:for-each select="$modules/revision/caller[(@organisation=$organisation and @name=$module) and @callerrev=$rev]">
                <xsl:call-template name="called">
                    <xsl:with-param name="callstack" select="concat($callstack, string('#'), $organisation, string('/'), $module)"/>
                    <xsl:with-param name="indent"    select="concat($indent, string('---'))"/>
                    <xsl:with-param name="revision"  select=".."/>
                </xsl:call-template>
            </xsl:for-each>
        </xsl:if>
    </xsl:if>
</xsl:template>

<xsl:template name="licenses">
    <xsl:param name="revision"/>
    <xsl:for-each select="$revision/license">
        <span style="padding-right:3px;">
            <xsl:if test="@url">
                <xsl:element name="a">
                    <xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
                    <xsl:value-of select="@name"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="not(@url)">
                <xsl:value-of select="@name"/>
            </xsl:if>
        </span>
    </xsl:for-each>
</xsl:template>

<xsl:template name="icons">
    <xsl:param name="revision"/>
    <xsl:if test="$revision/@searched = 'true'">
        <svg class="icon" height="1rem" viewBox="0 0 512 512" width="1rem"><g><title>searched</title><desc>required a search in repository</desc><use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#searched" x="0" y="0"/></g></svg>
    </xsl:if>
    <xsl:if test="$revision/@downloaded = 'true'">
        <svg class="icon" height="1rem" viewBox="0 0 512 512" width="1rem"><g><title>downloaded</title><desc>downloaded from repository</desc><use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#downloaded" x="0" y="0"/></g></svg>
    </xsl:if>
    <xsl:if test="$revision/@evicted">
        <svg class="icon" height="1rem" viewBox="0 0 512 512" width="1rem"><g><xsl:element name="title">evicted by <xsl:for-each select="$revision/evicted-by"><xsl:value-of select="@rev"/> </xsl:for-each></xsl:element><desc>evicted by other revisions</desc><use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#evicted" x="0" y="0"/></g></svg>
    </xsl:if>
    <xsl:if test="$revision/@error">
        <svg class="icon" height="1rem" viewBox="0 0 512 512" width="1rem"><g><xsl:element name="title">error: <xsl:value-of select="$revision/@error"/></xsl:element><desc>caused an error during resolution</desc><use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#failed" x="0" y="0"/></g></svg>
    </xsl:if>
</xsl:template>

<xsl:template name="error">
    <xsl:param name="organisation"/>
    <xsl:param name="module"/>
    <xsl:param name="revision"/>
    <xsl:param name="error"/>
    <tr>
    <td>
    <xsl:element name="a">
        <xsl:attribute name="href">#<xsl:value-of select="$organisation"/>-<xsl:value-of select="$module"/></xsl:attribute>
        <xsl:value-of select="$module"/>
        by
        <xsl:value-of select="$organisation"/>
    </xsl:element>
    </td>
    <td>
    <xsl:element name="a">
        <xsl:attribute name="href">#<xsl:value-of select="$organisation"/>-<xsl:value-of select="$module"/>-<xsl:value-of select="$revision"/></xsl:attribute>
        <xsl:value-of select="$revision"/>
    </xsl:element>
    </td>
    <td>
        <xsl:value-of select="$error"/>
    </td>
    </tr>
</xsl:template>

<xsl:template name="confs">
    <xsl:param name="configurations"/>

    <xsl:if test="contains($configurations, ',')">
        <xsl:call-template name="conf">
            <xsl:with-param name="conf" select="normalize-space(substring-before($configurations,','))"/>
        </xsl:call-template>
        <xsl:call-template name="confs">
            <xsl:with-param name="configurations" select="substring-after($configurations,',')"/>
        </xsl:call-template>
    </xsl:if>
    <xsl:if test="not(contains($configurations, ','))">
        <xsl:call-template name="conf">
            <xsl:with-param name="conf" select="normalize-space($configurations)"/>
        </xsl:call-template>
    </xsl:if>
</xsl:template>

<xsl:template name="conf">
    <xsl:param name="conf"/>

     <li>
     <xsl:element name="a">
         <xsl:if test="$conf = $myconf">
             <xsl:attribute name="class">active</xsl:attribute>
         </xsl:if>
         <xsl:attribute name="href"><xsl:value-of select="$myorg"/>-<xsl:value-of select="$mymod"/>-<xsl:value-of select="$conf"/>.<xsl:value-of select="$extension"/></xsl:attribute>
         <xsl:value-of select="$conf"/>
     </xsl:element>
     </li>
</xsl:template>

<xsl:template name="date">
    <xsl:param name="date"/>

    <xsl:value-of select="substring($date,1,4)"/>-<xsl:value-of select="substring($date,5,2)"/>-<xsl:value-of select="substring($date,7,2)"/>
    <xsl:value-of select="' '"/>
    <xsl:value-of select="substring($date,9,2)"/>:<xsl:value-of select="substring($date,11,2)"/>:<xsl:value-of select="substring($date,13)"/>
</xsl:template>


<xsl:template match="/ivy-report">

  <html>
  <head>
    <title>Ivy report :: <xsl:value-of select="info/@module"/> by <xsl:value-of select="info/@organisation"/> :: <xsl:value-of select="info/@conf"/></title>
    <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1"/>
    <meta http-equiv="content-language" content="en"/>
    <meta name="robots" content="index,follow"/>
    <link rel="stylesheet" type="text/css" href="ivy-report.css"/>
  </head>
  <body>
    <div id="logo"><a href="https://ant.apache.org/ivy/"><svg version="1.2" xmlns="https://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 496.711 350.535" xml:space="preserve"><g><path fill="#6E9244" d="M35.696,151.511h26.396c-4.127-3.129-9.725-7.471-9.771-11.101c-0.022-0.832-0.048-1.993-0.118-3.393c-3.201,1.826-15.321,4.34-22.627-2.182c-7.446-6.666-8.729-15.017-6.379-18.951c2.348-3.914,8.87-4.008,8.87-4.008l0.31-3.131c0,0-9.892-5.173-15.844-17.576C10.58,78.766,6.453,63.87,6.453,63.87s6.9-4.363,16.674-2.822c9.771,1.52,14.35,5.504,16.696,4.697C42.955,54.288,70.421,3.413,75.046-0.003C89.633,6.281,89.941,43.307,90.107,46.13s0,23.695,0,26.518c0,2.823,1.873,4.15,5.812,4.555c3.914,0.402,30.785-16.315,37.546-18.522c0,23.767-14.8,45.256-22.318,53.508c-4.625,8.398-1.162,10.011-0.096,13.947c1.066,3.938,1.114,12.547,0.498,14.185c-4.127,1.162-21.252,1.639-27.537-0.521c-6.284-2.158-6.546-5.621-8.441-7.021c-0.166-0.116-11.504-1.541-18.855,1.328l-0.143,6.381c4.222,3.892,7.377,5.455,10.673,11.007h87.496l31.145,90.27l32.921-90.27h98.548l34.893,93.332l30.978-93.332h50.709c0.285-12.265,9.867-10.744,12.713-10.814c-0.616-0.948-3.297-2.895-2.396-5.812c0.902-2.896,7.807-1.99,7.807-1.99
s2.252-5.361,3.318-7.875c1.065-2.516,5.146-3.107,5.146-3.107s-2.467,9.894,1.897,9.894h20.658c0,0-7.565,7.279-10.77,7.853c-3.203,0.592-9.321,3.793-7.424,4.957c1.896,1.162,6.995,4.602,6.115,6.901c-0.875,2.302-13.256-2.303-13.256,0c0,2.302-7.521,5.857-7.142,2.562c0.381-3.297,0.854-9.652-0.643-10.887c-1.518-1.232-5.717-0.9-9.105-0.476c-3.395,0.428-3.203,8.801-3.203,8.801h6.595l-2.086,4.979c17.574,0.688,20.16,18.287,20.896,16.604c0.829-1.922,3.745-8.159,4.34-8.871c2.205-2.682,7.944-2.348,9.319-1.327c2.351,1.729,0.403,5.432,5.054,8.157c7.756,3.345,13.446,2.351,14.941,4.625c-1.33,3.513-7.398,11.338-10.98,12.854c-2.634,1.115-2.896,5.289-2.421,6.264c5.029,10.578,3.728,17.764,2.277,27.418c-12.263-9.416-20.873-20.113-24.81-18.691c-3.938,1.425-18.572,2.517-18.572,2.517s3.818,13.26,2.088,19.662c-1.092,4.031,3.843,3.133,3.843,3.133s4.293-5.075,7.067-6.549c2.774-1.445,6.119-2.396,10.603-1.279c4.556,1.564,5.339,9.062,9.679,11.572c3.201,1.875,17.268,17.125,17.268,17.125s-6.809,3.396-12.567,3.654c-5.767,0.26-14.043-0.05-16.648,0.688c-3.727,1.045-7.662,29.224-14.066,35.271c-7.825-9.252-9.227-21.8-12.002-33.373c-0.877-3.629-3.58-4.554-4.885-4.554s-4.625,2.017-10.674,4.175c-6.05,2.184-14.655-0.9-14.655-0.9l16.48-21.25c0,0-1.562-9.084-0.427-12.073c5.435-5.193,12.855-1.232,12.855-1.232l6.664,6.166c0.235-7.445-2.42-16.246-7.709-20.895c-3.486-3.062-8.562-4.06-11.621-4.365l-46.203,109.813c-21.156,50.281-76.16,40.584-110.955,35.316l-4.149-35.222c13.211,1.685,51.657,8.871,58.487-17.194l-53.53-109.814l-56.282,109.814h-51.775l-61.479-122.08v35.77c0.973,0.068,1.729,0.427,2.373,0.996c1.562,1.425,8.633,6.5,10.032,8.541c3.746,5.404,9.153,4.72,8.323,9.513c-2.11,3.059-5.336,1.09-6.735,0.473c-1.423-0.617-6.119-9.012-7.685-10.271c-1.281-1.021-4.033-4.08-6.285-4.315v18.762c9.535,9.629,22.438,22.318,24.002,23.576c2.515,2.039,5.953,4.078,8.468,4.695c2.515,0.616,5.812,0.78,6.734,1.729c3.297,3.297,2.135,5.979-1.423,6.285c-1.874,0.168-12.404-1.803-14.111-4.008c-1.729-2.205-12.713-14.519-12.713-14.519s-7.067-7.992-9.56-9.416c-0.451-0.26-0.949-0.688-1.424-1.229v55.479H49.293c0.783,1.611,2.586,5.691,1.092,5.834c-1.875,0.166-2.987-0.096-4.08-2.275c-0.332-0.664-0.545-2.02-0.664-3.534h-9.844v-57.47h-0.022c-0.401,0.117-0.782,0.234-1.14,0.354c-2.939,0.098-5.812-0.31-7.377-0.166c-3.485,0.354-15.393,12.664-17.338,16.152c-0.783,1.424-3.771,4.243-5.336,4.694c-1.563,0.476-5.502-2.516-4.388-4.555c1.091-2.041,5.953-4.695,7.065-5.027c1.093-0.311,2.517-4.391,4.246-5.952c1.732-1.564,11.147-7.853,12.713-9.728c1.565-1.873,10.199-1.729,10.199-1.729l1.424,0.143v-25.76c-0.567-0.641-0.974-1.09-1.092-1.281c-1.256-1.969-2.752,1.188-2.275,5.338c0.477,4.148-1.092,11.717-1.092,13.424c0,2.207,5.74,5.408,2.351,7.425c-0.688,0.403-3.394-0.213-5.573-4.293c-2.207-4.078,0-19.306,0-19.306s0.426-8.729,4.056-9.037c1.686-0.145,2.87-0.145,3.651-0.071v-45.443L35.696,151.511z M440.661,160.026l-14.588,34.627c8.684,3.133,15.516,10.293,15.562,10.271c1.803-0.83,2.584-1.303,2.584-3.486c0-2.205,1.092-5.904,1.756-9.463c1.094-5.787-2.467-4.77-3.152-9.463c-0.996-6.879,9.152-10.01,13.066-7.496c1.021,0.641,0.569,2.205,1.686,2.846c1.092,0.617,2.869,0.332,3.227-0.236c1.826-3.084,0.095-3.368-1.896-7.045c-3.561-11.241-14.896-10.08-18.268-10.555H440.661L440.661,160.026z"/><path fill="#FFFFFF" d="M35.72,196.954c0.854,0.07,1.21,0.166,1.21,0.166s1.257,0.168,4.08,3.914c2.823,3.771,4.08,7.142,6.595,9.894c2.516,2.752,4.936,1.021,4.936,1.021s-2.111-11.599-0.546-16.011c1.563-4.387,3.104-5.336,4.672-6.594c1.565-1.258,3.89-3.272,1.687-5.312c-2.207-2.041-3.867-11.388-0.877-17.979c2.986-6.594,9.013-8.42,9.013-8.42s2.134-0.736-1.21-3.629c-0.783-0.688-1.945-1.541-3.229-2.539h5.146l2.704,3.203c8.752-4.224,17.883-0.382,17.883,8.254c1.257,0.946,7.638,3.012,10.604,3.012h15.367c1.566,0,15.371-7.971,17.41-8.113c2.039-0.166,2.11-3.483,9.652-2.465s7.447-0.262,8.539,1.826c1.091,2.108-1.803,4.223-3.534,4.86c-1.729,0.617-11.385-0.354-12.95-0.354c-1.562,0-13.188,5.764-16.861,7.565c-3.678,1.804-11.527,2.441-14.353,0.947c-2.821-1.494-8.491-2.606-8.491-2.606l-4.908-1.4c0,0-0.236,10.604-2.109,15.019c-1.897,4.387-10.127,10.268-12.619,10.268c-2.514,0-1.352,1.994-1.352,1.994c2.537,5.027,1.091,11.787-5.076,16.887c2.585,7.162,14.16-12.76,17.102-12.76c2.656,0,3.201,1.209,2.774,4.031l-9.914,10.861c0,0-3.416,2.274-6.119,2.11c-0.783-0.047-1.494,1.162-1.257,2.894c0.309,2.205-0.402,4.012,0.854,5.102c1.26,1.092,6.168-1.613,7.758-3.486c1.563-1.873,9.105-8.041,13.377-10.127c2.515-1.209,4.34-1.684,5.738-1.588l0.024,4.935c-0.521-0.049-1.044,0.049-1.494,0.381c-2.349,1.73-8.847,6.521-8.847,7.445c0,0.354,2.963,2.351,10.314,10.91v7.143c-2.135-2.371-4.389-6.832-6.167-7.854c-2.183-1.259-4.315-5.336-4.935-6.736c-0.615-1.422-2.987-0.615-3.463,1.094c-0.477,1.73-1.162,3.463-2.608,5.765c-3.463,5.551-9.983,6.047-10.958,7.021c-1.091,1.092,2.585,3.676,3.534,4.791c0.949,1.092,8.373,3.062,10.91,12.478c2.515,9.416,0.022,16.555-1.375,20.325c-3.062,8.185-8.608,8.896-18.287,4.058c-1.115-0.57-9.132-9.466-10.839-13.402c-1.73-3.912,0.166-16.697,0.166-16.697s2.986-4.076,1.562-4.242c-0.592-0.072-0.143-1.826-1.707-1.115c-2.16,0.996-6.355,4.554-6.355,4.554l-0.236,32.473c0,0,1.873-0.098,2.205,2.396c0.309,2.468,0,9.961,0,9.961l0.332,0.664H45.54c-0.285-3.51-0.119-8.041-0.119-8.041s-1.896-3.2-3.012-4.604c-1.092-1.422-1.066-30.287,0.498-34.533c1.564-4.244,2.656-5.881,4.861-5.574c2.206,0.312,5.502-0.166,5.502-0.166s0.783-10.741-0.477-11.383c-1.256-0.617-6.428,2.822-7.209,4.078c-0.711,1.139-6.07,1.66-9.916,2.775l0.072-5.767l3.415,0.332c0,0,3.676-2.514,5.574-3.629c1.874-1.09,7.21-0.897,7.685-2.562c0.477-1.662,0.545-6.998,0.166-7.827c-0.4-0.83-2.562,1.305-4.08-0.119l-12.784-11.953v-7.809h0.023h-0.021L35.72,196.954L35.72,196.954z M52.18,136.687c-0.31-6.664-1.021-17.739-1.139-18.643c-0.166-1.091-6.593-8.941-6.593-11.123c0-2.207-1.709-6.429,2.537-5.17c4.246,1.258,6.595,5.502,6.595,5.502s3.534,5.952,3.534,7.52c0,1.043-0.236,11.884-0.403,19.164l-4.529,2.775v-0.025H52.18z"/></g></svg></a></div>
    <h1>
    <xsl:element name="a">
        <xsl:attribute name="name"><xsl:value-of select="info/@organisation"/>-<xsl:value-of select="info/@module"/></xsl:attribute>
    </xsl:element>
    <span id="module">
        <xsl:value-of select="concat(info/@module, ' ', info/@revision)"/>
    </span>
    by
    <span id="organisation">
        <xsl:value-of select="info/@organisation"/>
    </span>
    </h1>
    <div id="date">
        resolved on
        <xsl:call-template name="date">
            <xsl:with-param name="date" select="info/@date"/>
        </xsl:call-template>
    </div>
    <ul id="confmenu">
        <xsl:call-template name="confs">
            <xsl:with-param name="configurations" select="$confs"/>
        </xsl:call-template>
    </ul>

    <div id="content">
    <h2>Dependencies Stats</h2>
    <table class="header">
        <tr><td class="title">Modules</td><td class="value"><xsl:value-of select="count($modules)"/></td></tr>
        <tr><td class="title">Revisions</td><td class="value"><xsl:value-of select="count($revisions)"/>
        (<xsl:value-of select="count($searcheds)"/> searched <svg class="icon" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="https://www.w3.org/2000/svg" xml:space="preserve" height="1rem" width="1rem" version="1.2" y="0px" x="0px" xmlns:cc="http://creativecommons.org/ns#" xmlns:dc="http://purl.org/dc/elements/1.1/" viewBox="0 0 512 512"><g><title>searched</title><desc>module revisions which required a search with a dependency resolver to be resolved</desc><path id="searched" fill="blue" d="m512 459.67-138.24-138.25c22.397-33.665 37.371-74.752 37.371-115.86 0-112.13-93.43-205.56-205.57-205.56-112.13 0-205.56 93.435-205.56 205.56 0 112.14 93.434 205.58 205.56 205.58 44.848 0 82.198-14.972 115.86-37.373l138.26 138.24 52.326-52.326zm-455.94-254.11c0-82.223 67.272-149.49 149.49-149.49 82.198 0 149.51 67.272 149.51 149.49 0 82.198-67.319 149.51-149.51 149.51-82.223 0-149.49-67.319-149.49-149.51z"/></g></svg>,
        <xsl:value-of select="count($downloadeds)"/> downloaded <svg class="icon" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="https://www.w3.org/2000/svg" xml:space="preserve" height="1rem" width="1rem" version="1.2" y="0px" x="0px" xmlns:cc="http://creativecommons.org/ns#" xmlns:dc="http://purl.org/dc/elements/1.1/" viewBox="0 0 512 512"><g><title>downloaded</title><desc>module revisions for which ivy file was downloaded by dependency resolver</desc><path id="downloaded" fill="limegreen" d="m409.6 204.8h-102.4v-204.8h-102.4v204.8h-102.4l153.6 153.6 153.6-153.6zm-409.6 204.8v102.4h512v-102.4h-512z"/></g></svg>,
        <xsl:value-of select="count($evicteds)"/> evicted <svg class="icon" xmlns:rdf="https://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://www.w3.org/2000/svg" xml:space="preserve" height="1rem" width="1rem" version="1.2" y="0px" x="0px" xmlns:cc="http://creativecommons.org/ns#" xmlns:dc="http://purl.org/dc/elements/1.1/" viewBox="0 0 512 512"><g><title>evicted</title><desc>module revisions which were evicted by others</desc><path id="evicted" fill="fuchsia" d="m256 0c-141.38 0-256 114.62-256 256s114.62 256 256 256c141.37 0 256-114.62 256-256s-114.63-256-256-256zm0 463.99c-114.89 0-207.98-93.098-207.98-207.98s93.1-207.98 207.98-207.98c114.89 0 207.98 93.1 207.98 207.98 0 114.89-93.099 207.98-207.98 207.98zm-95.974-303.96h192v192h-192v-192z"/></g></svg>,
        <xsl:value-of select="count($errors)"/> errors <svg class="icon" xmlns:rdf="https://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://www.w3.org/2000/svg" xml:space="preserve" height="1rem" width="1rem" version="1.2" y="0px" x="0px" xmlns:cc="http://creativecommons.org/ns#" xmlns:dc="http://purl.org/dc/elements/1.1/" viewBox="0 0 512 512"><g><title>error</title><desc>module revisions on which error occurred</desc><path id="failed" fill="red" d="m391.52 155.37-36.872-34.899-98.652 98.652-98.652-98.652-36.872 36.873 98.652 98.652-98.652 98.652 36.872 36.876 98.652-98.652 98.652 98.652 36.872-36.872l-98.65-98.65 98.652-98.652-0.002-1.9728zm-135.52-155.37c-141.16 0-256 114.83-256 256s114.84 256 256 256 256-114.83 256-256-114.83-256-256-256zm0 464.65c-120.79 0-208.65-87.86-208.65-208.65 0-120.8 87.86-208.61 208.65-208.61 120.8 0 208.61 87.815 208.61 208.61 0 120.79-87.816 208.65-208.61 208.65z"/></g></svg>)</td></tr>
        <tr><td class="title">Artifacts</td><td class="value"><xsl:value-of select="count($artifacts)"/>
        (<xsl:value-of select="count($dlartifacts)"/> downloaded,
        <xsl:value-of select="count($faileds)"/> failed)</td></tr>
        <tr><td class="title">Artifacts size</td><td class="value"><xsl:value-of select="round(sum($artifacts/@size) div 1024)"/> kB
        (<xsl:value-of select="round(sum($dlartifacts/@size) div 1024)"/> kB downloaded,
        <xsl:value-of select="round(sum($cacheartifacts/@size) div 1024)"/> kB in cache)</td></tr>
    </table>
    <xsl:if test="count($errors) > 0">
    <h2>Errors</h2>
    <table class="errors">
      <thead>
      <tr>
        <th>Module</th>
        <th>Revision</th>
        <th>Error</th>
      </tr>
      </thead>
      <tbody>
      <xsl:for-each select="$errors">
          <xsl:call-template name="error">
              <xsl:with-param name="organisation" select="../@organisation"/>
              <xsl:with-param name="module"       select="../@name"/>
              <xsl:with-param name="revision"     select="@name"/>
              <xsl:with-param name="error"        select="@error"/>
          </xsl:call-template>
      </xsl:for-each>
      </tbody>
      </table>
    </xsl:if>

    <xsl:if test="count($conflicts) > 0">
    <h2>Conflicts</h2>
    <table class="conflicts">
      <thead>
      <tr>
        <th>Module</th>
        <th>Selected</th>
        <th>Evicted</th>
      </tr>
      </thead>
      <tbody>
      <xsl:for-each select="$conflicts">
        <tr>
        <td>
        <xsl:element name="a">
            <xsl:attribute name="href">#<xsl:value-of select="@organisation"/>-<xsl:value-of select="@name"/></xsl:attribute>
            <xsl:value-of select="@name"/>
            by
            <xsl:value-of select="@organisation"/>
        </xsl:element>
        </td>
        <td>
        <xsl:for-each select="revision[not(@evicted)]">
            <xsl:element name="a">
                <xsl:attribute name="href">#<xsl:value-of select="../@organisation"/>-<xsl:value-of select="../@name"/>-<xsl:value-of select="@name"/></xsl:attribute>
                <xsl:value-of select="@name"/>
            </xsl:element>
            <xsl:text> </xsl:text>
        </xsl:for-each>
        </td>
        <td>
        <xsl:for-each select="revision[@evicted]">
            <xsl:element name="a">
                <xsl:attribute name="href">#<xsl:value-of select="../@organisation"/>-<xsl:value-of select="../@name"/>-<xsl:value-of select="@name"/></xsl:attribute>
                <xsl:value-of select="@name"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="@evicted-reason"/>
            </xsl:element>
            <xsl:text> </xsl:text>
        </xsl:for-each>
        </td>
        </tr>
      </xsl:for-each>
      </tbody>
      </table>
    </xsl:if>

    <h2>Dependencies Overview</h2>
        <xsl:call-template name="calling">
          <xsl:with-param name="org" select="info/@organisation"/>
          <xsl:with-param name="mod" select="info/@module"/>
          <xsl:with-param name="rev" select="info/@revision"/>
        </xsl:call-template>

    <h2>Details</h2>
    <xsl:for-each select="$modules">
    <h3>
      <xsl:element name="a">
         <xsl:attribute name="name"><xsl:value-of select="@organisation"/>-<xsl:value-of select="@name"/></xsl:attribute>
      </xsl:element>
      <xsl:value-of select="@name"/> by <xsl:value-of select="@organisation"/>
    </h3>
      <xsl:for-each select="revision">
        <h4>
          <xsl:element name="a">
             <xsl:attribute name="name"><xsl:value-of select="../@organisation"/>-<xsl:value-of select="../@name"/>-<xsl:value-of select="@name"/></xsl:attribute>
          </xsl:element>
           Revision: <xsl:value-of select="@name"/>
          <span style="padding-left:15px;">
          <xsl:call-template name="icons">
            <xsl:with-param name="revision" select="."/>
          </xsl:call-template>
          </span>
        </h4>
        <table class="header">
                <xsl:if test="@homepage">
            <tr><td class="title">Home Page</td><td class="value">
              <xsl:element name="a">
                    <xsl:attribute name="href"><xsl:value-of select="@homepage"/></xsl:attribute>
                        <xsl:value-of select="@homepage"/>
                </xsl:element></td>
            </tr>
                </xsl:if>
          <tr><td class="title">Status</td><td class="value"><xsl:value-of select="@status"/></td></tr>
          <tr><td class="title">Publication</td><td class="value"><xsl:value-of select="@pubdate"/></td></tr>
          <tr><td class="title">Resolver</td><td class="value"><xsl:value-of select="@resolver"/></td></tr>
          <tr><td class="title">Configurations</td><td class="value"><xsl:value-of select="@conf"/></td></tr>
          <tr><td class="title">Artifacts size</td><td class="value"><xsl:value-of select="round(sum(artifacts/artifact/@size) div 1024)"/> kB
          (<xsl:value-of select="round(sum(artifacts/artifact[@status='successful']/@size) div 1024)"/> kB downloaded,
          <xsl:value-of select="round(sum(artifacts/artifact[@status='no']/@size) div 1024)"/> kB in cache)</td></tr>
          <xsl:if test="count(license) > 0">
              <tr><td class="title">Licenses</td><td class="value">
              <xsl:call-template name="licenses">
                  <xsl:with-param name="revision" select="."/>
              </xsl:call-template>
            </td></tr>
          </xsl:if>
          <xsl:if test="@evicted">
          <tr><td class="title">Evicted by</td><td class="value">
          <b>
              <xsl:for-each select="evicted-by">
                  <xsl:value-of select="@rev"/>
                  <xsl:text> </xsl:text>
              </xsl:for-each>
          </b>
          <xsl:text> </xsl:text>
          <b><xsl:value-of select="@evicted-reason"/></b>
          in <b><xsl:value-of select="@evicted"/></b> conflict manager
          </td></tr>
          </xsl:if>
        </table>
        <h5>Required by</h5>
        <table>
          <thead>
          <tr>
            <th>Organisation</th>
            <th>Name</th>
            <th>Revision</th>
            <th>In Configurations</th>
            <th>Asked Revision</th>
          </tr>
          </thead>
          <tbody>
          <xsl:for-each select="caller">
              <tr>
              <td><xsl:value-of select="@organisation"/></td>
              <td>
              <xsl:element name="a">
                  <xsl:attribute name="href">#<xsl:value-of select="@organisation"/>-<xsl:value-of select="@name"/></xsl:attribute>
                  <xsl:value-of select="@name"/>
              </xsl:element>
              </td>
              <td><xsl:value-of select="@callerrev"/></td>
              <td><xsl:value-of select="@conf"/></td>
              <td><xsl:value-of select="@rev"/></td>
              </tr>
            </xsl:for-each>
          </tbody>
        </table>
        <xsl:if test="not(@evicted)">

        <h5>Dependencies</h5>
        <xsl:call-template name="calling">
            <xsl:with-param name="org" select="../@organisation"/>
            <xsl:with-param name="mod" select="../@name"/>
            <xsl:with-param name="rev" select="@name"/>
        </xsl:call-template>
        <h5>Artifacts</h5>
        <xsl:if test="count(artifacts/artifact) = 0">
        <table><tr><td>
        No artifact
        </td></tr></table>
        </xsl:if>
        <xsl:if test="count(artifacts/artifact) > 0">
        <table>
          <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Ext</th>
            <th>Download</th>
            <th>Size</th>
          </tr>
          </thead>
          <tbody>
          <xsl:for-each select="artifacts/artifact">
              <tr>
              <td><xsl:value-of select="@name"/></td>
              <td><xsl:value-of select="@type"/></td>
              <td><xsl:value-of select="@ext"/></td>
              <td align="center"><xsl:value-of select="@status"/></td>
              <td align="center"><xsl:value-of select="round(number(@size) div 1024)"/> kB</td>
              </tr>
          </xsl:for-each>
          </tbody>
        </table>
        </xsl:if>

        </xsl:if>
      </xsl:for-each>
    </xsl:for-each>
    </div>
  </body>
  </html>
</xsl:template>

</xsl:stylesheet>
