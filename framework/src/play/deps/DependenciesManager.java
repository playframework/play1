package play.deps;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.FilterHelper;

import play.libs.Files;
import play.libs.IO;

public class DependenciesManager {

    public static void main(String[] args) throws Exception {
        String applicationPath = System.getProperty("application.path");
        if (applicationPath == null) {
            System.out.println("~ ERROR: cannot resolve \"application.path\"");
            return;
        }
        String frameworkPath = System.getProperty("framework.path");
        if (frameworkPath == null) {
            System.out.println("~ ERROR: cannot resolve \"framework.path\"");
            return;
        }
        String userHomePath = System.getProperty("user.home");
        if (userHomePath == null) {
            System.out.println("~ ERROR: cannot resolve \"user.home\"");
            return;
        }
        String playVersion = System.getProperty("play.version");
        if (playVersion == null) {
            System.out.println("~ ERROR: cannot resolve \"play.version\"");
            return;
        }

        // Paths
        File application = new File(applicationPath);
        File framework = new File(frameworkPath);
        File userHome = new File(userHomePath);

        DependenciesManager deps = new DependenciesManager(application, framework, userHome);

        ResolveReport report = deps.resolve();
        if (report != null) {
            deps.report();
            List<File> installed = deps.retrieve(report);
            deps.sync(installed);
        }

        if (deps.problems()) {
            System.out.println("~");
            System.out.println("~ Some dependencies are still missing.");
            System.out.println("~");
        } else {
            System.out.println("~");
            System.out.println("~ Done!");
            System.out.println("~");
        }
    }

    File application;
    File framework;
    File userHome;
    HumanReadyLogger logger;

    final FileFilter dirsToTrim = new FileFilter() {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() && isDirToTrim(file.getName());
        }

        private boolean isDirToTrim(String fileName) {
            return "documentation".equals(fileName) || "src".equals(fileName) || "tmp".equals(fileName) || fileName.contains("sample")
                    || fileName.contains("test");
        }
    };

    public DependenciesManager(File application, File framework, File userHome) {
        this.application = application;
        this.framework = framework;
        this.userHome = userHome;
    }

    public void report() {
        if (logger != null) {
            if (!logger.dynamics.isEmpty()) {
                System.out.println("~");
                System.out.println("~ Some dynamic revisions have been resolved as following,");
                System.out.println("~");
                for (String d : logger.dynamics) {
                    System.out.println("~\t" + d);
                }
            }

            if (!logger.evicteds.isEmpty()) {
                System.out.println("~");
                System.out.println("~ Some dependencies have been evicted,");
                System.out.println("~");
                for (String d : logger.evicteds) {
                    System.out.println("~\t" + d);
                }
            }
        }
    }

    public void sync(List<File> installed) {

        List<File> notSync = new ArrayList<>();

        File[] paths = new File[] { new File(application, "lib"), new File(application, "modules") };
        for (File path : paths) {
            if (path.exists()) {
                for (File f : path.listFiles()) {
                    if (!installed.contains(f)) {
                        notSync.add(f);
                    }
                }
            }
        }

        boolean autoSync = System.getProperty("nosync") == null;

        if (autoSync && !notSync.isEmpty()) {
            System.out.println("~");
            System.out.println("~ Synchronizing, deleting unknown dependencies");
            System.out.println("~");
            for (File f : notSync) {
                Files.delete(f);
                System.out.println("~ \tDeleted: " + f.getAbsolutePath());
            }
            System.out.println("~");
        } else if (!notSync.isEmpty()) {
            System.out.println("~");
            System.out.println("~ *****************************************************************************");
            System.out.println(
                    "~ WARNING: Your lib/ and modules/ directories are not synced with current dependencies (don't use --nosync to automatically delete them)");
            System.out.println("~");
            for (File f : notSync) {
                System.out.println("~ \tUnknown: " + f.getAbsolutePath());
            }
            System.out.println("~ *****************************************************************************");
        }
    }

    public boolean problems() {
        if (logger != null) {
            if (!logger.notFound.isEmpty()) {
                System.out.println("~");
                System.out.println("~ *****************************************************************************");
                System.out.println(
                        "~ WARNING: These dependencies are missing, your application may not work properly (use --verbose for details),");
                System.out.println("~");
                for (String d : logger.notFound) {
                    System.out.println("~\t" + d);
                }
                System.out.println("~ *****************************************************************************");
                return true;
            }
        }
        return false;
    }

    // Retrieve the list of modules in the order they were defined in the dependencies.yml.
    public Set<String> retrieveModules() throws Exception {
        File ivyModule = new File(application, "conf/dependencies.yml");
        if (ivyModule == null || !ivyModule.exists()) {
            return new LinkedHashSet<>();
        }
        return YamlParser.getOrderedModuleList(ivyModule);
    }

    public List<File> retrieve(ResolveReport report) throws Exception {
        // Track missing artifacts
        List<ArtifactDownloadReport> missing = new ArrayList<>();

        // Track deps with errors
        List<IvyNode> problems = new ArrayList<>();

        List<ArtifactDownloadReport> artifacts = new ArrayList<>();
        for (IvyNode node : ((List<IvyNode>) report.getDependencies())) {
            if (node.isLoaded() && !node.isCompletelyEvicted()) {
                ArtifactDownloadReport[] adr = report.getArtifactsReports(node.getResolvedId());
                for (ArtifactDownloadReport artifact : adr) {
                    if (artifact.getLocalFile() == null) {
                        missing.add(artifact);
                    } else {
                        if (isPlayModule(artifact) || !isFrameworkLocal(artifact)) {
                            artifacts.add(artifact);

                            // Save the order of module
                            if (isPlayModule(artifact)) {
                                String mName = artifact.getLocalFile().getName();
                                if (mName.endsWith(".jar") || mName.endsWith(".zip")) {
                                    mName = mName.substring(0, mName.length() - 4);
                                }
                            }
                        }
                    }
                }
            } else if (node.hasProblem() && !node.isCompletelyEvicted()) {
                problems.add(node);
            }
        }

        // Create directory if not exist
        File modulesDir = new File(application, "modules");
        if (!modulesDir.exists()) {
            modulesDir.mkdir();
        }

        if (!missing.isEmpty() || !problems.isEmpty()) {
            System.out.println("~");
            System.out.println("~ WARNING: Some dependencies could not be downloaded (use --verbose for details),");
            System.out.println("~");
            for (ArtifactDownloadReport d : missing) {
                StringBuilder msg = new StringBuilder(d.getArtifact().getModuleRevisionId().getOrganisation()).append(" -> ")
                        .append(d.getArtifact().getModuleRevisionId().getName()).append(' ')
                        .append(d.getArtifact().getModuleRevisionId().getRevision()).append(": ").append(d.getDownloadDetails());
                System.out.println("~\t" + msg.toString());
                if (logger != null) {
                    logger.notFound.add(msg.toString());
                }
            }
            if (!problems.isEmpty()) {
                for (IvyNode node : problems) {
                    ModuleRevisionId moduleRevisionId = node.getId();

                    StringBuilder msg = new StringBuilder(moduleRevisionId.getOrganisation()).append("->")
                            .append(moduleRevisionId.getName()).append(' ').append(moduleRevisionId.getRevision()).append(": ")
                            .append(node.getProblemMessage());
                    System.out.println("~\t" + msg.toString());
                    if (logger != null) {
                        logger.notFound.add(msg.toString());
                    }
                }
            }
        }

        List<File> installed = new ArrayList<>();

        // Install
        if (artifacts.isEmpty()) {
            System.out.println("~");
            System.out.println("~ No dependencies to install");
        } else {
            System.out.println("~");
            System.out.println("~ Installing resolved dependencies,");
            System.out.println("~");

            for (ArtifactDownloadReport artifact : artifacts) {
                installed.add(install(artifact));
            }
        }

        return installed;
    }

    public File install(ArtifactDownloadReport artifact) throws Exception {
        boolean force = "true".equalsIgnoreCase(System.getProperty("play.forcedeps"));
        boolean trim = "true".equalsIgnoreCase(System.getProperty("play.trimdeps"));
        boolean shortModuleNames = "true".equalsIgnoreCase(System.getProperty("play.shortModuleNames"));
        
        try {
            File from = artifact.getLocalFile();

            if (!isPlayModule(artifact)) {
                if ("source".equals(artifact.getArtifact().getType())) {
                    // A source artifact: leave it in the cache, and write its path in tmp/lib-src/<jar-name>.src
                    // so that it can be used later by commands generating IDE project fileS.
                    new File(application, "tmp/lib-src").mkdirs();
                    IO.writeContent(from.getAbsolutePath(),
                            new File(application, "tmp/lib-src/" + from.getName().replace("-sources", "") + ".src"));
                    return null;

                } else {
                    // A regular library: copy it to the lib/ directory
                    File to = new File(application, "lib" + File.separator + from.getName()).getCanonicalFile();
                    new File(application, "lib").mkdir();
                    Files.copy(from, to);
                    System.out.println("~ \tlib/" + to.getName());
                    return to;
                }

            } else {
                // A module
                String mName = moduleName(artifact, shortModuleNames);
                File to = new File(application, "modules" + File.separator + mName).getCanonicalFile();
                new File(application, "modules").mkdir();
                Files.delete(to);
                if (from.isDirectory()) {
                    if (force) {
                        IO.copyDirectory(from, to);
                    } else {
                        IO.writeContent(from.getAbsolutePath(), to);
                    }
                    System.out.println("~ \tmodules/" + to.getName() + " -> " + from.getAbsolutePath());
                } else {
                    Files.unzip(from, to);
                    System.out.println("~ \tmodules/" + to.getName());
                }

                if (trim) {
                    for (File dirToTrim : to.listFiles(dirsToTrim)) {
                        Files.deleteDirectory(dirToTrim);
                    }
                }

                return to;
            }
        } catch (Exception e) {
            System.out.println("~ \tError installing " + artifact.getLocalFile());
            throw e;
        }
    }

    String moduleName(ArtifactDownloadReport artifact, boolean shortModuleNames) {
        if (shortModuleNames) {
            return artifact.getName();
        }

        String mName = artifact.getLocalFile().getName();
        if (mName.endsWith(".jar") || mName.endsWith(".zip")) {
            mName = mName.substring(0, mName.length() - 4);
        }
        return mName;
    }

    private boolean isFrameworkLocal(ArtifactDownloadReport artifact) throws Exception {
        String artifactFileName = artifact.getLocalFile().getName();
        return new File(framework, "framework/lib/" + artifactFileName).exists()
                || new File(framework, "framework/" + artifactFileName).exists();
    }

    private boolean isPlayModule(ArtifactDownloadReport artifact) throws Exception {
        boolean isPlayModule = artifact.getLocalFile().getName().endsWith(".zip");
        if (!isPlayModule) {
            // Check again from origin location
            if (!artifact.getArtifactOrigin().isLocal() && artifact.getArtifactOrigin().getLocation().endsWith(".zip")) {
                isPlayModule = true;
            } else if (artifact.getArtifactOrigin().isLocal() && artifact.getLocalFile().isDirectory()) {
                isPlayModule = true;
            } else if (artifact.getArtifactOrigin().isLocal()) {
                String frameworkPath = new File(framework, "modules").getCanonicalPath();
                isPlayModule = artifact.getArtifactOrigin().getLocation().startsWith(frameworkPath);
            }
        }
        return isPlayModule;
    }

    public ResolveReport resolve() throws Exception {
        // Module
        ModuleDescriptorParserRegistry.getInstance().addParser(new YamlParser());
        File ivyModule = new File(application, "conf/dependencies.yml");
        if (!ivyModule.exists()) {
            System.out.println("~ !! " + ivyModule.getAbsolutePath() + " does not exist");
            System.exit(-1);
            return null;
        }

        // Variables
        System.setProperty("play.path", framework.getAbsolutePath());

        // Ivy
        Ivy ivy = configure();

        // Clear the cache
        boolean clearcache = System.getProperty("clearcache") != null;
        if (clearcache) {
            System.out.println("~ Clearing cache : " + ivy.getResolutionCacheManager().getResolutionCacheRoot() + ",");
            System.out.println("~");
            try {
                FileUtils.deleteDirectory(ivy.getResolutionCacheManager().getResolutionCacheRoot());
                System.out.println("~       Clear");
            } catch (IOException e) {
                System.out.println("~       Could not clear");
                System.out.println("~ ");
                e.printStackTrace();
            }

            System.out.println("~");
        }

        System.out.println("~ Resolving dependencies using " + ivyModule.getAbsolutePath() + ",");
        System.out.println("~");

        // Resolve
        ResolveEngine resolveEngine = ivy.getResolveEngine();
        ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setConfs(new String[] { "default" });
        resolveOptions.setArtifactFilter(FilterHelper.getArtifactTypeFilter(new String[] { "jar", "bundle", "source" }));

        return resolveEngine.resolve(ivyModule.toURI().toURL(), resolveOptions);
    }

    public Ivy configure() throws Exception {
        boolean verbose = System.getProperty("verbose") != null;
        boolean debug = System.getProperty("debug") != null;
        this.logger = new HumanReadyLogger();

        IvySettings ivySettings = new IvySettings();
        new SettingsParser(this.logger).parse(ivySettings, new File(framework, "framework/dependencies.yml"));
        new SettingsParser(this.logger).parse(ivySettings, new File(application, "conf/dependencies.yml"));
        ivySettings.setDefaultResolver("mavenCentral");
        ivySettings.setDefaultUseOrigin(true);
        PlayConflictManager conflictManager = new PlayConflictManager();
        ivySettings.addConflictManager("playConflicts", conflictManager);
        ivySettings.addConflictManager("defaultConflicts", conflictManager.delegate);
        ivySettings.setDefaultConflictManager(conflictManager);

        Ivy ivy = Ivy.newInstance(ivySettings);

        // Default ivy config see: http://play.lighthouseapp.com/projects/57987-play-framework/tickets/807
        if (userHome != null) {
            File ivyDefaultSettings = new File(userHome, ".ivy2/ivysettings.xml");
            if (ivyDefaultSettings != null && ivyDefaultSettings.exists()) {
                ivy.configure(ivyDefaultSettings);
            }
        }

        if (debug) {
            ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_DEBUG));
        } else if (verbose) {
            ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_INFO));
        } else {
            ivy.getLoggerEngine().setDefaultLogger(this.logger);
        }

        ivy.pushContext();

        return ivy;
    }
}
