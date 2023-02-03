include java-shell-for-make/enable-java-shell.mk

.PHONY : all
all : check dist

.PHONY : clean
clean :
	@rm("java-shell-for-make/recipes");                 \
	rm("dist");                                         \
	rm("dist-check");                                   \
	rm("main.class");                                   \
	rm("main.jar");                                     \
	rm("jre-win64");                                    \
	rm("jre-mac");                                      \
	rm("classpath.txt");                                \
	rm("target");                                       \
	rm("OpenJDK11U-jdk_x64_mac_hotspot_11.0.13_8");     \
	rm("OpenJDK11U-jdk_x64_windows_hotspot_11.0.13_8"); \
	rm("dtb");                                          \
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
	println("lib/JODL/dist/JODL.jar");                                                      \
	println("lib/odt2daisy/dist/odt2daisy.jar");                                            \
	glob("lib/odt2daisy/lib/*.jar").forEach(System.out::println);                           )
ifeq ($(LIBS),)
$(error "Failed to set up classpath")
endif
DIST_CLASSPATH := $(LIBS)
CLASSPATH := $(DIST_CLASSPATH)
export CLASSPATH

export IMPORTS = com.versusoft.packages.ooo.odt2daisy.*

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

main.jar : classpath.txt main.class
	exec("jar cvfem $@ main $^".split("\\s+"));

.INTERMEDIATE : classpath.txt
classpath.txt : $(DIST_CLASSPATH)
	mkdirs("$(dir $@)");                             \
	File f = new File("$@");                         \
	f.delete();                                      \
	write(f, "Class-Path:");                         \
	for (String jar : "$^".split("\\s+"))            \
		write(f, " lib/" + new File(jar).getName()); \
	write(f, "\n");

main.class : %.class : %.java $(DIST_CLASSPATH)
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
DTB := $(patsubst odt/%.odt,dtb/%.xml,$(ODT))

.PHONY : check
check : dtb/000100_headings.xml

$(DTB) : dtb/%.xml : odt/%.odt
	@mkdirs("$(dir $@)");                                       \
	try {                                                       \
		Odt2Daisy odt2daisy = new Odt2Daisy("$<");              \
		odt2daisy.init();                                       \
		odt2daisy.setUidParam("no-uid");                        \
		odt2daisy.setWriteCSSParam(false);                      \
		odt2daisy.paginationProcessing();                       \
		odt2daisy.correctionProcessing();                       \
		odt2daisy.convertAsDTBook("$@", "$(notdir $@).images"); \
	} catch (Throwable e) {                                     \
		rm("$@");                                               \
		throw e;                                                \
	}                                                           \
	if (!new Odt2Daisy(null).validateDTD("$@")) {               \
		err.println("produced invalid DTBook: $@");             \
		exit(1);                                                \
	}

.PHONY : dist-check
dist-check : dist/mac
	rm("dist-check");                                                                                                        \
	exec("$</jre/bin/java", "-jar", "$</main.jar", "odt/000600_simple_image.odt", "dist-check/dtb/000600_simple_image.xml");
