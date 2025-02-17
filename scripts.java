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
