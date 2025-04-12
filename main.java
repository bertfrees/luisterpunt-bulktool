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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.xml.transform.Source;

import org.daisy.common.messaging.Message;
import org.daisy.common.messaging.MessageAccessor;
import org.daisy.common.messaging.Message.Level;
import org.daisy.common.messaging.ProgressMessage;
import org.daisy.pipeline.braille.common.Query;
import org.daisy.pipeline.css.Medium;
import org.daisy.pipeline.css.CssAnalyzer;
import org.daisy.pipeline.css.CssAnalyzer.SassVariable;
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
		if (outputDir.exists() && !outputDir.isDirectory())
			throw new IllegalArgumentException("file exists: " + outputDir);
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

						for (SassVariable v : new CssAnalyzer(Medium.parse(medium), null, null)
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

				MessageQueue messageQueue = new MessageQueue(job);
				new Thread(job).start();
				running: while (true) {
					for (Message m : messageQueue.getNewMessages())
						System.err.println(m.getText());
					switch (job.getStatus()) {
					case SUCCESS:
					case FAIL:
					case ERROR:
						break running;
					case IDLE:
					case RUNNING:
					default:
						Thread.sleep(1000);
					}
				}
				if (success = (job.getStatus() == Job.Status.SUCCESS)) {
					Collection<JobResult> results = job.getResults().getResults("result");
					boolean someFilesExist = false;
					for (JobResult r : results) {
						File f = new File(outputDir, r.strip().getIdx());
						if (f.exists()) {
							someFilesExist = true;
							break;
						}
					}
					if (someFilesExist && results.size() != 1)
						for (int i = 2; true; i++) {
							File f = new File(outputDir.getParentFile(), outputDir.getName() + " (" + i + ")");
							if (!f.exists()) {
								System.err.println(
									"WARNING: storing results in " + f.getName()
									+ " because some files in " + outputDir.getName() + " would be overwritten");
								outputDir = f;
								break;
							}
						}
					for (JobResult r : results) {
						File f = new File(outputDir, r.strip().getIdx());
						if (f.exists())
							if (results.size() == 1)
								for (int i = 2; true; i++) {
									String nameWithoutExtension = f.getName().replaceAll("\\.[^.]+$", "");
									String extension = f.getName().substring(nameWithoutExtension.length());
									File ff = new File(f.getParentFile(), nameWithoutExtension + " (" + i + ")" + extension);
									if (!ff.exists()) {
										System.err.println(
											"WARNING: storing result to " + ff.getName()
											+ " because " + f.getName() + " would be overwritten");
										f = ff;
										break;
									}
								}
							else
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
				File logFile = new File(outputDir, new SimpleDateFormat("yyMMddHHmmss").format(new Date()) + ".log");
				logFile.getParentFile().mkdirs();
				try (InputStream is = new FileInputStream(new File(job.getLogFile()));
				     OutputStream os = new FileOutputStream(logFile)) {
					byte data[] = new byte[1024];
					int read;
					while ((read = is.read(data)) != -1)
						os.write(data, 0, read);
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

	private static class MessageQueue {

		private final MessageAccessor accessor;
		private int previousLastMessage = -1;

		private MessageQueue(Job job) {
			accessor = job.getMonitor().getMessageAccessor();
		}

		public List<Message> getNewMessages() {
			List<Message> queue = new ArrayList<>();
			int lastMessage = flattenMessages(
				accessor.createFilter()
				        .greaterThan(previousLastMessage)
				        .filterLevels(Collections.singleton(Level.INFO))
				        .getMessages(),
				previousLastMessage + 1,
				queue::add);
			if (lastMessage > previousLastMessage)
				previousLastMessage = lastMessage;
			return queue;
		}

		private int flattenMessages(Iterable<? extends Message> messages, int firstMessage, Consumer<Message> collect) {
			int lastMessage = -1;
			for (Message m : messages) {
				int seq = m.getSequence();
				if (seq >= firstMessage) {
					collect.accept(m);
					if (seq > lastMessage)
						lastMessage = seq;
				}
				if (m instanceof ProgressMessage) {
					seq = flattenMessages((ProgressMessage)m, firstMessage, collect);
					if (seq > lastMessage)
						lastMessage = seq;
				}
			}
			return lastMessage;
		}
	}
}
