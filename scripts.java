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
}
