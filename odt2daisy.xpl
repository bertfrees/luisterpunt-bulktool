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
                xmlns:pxi="http://www.daisy.org/ns/pipeline/xproc/internal"
                type="px:odt2daisy">

	<p:option name="source" required="true"/>
	<p:option name="output-dir" required="true"/>
	<p:output port="result.fileset" primary="true"/>
	<p:output port="result.in-memory" sequence="true">
		<p:pipe step="load" port="result.in-memory"/>
	</p:output>

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

	<p:declare-step type="pxi:odt2daisy">
		<p:option name="source" required="true"/>
		<p:option name="output-dir" required="true"/>
		<p:output port="result"/>
		<!--
		    implemented in odt2daisy.java
		-->
	</p:declare-step>

	<pxi:odt2daisy name="odt2daisy">
		<p:with-option name="source" select="$source"/>
		<p:with-option name="output-dir" select="$output-dir"/>
	</pxi:odt2daisy>

	<p:sink/>
	<px:fileset-add-entry media-type="application/x-dtbook+xml" name="dtbook">
		<p:input port="entry">
			<p:pipe step="odt2daisy" port="result"/>
		</p:input>
	</px:fileset-add-entry>
	<px:dtbook-load name="load">
		<p:input port="source.in-memory">
			<p:pipe step="dtbook" port="result.in-memory"/>
		</p:input>
	</px:dtbook-load>

</p:declare-step>
