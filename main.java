import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.xml.transform.Source;

import org.daisy.pipeline.braille.common.Query;
import org.daisy.pipeline.css.Medium;
import org.daisy.pipeline.css.sass.SassAnalyzer;
import org.daisy.pipeline.css.sass.SassAnalyzer.SassVariable;
import org.daisy.pipeline.script.BoundScript;
import org.daisy.pipeline.script.ScriptInput;
import org.daisy.pipeline.script.ScriptRegistry;
import org.daisy.pipeline.job.Job;
import org.daisy.pipeline.job.JobFactory;
import org.daisy.pipeline.job.JobResult;

public class main {

	public static void main(String args[]) {
		try {
			_main(args);
		} catch (Throwable e) {
			e.printStackTrace();
			/*System.out.println("Press any key to continue...");
			try {
				System.in.read();
			} catch(IOException ioe) {
			}*/
			System.exit(1);
		}
	}

	private static void _main(String args[]) {
		if (getJavaVersion() < 8) {
			// call self with correct java
			try {
				URL jarFileURL = main.class.getProtectionDomain().getCodeSource().getLocation();
				File jarFile = new File(jarFileURL.toURI());
				File javaHome = new File(jarFile.getParentFile(), "jre");
				if (!javaHome.isDirectory())
					throw new RuntimeException("not a directory: " + javaHome);
				javaHome = javaHome.getCanonicalFile();
				String JAVA_HOME = System.getenv("JAVA_HOME");
				if (JAVA_HOME != null && JAVA_HOME.equals(javaHome.getAbsolutePath()))
					throw new RuntimeException("unexpected version of java found at " + javaHome);
				JAVA_HOME = javaHome.getAbsolutePath();
				File java = new File(new File(new File(JAVA_HOME), "bin"),
				                     System.getProperty("os.name").toLowerCase().startsWith("windows")
				                         ? "java.exe"
				                         : "java");
				if (!java.exists())
					throw new RuntimeException("file does not exist: " + java);
				String[] cmd = new String[3 + args.length];
				cmd[0] = java.getAbsolutePath();
				cmd[1] = "-jar";
				cmd[2] = jarFile.getAbsolutePath();
				System.arraycopy(args, 0, cmd, 3, args.length);
				ProcessBuilder process = new ProcessBuilder(cmd).redirectOutput(ProcessBuilder.Redirect.INHERIT)
				                                                .redirectError(ProcessBuilder.Redirect.INHERIT);
				process.environment().put("JAVA_HOME", JAVA_HOME);
				int rv = process.start().waitFor();
				System.out.flush();
				System.err.flush();
				System.exit(rv);
			} catch (Throwable e) {
				throw new IllegalStateException("unexpected error, contact maintainer", e);
			}
		}
		if (args.length < 1)
			throw new IllegalArgumentException("expected at least one argument");
		String command = args[0];
		Map<String,String> options = null;

		// FIXME: add a command to create multiple formats at once?

		if ("dtbook".equals(command)) {
			if (args.length != 3)
				throw new IllegalArgumentException("expected 3 arguments");
		} else if ("ebraille".equals(command) || "brf".equals(command)) {
			if (args.length < 3)
				throw new IllegalArgumentException("expected at least 3 arguments");
			options = new HashMap<>();
			for (int k = 3; k < args.length; k += 2) {
				String key = args[k];
				if (!key.startsWith("--"))
					throw new IllegalArgumentException("unexpected argument: " + key);
				key = key.substring(2);
				if (options.containsKey(key))
					throw new IllegalArgumentException("duplicate option: " + key);
				if (k + 1 == args.length)
					throw new IllegalArgumentException("expected an argument after " + key);
				options.put(key, args[k + 1]);
			}
		} else {
			throw new IllegalArgumentException("command '" + command + "' not recognized");
		}
		File source = new File(args[1]).getAbsoluteFile();
		File outputDir = new File(args[2]).getAbsoluteFile();
		if (!source.exists())

			// FIXME: add code to regenerate files when sources are newer

			throw new IllegalArgumentException("file does not exist: " + source);
		if (outputDir.exists())
			if (!outputDir.isDirectory())
				throw new IllegalArgumentException("file exists: " + outputDir);
			else if (outputDir.listFiles().length > 0)
				throw new IllegalArgumentException("directory is not empty: " + outputDir);
		boolean success = false;
		try {
			ScriptRegistry scriptRegistry = ServiceLoader.load(ScriptRegistry.class).iterator().next();
			BoundScript.Builder boundScript; {
				if ("dtbook".equals(command)) {
					boundScript = new BoundScript.Builder(scriptRegistry.getScript("odt-to-dtbook").load())
					                             .withInput("source", source);
				} else if ("brf".equals(command) && System.getenv("ODT2BRAILLE") != null) {

					// FIXME: somehow transform options into configuration file?
					// => do this in XProc

					if (!options.isEmpty())
						throw new IllegalArgumentException("invalid option given: " + options.keySet().iterator().next());
					boundScript = new BoundScript.Builder(scriptRegistry.getScript("odt2braille").load())
					                             .withInput("source", source);
				} else {
					boundScript = new BoundScript.Builder(scriptRegistry.getScript("text-to-" + command).load())
					                             .withInput("source", source);
					URL stylesheet = main.class.getResource("/braille.scss");
					// FIXME: ScriptInput.Builder currently does not support jar: URIs, so we must use temporary
					// files as a workaround.
					// Note that we can not use a stream (URL) because of the dedicon-default.scss dependency.
					if ("brf".equals(command) && !"file".equals(stylesheet.getProtocol())) {
						File tmpDir = Files.createTempDirectory(null).toFile();
						File f = new File(tmpDir, "braille.scss");
						Files.copy(stylesheet.openStream(), f.toPath());
						f.deleteOnExit();
						boundScript = boundScript.withInput("stylesheet", f);
						f = new File(tmpDir, "dedicon-default.scss");
						Files.copy(main.class.getResource("/dedicon-default.scss").openStream(), f.toPath());
						f.deleteOnExit();
					} else {
						boundScript = boundScript.withInput("stylesheet", stylesheet);
					}
					Query.MutableQuery stylesheetParameters; {
						stylesheetParameters = Query.util.mutableQuery();
						ScriptInput i = boundScript.build().getInput();
						Source xml = source.getName().endsWith(".xml")
							? i.getInput("source").iterator().next()
							: null;
						String medium = "embossed";

						// not adding page width and height to medium because they are defined in the CSS
						//if ("brf".equals(command)) {
						//	int pageWidth = 33;
						//	int pageHeight = 28;
						//	medium = medium + " AND (width: " + pageWidth + ") AND (height: " + pageHeight + ")";
						//}

						for (SassVariable v : new SassAnalyzer(Medium.parse(medium), null, null)
						                                      .analyze(i.getInput("stylesheet"), xml)
						                                      .getVariables()) {
							String key = v.getName();
							if (v.isDefault()) {
								if (options.containsKey(key))
									stylesheetParameters.add(key, options.remove(key));
							} else {
								// custom style sheet may set variables that are declared in the
								// default style sheet: make sure that the variables in the default
								// style sheet are overridden
								stylesheetParameters.add(key, v.getValue());
							}
						}
						if (!options.isEmpty())
							throw new IllegalArgumentException("invalid option given: " + options.keySet().iterator().next());
					}
					if (!stylesheetParameters.isEmpty())
						boundScript.withOption("stylesheet-parameters", stylesheetParameters.toString());
				}
			}
			JobFactory jobFactory = ServiceLoader.load(JobFactory.class).iterator().next();
			try (Job job = jobFactory.newJob(boundScript.build()).build().get()) {

				// FIXME: execute jobs in parallel?

				job.run();
				if (success = (job.getStatus() == Job.Status.SUCCESS)) {
					for (JobResult r : job.getResults().getResults("result")) {
						File f = new File(outputDir, r.strip().getIdx());
						if (f.exists())
							throw new IllegalStateException("file exists: " + f); // should normally not happen
						f.getParentFile().mkdirs();
						try (InputStream is = r.asStream();
						     OutputStream os = new FileOutputStream(f)) {
							byte data[] = new byte[1024];
							int read;
							while ((read = is.read(data)) != -1)
								os.write(data, 0, read);
						}
					}
				}
			}
		} catch (Throwable e) {
			throw new IllegalStateException("unexpected error, contact maintainer", e);
		}
		if (!success)
			throw new RuntimeException("Job did not succeed");
	}

	private static int getJavaVersion() {
		String v = System.getProperty("java.version");
		if (v.startsWith("1."))
			v = v.substring(2, 3);
		else
			v = v.replaceAll("\\..*", "");
		return Integer.parseInt(v);
	}
}
