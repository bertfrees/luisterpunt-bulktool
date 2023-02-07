<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                xmlns:px="http://www.daisy.org/ns/pipeline/xproc"
                type="px:odt-to-dtbook">

	<p:option name="source" required="true" px:type="anyFileURI" px:media-type="application/vnd.oasis.opendocument.text"/>
	<p:option name="result" required="true" px:output="result" px:type="anyDirURI" px:media-type="application/x-dtbook+xml"/>

	<p:import href="odt2daisy.xpl">
		<p:documentation>
			px:odt2daisy
		</p:documentation>
	</p:import>
	
	<px:odt2daisy name="odt2daisy" >
		<p:with-option name="source" select="$source"/>
		<p:with-option name="output-dir" select="$result"/>
	</px:odt2daisy>
	<p:sink/>

</p:declare-step>
