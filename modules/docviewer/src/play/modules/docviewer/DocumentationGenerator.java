package play.modules.docviewer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import play.Play;
import play.libs.IO;
import play.templates.Template;
import play.templates.TemplateLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marek Piechut
 */
public class DocumentationGenerator {

    public File projectDocsPath = new File(Play.applicationPath, "documentation");

    public String generateIndex() {
        List<Map<String, String>> sections = listSections();

        String appName = StringUtils.capitalize(Play.configuration.getProperty("application.name"));
        StringBuilder htmlBuilder = new StringBuilder("<html><body><h1>")
                .append(appName).append(" documentation:</h1><ol>\n");

        for (Map<String, String> section : sections) {
            htmlBuilder.append("<li><a href='").append(section.get("href"));
            htmlBuilder.append("'>").append(section.get("title")).append("</a></oi>\n");
        }

        htmlBuilder.append("</ul></body></html>\n");

        String html = applyTemplate("index", appName + " documentation",
                sections, htmlBuilder.toString());
        return html;
    }

    public String generatePage(String id) {
        File page = new File(projectDocsPath, id + ".textile");

        if (!page.exists()) {
            return null;
        }

        String textile = IO.readContentAsString(page);
        String title = getTitle(textile);

        String html = toHTML(textile);
        List<Map<String, String>> sections = listSections();
        html = applyTemplate(id, title, sections, html);
        return html;
    }

    public String applyTemplate(String id, String title, List<Map<String, String>> sections, String html) {
        //Template to use when rendering project documentation
        File templateFile = new File(projectDocsPath, "template.html");
        if (templateFile.exists()) {
            //Render documentation using user template

            //Strip body so html fits some external content
            html = stripBody(html);

            Map<String, Object> params = new HashMap<String, Object>(3);
            params.put("id", id);
            params.put("title", title);
            params.put("sections", sections);
            params.put("html", html);
            Template template = TemplateLoader.load("docviewer-" + templateFile.getName(), IO.readContentAsString(templateFile, "utf-8"));
            String pageContents = template.render(params);
            return pageContents;
        } else {
            //Render plain output
            return html;
        }
    }

    public List<Map<String, String>> listSections() {
        File[] textileFiles = projectDocsPath.listFiles((FilenameFilter) new SuffixFileFilter(".textile"));
        List<Map<String, String>> sections = new ArrayList<Map<String, String>>(textileFiles.length);

        for (File textileFile : textileFiles) {
            String textile = IO.readContentAsString(textileFile);
            String title = getTitle(textile);
            String id = FilenameUtils.getBaseName(textileFile.getPath());

            Map<String, String> section = new HashMap<String, String>(2);
            section.put("title", title);
            section.put("href", id);
            section.put("id", id);

            sections.add(section);
        }

        return sections;
    }

    public String toHTML(String textile) {
        return new jj.play.org.eclipse.mylyn.wikitext.core.parser.MarkupParser(
                new jj.play.org.eclipse.mylyn.wikitext.textile.core.TextileLanguage()).parseToHtml(textile);
    }

    public String getTitle(String textile) {
        if (textile.length() == 0) {
            return "";
        }
        return textile.split("\n")[0].substring(3).trim();
    }

    public String stripBody(String html) {
        html = html.substring(html.indexOf("<body>") + 6, html.lastIndexOf("</body>"));
        return html;
    }
}
