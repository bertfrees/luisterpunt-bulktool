<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                xmlns:px="http://www.daisy.org/ns/pipeline/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:d="http://www.daisy.org/ns/pipeline/data"
                xmlns:css="http://www.daisy.org/ns/pipeline/braille-css"
                type="px:text-to-brf">

	<p:option name="source" required="true" px:type="anyFileURI" px:media-type="application/vnd.oasis.opendocument.text application/x-dtbook+xml"/>
	<p:option name="stylesheet" required="false" select="''" px:sequence="true" px:separator=" " px:type="anyFileURI" px:media-type="text/css text/x-scss"/>
	<p:option name="stylesheet-parameters" required="false" select="''" px:type="transform-query"/>
	<p:option name="temp-dir" required="true" px:output="temp" px:type="anyDirURI"/>
	<p:option name="result" required="true" px:output="result" px:type="anyDirURI"/>

	<p:import href="odt2daisy.xpl">
		<p:documentation>
			px:odt2daisy
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/fileset-utils/library.xpl">
		<p:documentation>
			px:fileset-add-entry
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/braille/dtbook-to-pef/library.xpl">
		<p:documentation>
			px:dtbook-to-pef
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/braille/common-utils/library.xpl">
		<p:documentation>
			px:transform
			px:apply-stylesheets
			px:parse-query
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/dtbook-utils/library.xpl">
		<p:documentation>
			px:dtbook-load
		</p:documentation>
	</p:import>
	<p:import href="http://www.daisy.org/pipeline/modules/braille/pef-utils/library.xpl">
		<p:documentation>
			px:pef-store
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
			<px:odt2daisy name="odt2daisy">
				<p:with-option name="source" select="$source"/>
				<p:with-option name="output-dir" select="concat($temp-dir,'dtbook/')"/>
			</px:odt2daisy>
		</p:when>
		<p:otherwise>
			<p:output port="fileset" primary="true"/>
			<p:output port="in-memory" sequence="true">
				<p:pipe step="load" port="result.in-memory"/>
			</p:output>
			<px:fileset-add-entry media-type="application/x-dtbook+xml" name="fileset">
				<p:with-option name="href" select="$source"/>
			</px:fileset-add-entry>
			<px:dtbook-load name="load">
				<p:input port="source.in-memory">
					<p:pipe step="fileset" port="result.in-memory"/>
				</p:input>
			</px:dtbook-load>
		</p:otherwise>
	</p:choose>

	<!-- HTML to PEF -->
	<px:dtbook-to-pef name="pef">
		<p:input port="source.in-memory">
			<p:pipe step="dtbook" port="in-memory"/>
		</p:input>
		<p:with-option name="temp-dir" select="concat($temp-dir,'pef/')"/>
		<p:with-option name="stylesheet" select="$stylesheet"/>
		<p:input port="parameters">
			<p:pipe step="stylesheet-parameters" port="result"/>
		</p:input>
	</px:dtbook-to-pef>

	<!-- PEF to BRF -->
	<p:group>
		<p:variable name="name" select="replace($source,'^.*/([^/]*)\.[^/\.]*$','$1')"/>
		<px:pef-store>
			<p:with-option name="output-dir" select="$result"/>
			<p:with-option name="name-pattern" select="concat($name,'_vol-{}')"/>
			<p:with-option name="single-volume-name" select="$name"/>
			<p:with-option name="file-format" select="'(table:&quot;org_daisy.EmbosserTableProvider.TableType.DE_DE&quot;)
			                                           (line-breaks:dos)
			                                           (pad:both)
			                                           (charset:&quot;IBM00858&quot;)
			                                           (file-extension:&quot;.brf&quot;)'"/>
		</px:pef-store>
	</p:group>

</p:declare-step>
