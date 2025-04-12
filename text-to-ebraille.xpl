<?xml version="1.0" encoding="UTF-8"?>
<!--

Copyright © 2024, 2025 Luisterpuntbibiotheek

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.

-->
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                xmlns:px="http://www.daisy.org/ns/pipeline/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:d="http://www.daisy.org/ns/pipeline/data"
                xmlns:css="http://www.daisy.org/ns/pipeline/braille-css"
                type="px:text-to-ebraille">

	<p:option name="source" required="true" px:type="anyFileURI" px:media-type="application/vnd.oasis.opendocument.text application/x-dtbook+xml"/>
	<p:option name="stylesheet" required="false" select="''" px:sequence="true" px:separator=" " px:type="anyFileURI" px:media-type="text/css text/x-scss"/>
	<p:option name="stylesheet-parameters" required="false" select="''" px:type="transform-query"/>
	<p:option name="temp-dir" required="true" px:output="temp" px:type="anyDirURI"/>
	<p:option name="result" required="true" px:output="result" px:type="anyDirURI" px:media-type="application/xhtml+xml"/>

	<p:import href="odt2daisy.xpl">
		<p:documentation>
			px:odt2daisy
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/dtbook-to-zedai/library.xpl">
		<p:documentation>
			px:dtbook-to-zedai
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/zedai-to-html/library.xpl">
		<p:documentation>
			px:zedai-to-html
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/fileset-utils/library.xpl">
		<p:documentation>
			px:fileset-load
			px:fileset-update
			px:fileset-store
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/braille/common-utils/library.xpl">
		<p:documentation>
			px:transform
			px:parse-query
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/css-utils/library.xpl">
		<p:documentation>
			px:css-cascade
			px:css-detach
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/fileset-utils/library.xpl">
		<p:documentation>
			px:fileset-add-entry
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/dtbook-utils/library.xpl">
		<p:documentation>
			px:dtbook-load
		</p:documentation>
	</p:import>

	<px:parse-query name="stylesheet-parameters">
		<p:with-option name="query" select="$stylesheet-parameters"/>
	</px:parse-query>
	<p:sink/>

	<!-- Load DTBook or convert ODT to DTBook -->
	<p:choose name="dtbook" px:progress="1/3">
		<p:when test="matches($source,'\.odt$')">
			<p:output port="fileset" primary="true"/>
			<p:output port="in-memory" sequence="true">
				<p:pipe step="odt2daisy" port="result.in-memory"/>
			</p:output>
			<px:odt2daisy name="odt2daisy" >
				<p:with-option name="source" select="$source"/>
				<p:with-option name="output-dir" select="concat($temp-dir,'dtbook/')"/>
			</px:odt2daisy>
		</p:when>
		<p:otherwise>
			<p:output port="fileset" primary="true"/>
			<p:output port="in-memory" sequence="true">
				<p:pipe step="load" port="result.in-memory"/>
			</p:output>
			<px:fileset-add-entry media-type="application/x-dtbook+xml">
				<p:with-option name="href" select="$source"/>
			</px:fileset-add-entry>
			<px:dtbook-load name="load"/>
		</p:otherwise>
	</p:choose>

	<!-- DTBook to ZedAI -->
	<px:dtbook-to-zedai name="zedai" px:progress="1/3">
		<p:input port="source.in-memory">
			<p:pipe step="dtbook" port="in-memory"/>
		</p:input>
		<p:with-option name="output-dir" select="concat($temp-dir,'zedai/')"/>
		<p:with-option name="zedai-filename" select="replace($source,'^.*/([^/]*)\.(odt|xml)$','$1.xml')"/>
	</px:dtbook-to-zedai>

	<!-- ZedAI to HTML -->

	<px:fileset-filter href="zedai-mods.xml" name="filter-mods">
	  <p:input port="source.in-memory">
			<p:pipe step="zedai" port="result.in-memory"/>
		</p:input>
	</px:fileset-filter>
	<p:sink/>

	<px:zedai-to-html name="html" px:progress="1/3">
		<p:input port="fileset.in">
			<p:pipe step="filter-mods" port="not-matched"/>
		</p:input>
		<p:input port="in-memory.in">
			<p:pipe step="filter-mods" port="not-matched.in-memory"/>
		</p:input>
		<p:with-option name="output-dir" select="$result"/>
	</px:zedai-to-html>

	<p:group name="process-html">
		<p:output port="fileset" primary="true"/>
		<p:output port="in-memory" sequence="true">
			<p:pipe step="update" port="result.in-memory"/>
		</p:output>

		<px:fileset-load media-types="application/xhtml+xml" name="load">
			<p:input port="in-memory">
				<p:pipe step="html" port="in-memory.out"/>
			</p:input>
		</px:fileset-load>

		<!-- Transcribe text to braille -->
		<p:for-each>
			<p:variable name="lang" select="(/*/@xml:lang,/*/@lang,'und')[1]"/>
			<px:css-cascade type="text/css text/x-scss" media="braille" name="html-with-css">
				<p:with-option name="user-stylesheet" select="$stylesheet"/>
				<p:input port="parameters">
					<p:pipe step="stylesheet-parameters" port="result"/>
				</p:input>
			</px:css-cascade>
			<px:transform name="transform">
				<p:with-option name="query" select="concat('(input:html)(input:css)(output:html)(output:braille)',
				                                    '(document-locale:',$lang,')')"/>
				<p:input port="parameters">
					<p:pipe step="html-with-css" port="result.parameters"/>
				</p:input>
			</px:transform>
			<px:css-detach name="extract"/>
			<p:delete match="@style"/>
		</p:for-each>
		<p:identity name="html-with-braille"/>
		<p:sink/>

		<px:fileset-update name="update">
			<p:input port="source.fileset">
				<p:pipe step="html" port="fileset.out"/>
			</p:input>
			<p:input port="source.in-memory">
				<p:pipe step="html" port="in-memory.out"/>
			</p:input>
			<p:input port="update.fileset">
				<p:pipe step="load" port="result.fileset"/>
			</p:input>
			<p:input port="update.in-memory">
				<p:pipe step="html-with-braille" port="result"/>
			</p:input>
		</px:fileset-update>
	</p:group>

	<p:add-attribute match="d:file[@media-type='application/xhtml+xml']" attribute-name="indent" attribute-value="true"/>
	<px:fileset-store>
		<p:input port="in-memory.in">
			<p:pipe step="process-html" port="in-memory"/>
		</p:input>
	</px:fileset-store>

</p:declare-step>
