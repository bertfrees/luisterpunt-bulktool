<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                xmlns:px="http://www.daisy.org/ns/pipeline/xproc"
                xmlns:pxi="http://www.daisy.org/ns/pipeline/xproc/internal"
                type="px:odt2braille">

	<p:option name="source" required="true" px:type="anyFileURI" px:media-type="application/vnd.oasis.opendocument.text"/>
	<p:option name="result" required="true" px:output="result" px:type="anyDirURI"/>

	<p:import href="http://www.daisy.org/pipeline/modules/braille/pef-utils/library.xpl">
		<p:documentation>
			px:pef-store
		</p:documentation>
	</p:import>

	<p:declare-step type="pxi:odt2braille">
		<p:option name="source" required="true"/>
		<p:input port="config" sequence="true"/>
		<p:output port="result"/>
		<!--
		    implemented in odt2braille.java
		-->
	</p:declare-step>

	<pxi:odt2braille>
		<p:with-option name="source" select="$source"/>
		<p:input port="config">
			<p:empty/>
		</p:input>
	</pxi:odt2braille>

	<!-- PEF to BRL -->
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
			                                           (file-extension:&quot;.brl&quot;)'"/>
		</px:pef-store>
	</p:group>

</p:declare-step>
