import java.io.File;
import java.net.URL;

import com.versusoft.packages.ooo.odt2daisy.Odt2Daisy;

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
		File odt = new File(args[0]).getAbsoluteFile();
		File dtb = new File(args[1]).getAbsoluteFile();
		if (!odt.exists())
			throw new IllegalArgumentException("file does not exist: " + odt);
		if (dtb.exists())
			throw new IllegalArgumentException("file exists: " + dtb);
		File imagesDir = new File(dtb.getParentFile(), dtb.getName() + ".images");
		if (imagesDir.exists())
			if (!imagesDir.isDirectory())
				throw new IllegalArgumentException("file is not a directory: " + imagesDir);
			else if (imagesDir.list().length == 0)
				throw new IllegalArgumentException("directory is not empty: " + imagesDir);
		try {
			Odt2Daisy odt2daisy = new Odt2Daisy(odt.getPath());
			odt2daisy.init();
			odt2daisy.setUidParam("no-uid");
			odt2daisy.setWriteCSSParam(false);
			odt2daisy.paginationProcessing();
			odt2daisy.correctionProcessing();
			dtb.getParentFile().mkdirs();
			odt2daisy.convertAsDTBook(dtb.getPath(), imagesDir.getName());
			if (!new Odt2Daisy(null).validateDTD(dtb.getPath())) {
				System.err.println("produced invalid DTBook: " + dtb);
				System.exit(1);
			}
		} catch (Throwable e) {
			throw new IllegalStateException("unexpected error, contact maintainer", e);
		}
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
