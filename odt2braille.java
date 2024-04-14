import java.io.File;
import java.util.HashMap;
import java.util.Map;

import be.docarch.odt2braille.xproc.Odt2Braille;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

import net.sf.saxon.s9api.QName;

import org.daisy.common.file.URLs;
import org.daisy.common.spi.ServiceWithProperties;
import org.daisy.common.xproc.calabash.XProcStep;
import org.daisy.common.xproc.calabash.XProcStepProvider;
import org.daisy.common.xproc.XProcMonitor;

public class odt2braille extends Odt2Braille implements XProcStep {

	public odt2braille(XProcRuntime runtime, XAtomicStep step) {
		super(runtime, step);
		File jarFile = new File(URLs.asURI(odt2braille.class.getProtectionDomain().getCodeSource().getLocation()));
		File liblouisDir = new File(jarFile.getParentFile(), "lib/odt2braille-resources");
		if (!liblouisDir.exists())
			throw new IllegalStateException("Directory does not exist: " + liblouisDir);
		setOption(new QName("liblouis-dir"), new RuntimeValue(liblouisDir.toURI().toASCIIString()));
	}

	public static class Provider implements XProcStepProvider, ServiceWithProperties {

		private static final Map spi_props = new HashMap();
		static {
			spi_props.put("component.name", "pxi:odt2braille");
			spi_props.put("type", "{http://www.daisy.org/ns/pipeline/xproc/internal}odt2braille");
		}

		@Override
		public Map spi_getProperties() {
			return spi_props;
		}

		@Override
		public XProcStep newStep(XProcRuntime runtime, XAtomicStep step, XProcMonitor monitor, Map<String,String> properties) {
			return new odt2braille(runtime, step);
		}

		@Override
		public void spi_deactivate() {
		}
	}
}
