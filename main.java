import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ServiceLoader;

import org.daisy.pipeline.script.BoundScript;
import org.daisy.pipeline.script.ScriptRegistry;
import org.daisy.pipeline.job.Job;
import org.daisy.pipeline.job.JobFactory;
import org.daisy.pipeline.job.JobResult;

public class main {

	public static void main(String args[]) {
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
		if (args.length != 2)
			throw new IllegalArgumentException("expected 2 arguments");
		File source = new File(args[0]).getAbsoluteFile();
		File outputDir = new File(args[1]).getAbsoluteFile();
		if (!source.exists())
			throw new IllegalArgumentException("file does not exist: " + source);
		if (outputDir.exists())
			if (!outputDir.isDirectory())
				throw new IllegalArgumentException("file exists: " + outputDir);
			else if (outputDir.listFiles().length > 0)
				throw new IllegalArgumentException("directory is not empty: " + outputDir);
		boolean success = false;
		try {
			ScriptRegistry scriptRegistry = ServiceLoader.load(ScriptRegistry.class).iterator().next();
			BoundScript.Builder boundScript = new BoundScript.Builder(scriptRegistry.getScript("odt-to-dtbook").load())
			                                                 .withInput("source", source);
			JobFactory jobFactory = ServiceLoader.load(JobFactory.class).iterator().next();
			try (Job job = jobFactory.newJob(boundScript.build()).build().get()) {
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
