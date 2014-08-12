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
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:output encoding="UTF-8" method="xml" indent="yes" media-type="text/xml"/>

    <xsl:param name="resourceURL"/>
    <xsl:param name="restricted"/>
    <xsl:param name="quiet"/>

    <xsl:variable name="maven2repo" select="'https://repo1.maven.org/maven2/'"/>

    <xsl:template match="/packager-module">
        <xsl:comment> GENERATED FILE - DO NOT EDIT </xsl:comment>
        <project name="packager" default="build">
            <xsl:apply-templates select="property"/>

            <xsl:apply-templates select="resource | m2resource"/>

            <!-- First, download and extract all resources -->
            <target name="resources">
                <xsl:attribute name="depends">
                    <xsl:for-each select="resource | m2resource/artifact">
                        <xsl:if test="position() &gt; 1">
                            <xsl:value-of select="', '"/>
                        </xsl:if>
                        <xsl:value-of select="concat('resource.', generate-id())"/>
                    </xsl:for-each>
                </xsl:attribute>
            </target>

            <!-- Second, put all artifacts into place under artifacts/ -->
            <target name="build" depends="resources">
                <mkdir dir="artifacts/jars"/>
                <mkdir dir="artifacts/sources"/>
                <mkdir dir="artifacts/javadocs"/>
                <!-- ...add some other common artifact types here... -->
                <xsl:apply-templates select="build/*"/>
            </target>
        </project>
    </xsl:template>

    <!-- Properties -->
    <xsl:template match="/packager-module/property">
        <xsl:copy-of select="."/>
    </xsl:template>

    <!-- The allowed build actions in restricted mode -->
    <xsl:template match="/packager-module/build/copy" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/jar" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/mkdir" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/move" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/tar" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/unjar" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/untar" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/unwar" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/unzip" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/war" priority="1"><xsl:copy-of select="."/></xsl:template>
    <xsl:template match="/packager-module/build/zip" priority="1"><xsl:copy-of select="."/></xsl:template>

    <!-- Allow other build actions when restricted="false", otherwise generate error -->
    <xsl:template match="/packager-module/build/*">
        <xsl:choose>
            <xsl:when test="$restricted = 'false'">
                <xsl:copy-of select="."/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="yes">build tag &lt;<xsl:value-of select="name()"/>&gt; not allowed in restricted mode</xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Resource definitions -->
    <xsl:template match="/packager-module/resource">

        <!-- Convert URL into simple filename -->
        <target name="setfilename.{generate-id()}">
            <xsl:choose>
                <xsl:when test="@filename">
                    <property name="filename.{generate-id()}" value="{@filename}"/>
                </xsl:when>
                <xsl:otherwise>
                    <basename property="filename.{generate-id()}" file="{@url}"/>
                </xsl:otherwise>
            </xsl:choose>
        </target>

        <!-- Generate list of URLs to try -->
        <xsl:variable name="urls">
            <xsl:call-template name="concat">
                <xsl:with-param name="nodes" select="@url | url/@href"/>
            </xsl:call-template>
        </xsl:variable>

        <!-- Get resource -->
        <xsl:call-template name="resource">
            <xsl:with-param name="urls" select="$urls"/>
            <xsl:with-param name="type" select="@type"/>
            <xsl:with-param name="csum" select="@sha1"/>
            <xsl:with-param name="dest" select="@dest"/>
            <xsl:with-param name="tofile" select="@tofile"/>
            <xsl:with-param name="filename" select="concat('${filename.', generate-id(), '}')"/>
            <xsl:with-param name="depends" select="concat('setfilename.', generate-id())"/>
        </xsl:call-template>
    </xsl:template>

    <!-- Maven2 resources -->
    <xsl:template match="/packager-module/m2resource">

        <!-- Convert groupId into URL directories, where dots become slashes -->
        <xsl:variable name="groupdirs" select="concat('groupdirs.', generate-id())"/>
        <target name="setgroupdirs.{generate-id()}">
            <xsl:variable name="groupId">
                <xsl:choose>
                    <xsl:when test="@groupId">
                        <xsl:value-of select="@groupId"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'${ivy.packager.organisation}'"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <pathconvert property="{$groupdirs}" dirsep="/">
                <path location="{$groupId}"/>
                <mapper type="unpackage" to="*">
                    <xsl:attribute name="from">
                        <xsl:value-of select="'${basedir}${file.separator}*'"/>
                    </xsl:attribute>
                </mapper>
            </pathconvert>
        </target>

        <!-- Get maven2 artifactId (or use default) -->
        <xsl:variable name="artifactId">
            <xsl:choose>
                <xsl:when test="@artifactId">
                    <xsl:value-of select="@artifactId"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="'${ivy.packager.module}'"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- Get maven2 version (or use default) -->
        <xsl:variable name="version">
            <xsl:choose>
                <xsl:when test="@version">
                    <xsl:value-of select="@version"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="'${ivy.packager.revision}'"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- Get maven2 repository URL (or use default) -->
        <xsl:variable name="repourl">
            <xsl:choose>
                <xsl:when test="@repo and substring(@repo, string-length(@repo) - 1) = '/'">
                    <xsl:value-of select="@repo"/>
                </xsl:when>
                <xsl:when test="@repo">
                    <xsl:value-of select="concat(@repo, '/')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$maven2repo"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- Compose directory in the maven2 repository -->
        <xsl:variable name="m2dir" select="concat($repourl, '${', $groupdirs, '}/', $artifactId, '/', $version, '/')"/>

        <!-- Iterate over artifacts -->
        <xsl:for-each select="artifact">

            <!-- Get classifier (or use default) -->
            <xsl:variable name="classifier">
                <xsl:choose>
                    <xsl:when test="@classifier">
                        <xsl:value-of select="concat('-', @classifier)"/>
                    </xsl:when>
                    <xsl:otherwise/>
                </xsl:choose>
            </xsl:variable>

            <!-- Get classifier (or use default) -->
            <xsl:variable name="suffix">
                <xsl:choose>
                    <xsl:when test="@ext">
                        <xsl:value-of select="concat('.', @ext)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'.jar'"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <!-- Compose to get filename, then complete URL -->
            <xsl:variable name="filename" select="concat($artifactId, '-', $version, $classifier, $suffix)"/>
            <xsl:variable name="url" select="concat($m2dir, $filename)"/>

            <!-- Get resource -->
            <xsl:call-template name="resource">
                <xsl:with-param name="urls" select="$url"/>
                <xsl:with-param name="csum" select="@sha1"/>
                <xsl:with-param name="dest" select="@dest"/>
                <xsl:with-param name="tofile" select="@tofile"/>
                <xsl:with-param name="type" select="@type"/>
                <xsl:with-param name="filename" select="$filename"/>
                <xsl:with-param name="depends" select="concat('setgroupdirs.', generate-id(..))"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <!-- Download and optionally unpack a resource -->
    <xsl:template name="resource">
        <xsl:param name="urls"/>
        <xsl:param name="type"/>
        <xsl:param name="csum"/>
        <xsl:param name="dest"/>
        <xsl:param name="tofile"/>
        <xsl:param name="filename"/>
        <xsl:param name="depends"/>

        <!-- Figure out which directory to download into (at runtime) -->
        <target name="genresdir.1.{generate-id()}" unless="ivy.packager.resourceCache">
            <property name="resdir.{generate-id()}" value="tempdir.{generate-id()}"/>
        </target>
        <target name="genresdir.2.{generate-id()}" if="ivy.packager.resourceCache">
            <property name="resdir.{generate-id()}">
                <xsl:attribute name="value">${ivy.packager.resourceCache}</xsl:attribute>
            </property>
            <echo level="info">
                <xsl:attribute name="message">using resource cache: ${ivy.packager.resourceCache}</xsl:attribute>
            </echo>
        </target>
        <xsl:variable name="resdir" select="concat('${resdir.', generate-id(), '}')"/>
        <xsl:variable name="downloadfile" select="concat($resdir, '${file.separator}', $filename)"/>

        <!-- Create directory for the downloaded resource -->
        <target name="checkdownload.0.{generate-id()}" depends="{$depends}, genresdir.1.{generate-id()}, genresdir.2.{generate-id()}">
            <mkdir dir="{$resdir}"/>
            <condition property="alreadydownloaded.{generate-id()}">
                <and>
                    <available file="{$downloadfile}"/>
                    <checksum file="{$downloadfile}" algorithm="SHA" property="{$csum}"/>
                </and>
            </condition>
        </target>

        <!-- Prepend URL list with resourceURL if configured -->
        <xsl:variable name="urls2">
            <xsl:choose>
                <xsl:when test="$resourceURL">
                    <xsl:value-of select="concat($resourceURL, $filename, ' ', $urls)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$urls"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- Try to download each of the URLs in order -->
        <xsl:call-template name="downloads">
            <xsl:with-param name="urls" select="$urls2"/>
            <xsl:with-param name="filename" select="$filename"/>
            <xsl:with-param name="destfile" select="$downloadfile"/>
            <xsl:with-param name="csum" select="$csum"/>
        </xsl:call-template>

        <!-- Unpack or just copy the file to its destination -->
        <target name="resource.{generate-id()}" depends="download.{generate-id()}">
            <xsl:choose>
                <xsl:when test="$tofile">
                    <copy file="{$downloadfile}" tofile="{$tofile}"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:variable name="realdest">
                        <xsl:choose>
                            <xsl:when test="$dest">
                                <xsl:value-of select="$dest"/>
                            </xsl:when>
                            <xsl:otherwise>archive</xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:choose>
                        <xsl:when test="$type = 'none'">
                            <copy file="{$downloadfile}" todir="{$realdest}"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:variable name="actualtype">
                                <xsl:choose>
                                    <xsl:when test="$type">
                                        <xsl:value-of select="$type"/>
                                    </xsl:when>
                                    <xsl:when test="@filename">
                                        <xsl:call-template name="archiveType">
                                            <xsl:with-param name="file" select="@filename"/>
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="archiveType">
                                            <xsl:with-param name="file" select="$urls"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <xsl:call-template name="unpack">
                                <xsl:with-param name="file" select="$downloadfile"/>
                                <xsl:with-param name="dir" select="$realdest"/>
                                <xsl:with-param name="type" select="$actualtype"/>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </target>
    </xsl:template>

    <!-- Download resource, trying one or more URLs in order -->
    <xsl:template name="downloads">
        <xsl:param name="urls"/>
        <xsl:param name="filename"/>
        <xsl:param name="destfile"/>
        <xsl:param name="csum"/>
        <xsl:param name="index" select="1"/>

        <!-- Parse out first URL from whitespace-separated list -->
        <xsl:variable name="nurls" select="normalize-space($urls)"/>
        <xsl:variable name="isFirstURL" select="$index = 1"/>
        <xsl:variable name="isLastURL" select="not(contains($nurls, ' '))"/>
        <xsl:variable name="url">
            <xsl:choose>
                <xsl:when test="not($isLastURL)">
                    <xsl:value-of select="substring-before($nurls, ' ')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$nurls"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="rurls" select="substring-after($nurls, ' ')"/>

        <!-- Attempt download using this URL; allow download to fail if this is not the last URL -->
        <target name="download.{$index}.{generate-id()}" depends="checkdownload.{$index - 1}.{generate-id()}"
          unless="alreadydownloaded.{generate-id()}">
            <get src="{$url}" dest="{$destfile}" verbose="{string($quiet = 'false')}" ignoreerrors="{string(not($isLastURL))}"/>
        </target>

        <!-- Check whether download attempt was successful -->
        <target name="checkdownload.{$index}.{generate-id()}" depends="download.{$index}.{generate-id()}"
          unless="alreadydownloaded.{generate-id()}">
            <condition property="alreadydownloaded.{generate-id()}">
                <and>
                    <available file="{$destfile}"/>
                    <checksum file="{$destfile}" algorithm="SHA" property="{$csum}"/>
                </and>
            </condition>
        </target>

        <!-- Do final check after last attempt, or recurse -->
        <xsl:choose>
            <xsl:when test="$isLastURL">
                <target name="download.{generate-id()}" depends="checkdownload.{$index}.{generate-id()}"
                  unless="alreadydownloaded.{generate-id()}">
                    <fail message="Unable to download {$filename} from any configured URL">
                        <condition>
                            <not>
                                <available file="{$destfile}"/>
                            </not>
                        </condition>
                    </fail>
                    <fail message="SHA1 checksum verification for {$filename} failed!"/>
                </target>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="downloads">
                    <xsl:with-param name="urls" select="$rurls"/>
                    <xsl:with-param name="filename" select="$filename"/>
                    <xsl:with-param name="destfile" select="$destfile"/>
                    <xsl:with-param name="csum" select="$csum"/>
                    <xsl:with-param name="index" select="$index + 1"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Unpack an archive -->
    <xsl:template name="unpack">
        <xsl:param name="file"/>
        <xsl:param name="dir"/>
        <xsl:param name="type"/>

        <!-- Get nested includes, excludes, etc. -->
        <xsl:variable name="includes" select="*[name() != 'url']"/>

        <!-- Unpack -->
        <mkdir dir="{$dir}"/>
        <xsl:choose>

            <!-- ZIP type files -->
            <xsl:when test="$type = 'zip' or $type = 'war' or $type = 'jar'">
                <unzip src="{$file}" dest="{$dir}">
                    <xsl:if test="$includes">
                        <patternset>
                            <xsl:copy-of select="$includes"/>
                        </patternset>
                    </xsl:if>
                </unzip>
            </xsl:when>

            <!-- TAR files, optionally compressed -->
            <xsl:when test="starts-with($type, 'tar')">
                <untar src="{$file}" dest="{$dir}">
                    <xsl:choose>
                        <xsl:when test="$type = 'tar.gz'">
                            <xsl:attribute name="compression">gzip</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="$type = 'tar.bz2'">
                            <xsl:attribute name="compression">bzip2</xsl:attribute>
                        </xsl:when>
                    </xsl:choose>
                    <xsl:if test="$includes">
                        <patternset>
                            <xsl:copy-of select="$includes"/>
                        </patternset>
                    </xsl:if>
                </untar>
            </xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="yes">
                    <xsl:value-of select="concat('ERROR: unknown archive type &quot;', $type, '&quot;&#10;')"/>
                    <xsl:value-of select="'Please set the &quot;type&quot; attribute to one of: zip, tar, tar.gz, or tar.bz2.&#10;'"/>
                </xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Determine type of archive automatically based on filename -->
    <xsl:template name="archiveType">
        <xsl:param name="file"/>
        <xsl:choose>
            <xsl:when test="substring($file, string-length($file) - 3) = '.jar'">jar</xsl:when>
            <xsl:when test="substring($file, string-length($file) - 3) = '.war'">war</xsl:when>
            <xsl:when test="substring($file, string-length($file) - 3) = '.zip'">zip</xsl:when>
            <xsl:when test="substring($file, string-length($file) - 3) = '.tar'">tar</xsl:when>
            <xsl:when test="substring($file, string-length($file) - 3) = '.tgz'">tar.gz</xsl:when>
            <xsl:when test="substring($file, string-length($file) - 6) = '.tar.gz'">tar.gz</xsl:when>
            <xsl:when test="substring($file, string-length($file) - 7) = '.tar.bz2'">tar.bz2</xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="yes">
                    <xsl:value-of select="concat('ERROR: cannot determine type of archive: ', $file, '&#10;')"/>
                    <xsl:value-of select="'Please set the &quot;type&quot; attribute to one of: zip, tar, tar.gz, or tar.bz2.&#10;'"/>
                </xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Concatenate nodes separated by spaces -->
    <xsl:template name="concat">
        <xsl:param name="nodes"/>
        <xsl:choose>
            <xsl:when test="count($nodes) &lt;= 1">
                <xsl:value-of select="string($nodes)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat(string($nodes[1]), ' ')"/>
                <xsl:call-template name="concat">
                    <xsl:with-param name="nodes" select="$nodes[position() &gt; 1]"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Ignore anything unexpected -->
    <xsl:template match="*">
        <xsl:message terminate="no">ignoring unexpected XML node &lt;<xsl:value-of select="name()"/>&gt;</xsl:message>
    </xsl:template>
    <xsl:template match="@*|node()"/>

</xsl:transform>
