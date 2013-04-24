package play.modules.docviewer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import play.Play;
import play.libs.Files;
import play.libs.IO;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marek Piechut
 */
public class ExportDocumentationGenerator extends DocumentationGenerator {

    @Override
    public List<Map<String, String>> listSections() {
        File[] textileFiles = projectDocsPath.listFiles((FilenameFilter) new SuffixFileFilter(".textile"));
        List<Map<String, String>> sections = new ArrayList<Map<String, String>>(textileFiles.length);

        for (File textileFile : textileFiles) {
            String textile = IO.readContentAsString(textileFile);
            String title = getTitle(textile);
            String id = FilenameUtils.getBaseName(textileFile.getPath());

            Map<String, String> section = new HashMap<String, String>(2);
            section.put("title", title);
            section.put("href", id + ".html");
            section.put("id", id);

            sections.add(section);
        }

        return sections;
    }

    /**
     * Generate HTMLs so you can share project documentation without source
     *
     * @param args
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        if (!Play.started) {
            Play.init(new File("."), "dev");
            Play.start();
        }

        DocumentationGenerator generator = new ExportDocumentationGenerator();
        File targetFolder = new File(Play.tmpDir, "documentation");
        Files.deleteDirectory(targetFolder);

        targetFolder.mkdirs();
        String index = generator.generateIndex();
        File outFile = new File(targetFolder, "index.html");
        outFile.createNewFile();
        IO.writeContent(index, outFile);

        List<Map<String, String>> sections = generator.listSections();
        for (Map<String, String> section : sections) {
            String href = section.get("href");
            outFile = new File(targetFolder, href);
            outFile.createNewFile();
            IO.writeContent(generator.generatePage(section.get("id")), outFile);
        }

        Play.stop();

        File images = new File(generator.projectDocsPath, "images");
        if (images.exists()) {
            IO.copyDirectory(images, new File(targetFolder, "images"));
        }

        File files = new File(generator.projectDocsPath, "files");
        if (files.exists()) {
            IO.copyDirectory(files, new File(targetFolder, "files"));
        }
        
        File zipFile = new File(generator.projectDocsPath,
                Play.configuration.getProperty("application.name") + "-docs.zip");
        Files.zip(targetFolder, zipFile);
        System.out.println("Project documentation exported to: " + zipFile.getAbsolutePath());
    }
}
