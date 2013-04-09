package play.modules.docviewer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import play.Play;
import play.libs.Files;
import play.libs.IO;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Marek Piechut
 */
public class DocumentationGenerator {

    public static File projectDocsPath = new File(Play.applicationPath, "documentation");

    public static String generateIndex(boolean forExport) {
        Map<String, String> sections = listSections();

        StringBuilder htmlBuilder = new StringBuilder("<html><body><h1>Project documentation:</h1><ul>\n");

        for (Entry<String, String> section : sections.entrySet()) {
            htmlBuilder.append("<li><a href='").append(section.getKey());
            if (forExport) {
                htmlBuilder.append(".html");
            }
            htmlBuilder.append("'>").append(section.getValue()).append("</a></li>\n");
        }

        htmlBuilder.append("</ul></body></html>\n");
        return htmlBuilder.toString();
    }

    public static String generatePage(String id) {
        File page = new File(projectDocsPath, id + ".textile");

        if (!page.exists()) {
            return null;
        }

        String textile = IO.readContentAsString(page);
        String title = getTitle(textile);

        String html = toHTML(textile);

        html = applyTemplate(id, title, html);
        return html;
    }

    public static String applyTemplate(String id, String title, String html) {
        //Template to use when rendering project documentation
        File templateFile = new File(projectDocsPath, "index.html");
        if (templateFile.exists()) {
            //Render documentation using user template

            //Strip body so html fits some external content
            html = stripBody(html);

            Map<String, Object> params = new HashMap<String, Object>(3);
            params.put("id", id);
            params.put("html", html);
            params.put("title", title);
            Template template = TemplateLoader.load(VirtualFile.open(templateFile));
            String pageContents = template.render(params);
            return pageContents;
        } else {
            //Render plain output
            return html;
        }
    }

    public static Map<String, String> listSections() {
        File[] textileFiles = projectDocsPath.listFiles((FilenameFilter) new SuffixFileFilter(".textile"));
        Map<String, String> sections = new HashMap<String, String>(textileFiles.length);

        for (File textileFile : textileFiles) {
            String textile = IO.readContentAsString(textileFile);
            String title = getTitle(textile);
            String id = FilenameUtils.getBaseName(textileFile.getPath());

            sections.put(id, title);
        }

        return sections;
    }

    public static String toHTML(String textile) {
        return new jj.play.org.eclipse.mylyn.wikitext.core.parser.MarkupParser(
                new jj.play.org.eclipse.mylyn.wikitext.textile.core.TextileLanguage()).parseToHtml(textile);
    }

    public static String getTitle(String textile) {
        if (textile.length() == 0) {
            return "";
        }
        return textile.split("\n")[0].substring(3).trim();
    }

    public static String stripBody(String html) {
        html = html.substring(html.indexOf("<body>") + 6, html.lastIndexOf("</body>"));
        return html;
    }

    public static void main(String[] args) throws IOException {
        DocumentationGenerator generator = new DocumentationGenerator();
        //Generate HTMLs so you can share project documentation without source
        String target = args.length > 0 ? args[0] : "tmp";
        File targetFolder = new File(projectDocsPath, target);
        targetFolder.mkdirs();
        String index = generator.generateIndex(true);
        File outFile = new File(targetFolder, "index.html");
        outFile.createNewFile();
        IO.writeContent(index, outFile);

        Map<String, String> sections = generator.listSections();
        for (String id : sections.keySet()) {
            outFile = new File(targetFolder, id + ".html");
            outFile.createNewFile();
            IO.writeContent(generator.generatePage(id), outFile);
        }

        IO.copyDirectory(new File(projectDocsPath, "images"), new File(targetFolder, "images"));
        IO.copyDirectory(new File(projectDocsPath, "files"), new File(targetFolder, "files"));

        File zipFile = new File(projectDocsPath, "docs.zip");
        Files.zip(targetFolder, zipFile);

        System.out.println("Project documentation exported to: " + zipFile.getAbsolutePath());
    }
}
