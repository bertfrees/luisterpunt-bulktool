import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.collect.ImmutableMap;

import com.versusoft.packages.ooo.odt2daisy.Odt2Daisy;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;

import org.daisy.common.saxon.SaxonOutputValue;
import org.daisy.common.spi.ServiceWithProperties;
import org.daisy.common.transform.InputValue;
import org.daisy.common.transform.OutputValue;
import org.daisy.common.transform.TransformerException;
import org.daisy.common.transform.XMLOutputValue;
import org.daisy.common.transform.XMLTransformer;
import org.daisy.common.xproc.calabash.XMLCalabashOutputValue;
import org.daisy.common.xproc.calabash.XProcStep;
import org.daisy.common.xproc.calabash.XProcStepProvider;
import org.daisy.common.xproc.XProcMonitor;

import org.xml.sax.SAXException;

public class odt2daisy implements XMLTransformer {

	private final File odtFile;
	private final File outputDir;
	private final XProcRuntime runtime;

	private final static QName _RESULT = new QName("result");

	public odt2daisy(File odtFile, File outputDir, XProcRuntime runtime) {
		this.odtFile = odtFile;
		this.outputDir = outputDir;
		this.runtime = runtime;
	}

	@Override
	public Runnable transform(Map<QName,InputValue<?>> input, Map<QName,OutputValue<?>> output) {
		input = XMLTransformer.validateInput(input, null);
		output = XMLTransformer.validateOutput(output, ImmutableMap.of(_RESULT, OutputType.NODE_SEQUENCE));
		XMLOutputValue<?> result = (XMLOutputValue<?>)output.get(_RESULT);
		if (!(result instanceof SaxonOutputValue))
			throw new IllegalArgumentException();
		return () -> {
			try {
				DocumentBuilder parser = runtime.getProcessor().newDocumentBuilder();
				((SaxonOutputValue)result).asXdmItemConsumer().accept(
					parser.build(transform(odtFile, outputDir)));
			} catch (IllegalArgumentException |
			         IllegalStateException e) {
				throw new TransformerException(e);
			} catch (SaxonApiException e) {
				throw new RuntimeException(e); // unexpected
			}
		};
	}

	private static File transform(File odtFile, File outputDir) throws IllegalArgumentException, IllegalStateException {
		odtFile = odtFile.getAbsoluteFile();
		if (!odtFile.exists())
			throw new IllegalArgumentException("file does not exist: " + odtFile);
		else if (odtFile.isDirectory())
			throw new IllegalArgumentException("file is a directory: " + odtFile);
		File dtbFile = new File(outputDir, odtFile.getName().replaceAll("\\.odt$", "") + ".xml");
		if (dtbFile.exists())
			throw new IllegalArgumentException("file exists: " + dtbFile);
		File imagesDir = new File(outputDir, "images");
		if (imagesDir.exists())
			if (!imagesDir.isDirectory())
				throw new IllegalArgumentException("file is not a directory: " + imagesDir);
			else if (imagesDir.list().length == 0)
				throw new IllegalArgumentException("directory is not empty: " + imagesDir);
		try {
			Odt2Daisy odt2daisy = new Odt2Daisy(odtFile.getPath());
			odt2daisy.init();
			odt2daisy.setUidParam("no-uid");
			odt2daisy.setWriteCSSParam(false);
			odt2daisy.paginationProcessing();
			odt2daisy.correctionProcessing();
			outputDir = outputDir.getAbsoluteFile();
			imagesDir.mkdirs();
			odt2daisy.convertAsDTBook(dtbFile.getPath(), imagesDir.getName());
		} catch (IOException |
		         SAXException |
		         ParserConfigurationException |
		         javax.xml.transform.TransformerException e) {
			throw new RuntimeException(e); // unexpected
		}
		if (!validateDTBook(dtbFile))
			throw new IllegalStateException("produced invalid DTBook: " + dtbFile);
		return dtbFile;
	}

	private static boolean validateDTBook(File dtbFile) {
		try {
			return new Odt2Daisy(null).validateDTD(dtbFile.getPath());
		} catch (IOException |
		         SAXException |
		         ParserConfigurationException |
		         javax.xml.transform.TransformerException e) {
			throw new RuntimeException(e); // unexpected
		}
	}

	public static class Step extends DefaultStep implements XProcStep {

		public static class Provider implements XProcStepProvider, ServiceWithProperties {

			private static final Map spi_props = new HashMap();
			static {
				spi_props.put("component.name", "pxi:odt2daisy");
				spi_props.put("type", "{http://www.daisy.org/ns/pipeline/xproc/internal}odt2daisy");
			}

			@Override
			public Map spi_getProperties() {
				return spi_props;
			}

			@Override
			public XProcStep newStep(XProcRuntime runtime, XAtomicStep step, XProcMonitor monitor, Map<String,String> properties) {
				return new Step(runtime, step);
			}

			@Override
			public void spi_deactivate() {
			}
		}

		private Step(XProcRuntime runtime, XAtomicStep step) {
			super(runtime, step);
		}

		private String sourceOption = null;
		private String outputDirOption = null;
		private WritablePipe resultPipe = null;

		@Override
			public void setOutput(String port, WritablePipe pipe) {
			resultPipe = pipe;
		}

		@Override
			public void reset() {
			resultPipe.resetWriter();
		}

		@Override
			public void setOption(net.sf.saxon.s9api.QName name, RuntimeValue value) {
			if ("source".equals(name.getLocalName()))
				sourceOption = value.getString();
			else if ("output-dir".equals(name.getLocalName()))
				outputDirOption = value.getString();
		}

		@Override
			public void run() throws SaxonApiException {
			super.run();
			try {
				new odt2daisy(
					new File(URI.create(sourceOption)),
					new File(URI.create(outputDirOption)),
					runtime
				).transform(
					ImmutableMap.of(),
					ImmutableMap.of(new QName("result"), new XMLCalabashOutputValue(resultPipe, runtime))
				).run();
			} catch (Throwable e) {
				throw XProcStep.raiseError(e, step);
			}
		}
	}
}
