include java-shell-for-make/enable-java-shell.mk

.PHONY : all
all : check dist

.PHONY : clean
clean :
	@glob("lib/*.jar").forEach(x -> rm(x));             \
	rm("java-shell-for-make/recipes");                  \
	rm("dist");                                         \
	rm("dist-check");                                   \
	rm("main.class");                                   \
	rm("odt2daisy.class");                              \
	rm("odt2daisy$$1.class");                           \
	rm("odt2daisy$$Step.class");                        \
	rm("odt2daisy$$Step$$Provider.class");              \
	rm("scripts.class");                                \
	rm("scripts$$odtToDTBook.class");                   \
	rm("scripts$$textToEbraille.class");                \
	rm("main.jar");                                     \
	rm("jre-win64");                                    \
	rm("jre-mac");                                      \
	rm("classpath.txt");                                \
	rm("target");                                       \
	rm("OpenJDK11U-jdk_x64_mac_hotspot_11.0.13_8");     \
	rm("OpenJDK11U-jdk_x64_windows_hotspot_11.0.13_8"); \
	rm("dtb");                                          \
	rm("ebraille");                                     \
	rm("xprocspec-reports");                            \
	exec(new File("lib/odt2daisy"), "ant", "clean");

ifneq ($(MAKECMDGOALS),clean)

LIBS := $(shell                                                                             \
	/* compile odt2daisy */                                                                 \
	if (!new File("lib/odt2daisy/dist/odt2daisy.jar").exists()) {                           \
		StringWriter output = new StringWriter();                                           \
		int rv =                                                                            \
			captureOutput(                                                                  \
				err::println,                                                               \
				new File("lib/odt2daisy"),                                                  \
				"ant", "jar");                                                              \
		if (rv != 0) {                                                                      \
			err.println(output);                                                            \
			err.println("Failed to compile odt2daisy");                                     \
			exit(rv); }}                                                                    \
	/* download Maven dependencies */                                                       \
	if (glob("lib/*.jar").isEmpty()) {                                                      \
		StringWriter output = new StringWriter();                                           \
		int rv =                                                                            \
			captureOutput(                                                                  \
				err::println,                                                               \
				new File("lib"),                                                            \
				"mvn", "-B",                                                                \
				"org.apache.maven.plugins:maven-dependency-plugin:3.0.0:copy-dependencies", \
				"-DoutputDirectory=.");                                                     \
		if (rv != 0) {                                                                      \
			err.println(output);                                                            \
			err.println("Failed to download dependencies");                                 \
			exit(rv); }}                                                                    \
	println("lib/JODL/dist/JODL.jar");                                                      \
	println("lib/odt2daisy/dist/odt2daisy.jar");                                            \
	glob("lib/odt2daisy/lib/*.jar").forEach(System.out::println);                           \
	glob("lib/*.jar").forEach(System.out::println);                                         )
ifeq ($(LIBS),)
$(error "Failed to set up classpath")
endif
TEST_CLASSPATH :=                              \
	lib/xprocspec-1.4.2.jar                    \
	lib/xprocspec-runner-1.2.6.jar             \
	lib/xproc-engine-api-1.3.0.jar             \
	lib/xproc-engine-daisy-pipeline-1.14.4.jar
DIST_CLASSPATH := $(filter-out $(TEST_CLASSPATH),$(LIBS))
CLASSPATH := $(DIST_CLASSPATH)
CLASSPATH += $(TEST_CLASSPATH)
CLASSPATH += .
export CLASSPATH

export IMPORTS = \
	com.versusoft.packages.ooo.odt2daisy.*          \
	org.daisy.maven.xproc.xprocspec.XProcSpecRunner \
	org.daisy.pipeline.script.*                     \
	org.daisy.pipeline.job.*

endif

.PHONY : dist dist-win dist-win64 dist-mac
dist dist-win : dist-win64
dist-win64 dist-mac : dist-% : dist/%.zip
dist/win64.zip dist/mac.zip : %.zip : %
	List<String> cmd = new ArrayList<>();      \
	cmd.add("zip");                            \
	cmd.add("-r");                             \
	cmd.add(new File("$@").getAbsolutePath()); \
	for (String f : new File("$<").list())     \
		cmd.add(f);                            \
	exec(new File("$<"), cmd);

dist/win64 dist/mac : dist/% : jre-% main.jar $(DIST_CLASSPATH)
	rm("$@");                            \
	mkdirs("$@");                        \
	mv("$<", "$@/jre");                  \
	cp("$(word 2,$^)", "$@/");           \
	mkdirs("$@/lib");                    \
	int i = 0;                           \
	for (String f : "$^".split("\\s+"))  \
		if (i++ >= 2)                    \
			cp(f, "$@/lib/");

main.jar : classpath.txt \
           main.class \
           scripts$$odtToDTBook.class scripts$$textToEbraille.class \
           odt2daisy.class odt2daisy$$1.class odt2daisy$$Step.class odt2daisy$$Step$$Provider.class \
           META-INF/services/org.daisy.common.xproc.calabash.XProcStepProvider \
           META-INF/services/org.daisy.pipeline.script.XProcScriptService \
           odt-to-dtbook.xpl text-to-ebraille.xpl odt2daisy.xpl\
           braille.css
	exec("jar cvfem $@ main $^".split("\\s+"));

.INTERMEDIATE : classpath.txt
classpath.txt : $(DIST_CLASSPATH)
	mkdirs("$(dir $@)");                             \
	File f = new File("$@");                         \
	f.delete();                                      \
	StringBuilder s = new StringBuilder();           \
	s.append("Class-Path: \n");                      \
	for (String jar : "$^".split("\\s+"))            \
		s.append(" lib/" + new File(jar).getName())  \
		 .append(" \n");                             \
	write(f, s.toString());

scripts$$odtToDTBook.class scripts$$textToEbraille.class : scripts.class
	new File("$@").setLastModified(System.currentTimeMillis());
odt2daisy$$1.class odt2daisy$$Step.class odt2daisy$$Step$$Provider.class : odt2daisy.class
	new File("$@").setLastModified(System.currentTimeMillis());

main.class odt2daisy.class scripts.class : %.class : %.java $(DIST_CLASSPATH)
	javac("-cp", String.join(File.pathSeparator, "$(DIST_CLASSPATH)".split("\\s+")), "$<");

.INTERMEDIATE : jre-mac jre-win64
jre-win64 : OpenJDK11U-jdk_x64_windows_hotspot_11.0.13_8/jdk-11.0.13+8
jre-win64 jre-mac : OpenJDK11U-jdk_x64_mac_hotspot_11.0.13_8/jdk-11.0.13+8
	exec(env("JAVA_HOME", "$(CURDIR)/$</Contents/Home"),                       \
	     "mvn", "-B", "-f", "build-jre.xml", "jlink:jlink", "-Pbuild-$@");
	mkdirs("$(dir $@)");                                                       \
	rm("$@");                                                                  \
	mv("target/maven-jlink/classifiers/$(patsubst jre-%,%,$@)", "$@");         \
	rm("target");

OpenJDK11U-jdk_x64_mac_hotspot_11.0.13_8/jdk-11.0.13+8 : %/jdk-11.0.13+8 : | %.tar.gz
	mkdirs("$(dir $@)"); \
	exec("tar", "-zxvf", "OpenJDK11U-jdk_x64_mac_hotspot_11.0.13_8.tar.gz", "-C", "$(dir $@)/");

OpenJDK11U-jdk_x64_windows_hotspot_11.0.13_8/jdk-11.0.13+8 : %/jdk-11.0.13+8 : %.zip
	mkdirs("$(dir $@)"); \
	unzip(new File("$<"), new File("$(dir $@)"));

.INTERMEDIATE : OpenJDK11U-jdk_x64_mac_hotspot_11.0.13_8.tar.gz
OpenJDK11U-jdk_x64_mac_hotspot_11.0.13_8.tar.gz :
	mkdirs("$(dir $@)");                                                                                           \
	copy(new URL("https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.13%2B8/$(notdir $@)"), \
	     new File("$@"));

.INTERMEDIATE : OpenJDK11U-jdk_x64_windows_hotspot_11.0.13_8.zip
OpenJDK11U-jdk_x64_windows_hotspot_11.0.13_8.zip :
	mkdirs("$(dir $@)");                                                                                           \
	copy(new URL("https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.13%2B8/$(notdir $@)"), \
	     new File("$@"));

ODT := $(shell glob("odt/*.odt").forEach(System.out::println);)
DTB := $(patsubst odt/%.odt,dtb/%,$(ODT))
EBRAILLE := $(patsubst dtb/%,ebraille/%,$(DTB))

.PHONY : check
check : dtb/000100_headings
$(DTB) : dtb/% : odt/%.odt
	@rm("$@");                                                    \
	mkdirs("$@");                                                 \
	File dtb = new File("$@/$(notdir $@).xml").getAbsoluteFile(); \
	try {                                                         \
		Odt2Daisy odt2daisy = new Odt2Daisy("$<");                \
		odt2daisy.init();                                         \
		odt2daisy.setUidParam("no-uid");                          \
		odt2daisy.setWriteCSSParam(false);                        \
		odt2daisy.paginationProcessing();                         \
		odt2daisy.correctionProcessing();                         \
		odt2daisy.convertAsDTBook(dtb.getPath(), "images");       \
	} catch (Throwable e) {                                       \
		rm("$@");                                                 \
		throw e;                                                  \
	}                                                             \
	if (!new Odt2Daisy(null).validateDTD(dtb.getPath())) {        \
		err.println("produced invalid DTBook: " + dtb);           \
		exit(1);                                                  \
	}

check : ebraille/000100_headings
$(EBRAILLE) : scripts$$textToEbraille.class odt2daisy$$Step$$Provider.class
$(EBRAILLE) : ebraille/% : dtb/%
	@ScriptRegistry scriptRegistry = ServiceLoader.load(ScriptRegistry.class).iterator().next();   \
	JobFactory jobFactory = ServiceLoader.load(JobFactory.class).iterator().next();                \
	Job job = jobFactory.newJob(                                                                   \
		new BoundScript.Builder(scriptRegistry.getScript("text-to-ebraille").load())               \
		               .withInput("source", new File("$<", "$(notdir $<).xml").getAbsoluteFile())  \
		               .withInput("stylesheet", new File("braille.css").getAbsoluteFile())         \
		               .build()).build().get();                                                    \
	job.run();                                                                                     \
	if (job.getStatus() != Job.Status.SUCCESS) {                                                   \
		throw new RuntimeException("Job finished with status " + job.getStatus() + "\n"            \
			+ "Job files have not been deleted: " + new File(job.getLogFile()).getParentFile()); } \
	rm("$@");                                                                                      \
	for (JobResult r : job.getResults().getResults("result")) {                                    \
		File f = new File(new File("$@"), r.strip().getIdx());                                     \
		mkdirs(f.getParentFile());                                                                 \
		cp(r.asStream(), new FileOutputStream(f));                                                 \
	}                                                                                              \
	job.close();

check : xprocspec
.PHONY : xprocspec
xprocspec : odt2daisy$$Step$$Provider.class
	@File tempDir = Files.createTempDirectory("xprocspec-").toFile();      \
	rm("xprocspec-reports");                                               \
	if (ServiceLoader.load(XProcSpecRunner.class).iterator().next()        \
	                 .run(new File("."),                                   \
	                      new File("xprocspec-reports"),                   \
	                      tempDir,                                         \
	                      tempDir,                                         \
	                      new XProcSpecRunner.Reporter.DefaultReporter())) \
		rm("xprocspec-reports");                                           \
	rm(tempDir);

.PHONY : dist-check
dist-check : dist/mac
	rm("dist-check");                                                                                             \
	exec("$</jre/bin/java", "-jar", "$</main.jar", "dtbook",                                                      \
	                                               "odt/000600_simple_image.odt",                                 \
	                                               "dist-check/dtb/000600_simple_image");
	exec("$</jre/bin/java", "-jar", "$</main.jar", "ebraille",                                                    \
	                                               "dist-check/dtb/000600_simple_image/000600_simple_image.xml",  \
	                                               "dist-check/ebraille/000600_simple_image");
	exec("$</jre/bin/java", "-jar", "$</main.jar", "ebraille",                                                    \
	                                               "odt/000600_simple_image.odt",                                 \
	                                               "dist-check/ebraille/000600_simple_image_from_odt");
