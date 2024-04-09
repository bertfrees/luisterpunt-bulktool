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

	public static class textToBRL extends XProcScriptService {

		public textToBRL() {
			activate(ImmutableMap.of(SCRIPT_URL,     "/text-to-brl.xpl",
			                         SCRIPT_ID,      "text-to-brl",
			                         SCRIPT_VERSION, "1.0.0"),
			         textToBRL.class);
		}
	}
}
