/*
 * Copyright © 2024, 2025 Luisterpuntbibiotheek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import org.daisy.pipeline.script.XProcScriptService;

import com.google.common.collect.ImmutableMap;

public final class scripts {
	private scripts() {}

	public static class odtToDTBook extends XProcScriptService {

		public odtToDTBook() {
			activate(ImmutableMap.of(SCRIPT_URL,     "/odt-to-dtbook.xpl",
			                         SCRIPT_ID,      "odt-to-dtbook",
			                         SCRIPT_VERSION, "1.0.0"),
			         odtToDTBook.class);
		}
	}

	public static class textToEbraille extends XProcScriptService {

		public textToEbraille() {
			activate(ImmutableMap.of(SCRIPT_URL,     "/text-to-ebraille.xpl",
			                         SCRIPT_ID,      "text-to-ebraille",
			                         SCRIPT_VERSION, "1.0.0"),
			         textToEbraille.class);
		}
	}

	public static class textToBRF extends XProcScriptService {

		public textToBRF() {
			activate(ImmutableMap.of(SCRIPT_URL,     "/text-to-brf.xpl",
			                         SCRIPT_ID,      "text-to-brf",
			                         SCRIPT_VERSION, "1.0.0"),
			         textToBRF.class);
		}
	}

	public static class odtToBraille extends XProcScriptService {

		public odtToBraille() {
			activate(ImmutableMap.of(SCRIPT_URL,     "/odt2braille.xpl",
			                         SCRIPT_ID,      "odt2braille",
			                         SCRIPT_VERSION, "1.0.0"),
			         odtToBraille.class);
		}
	}
}
