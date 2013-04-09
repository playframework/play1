package controllers;

import play.mvc.Controller;
import java.io.File;

import static play.modules.docviewer.DocumentationGenerator.*;

/**
 * Controller to render project documentation.
 * Allows to embed textile docs inside project and access it via browser
 * in development mode.
 *
 * @author Marek Piechut
 */
public class ProjectDocumentation extends Controller {

    public static void index() throws Exception {
        String indexHtml = generateIndex(false);
        String html = applyTemplate("index", "Project documentation", indexHtml);
        //We need trailing slash or links won't work
        if(!request.url.endsWith("/")) {
            redirect(request.url + "/");
        }
        renderHtml(html);
    }

    public static void page(String id) {
        String html = generatePage(id);
        if (html == null) {
            notFound("Documentation page for " + id + " not found");
        }
        renderHtml(html);
    }


    public static void file(String name) {
        File file = new File(projectDocsPath, "files/" + name);
        if (!file.exists()) {
            notFound();
        }
        renderBinary(file);
    }

    public static void image(String name) {
        File image = new File(projectDocsPath, "images/" + name);

        if (!image.exists()) {
            notFound();
        }
        renderBinary(image);
    }
}
