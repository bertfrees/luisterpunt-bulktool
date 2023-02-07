<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                xmlns:px="http://www.daisy.org/ns/pipeline/xproc"
                xmlns:pxi="http://www.daisy.org/ns/pipeline/xproc/internal"
                type="px:odt2daisy">

	<p:option name="source" required="true"/>
	<p:option name="output-dir" required="true"/>
	<p:output port="result.fileset" primary="true"/>
	<p:output port="result.in-memory" sequence="true">
		<p:pipe step="load" port="in-memory.out"/>
	</p:output>

	<p:import href="http://www.daisy.org/pipeline/modules/dtbook-utils/library.xpl">
		<p:documentation>
			px:dtbook-load
		</p:documentation>
	</p:import>

	<p:declare-step type="pxi:odt2daisy">
		<p:option name="source" required="true"/>
		<p:option name="output-dir" required="true"/>
		<p:output port="result"/>
		<!--
		    implemented in odt2daisy.java
		-->
	</p:declare-step>

	<pxi:odt2daisy>
		<p:with-option name="source" select="$source"/>
		<p:with-option name="output-dir" select="$output-dir"/>
	</pxi:odt2daisy>

	<px:dtbook-load name="load"/>

</p:declare-step>
