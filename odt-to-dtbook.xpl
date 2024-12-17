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
