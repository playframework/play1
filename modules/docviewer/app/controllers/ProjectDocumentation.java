package controllers;

import java.io.File;

import play.db.jpa.NoTransaction;
import play.modules.docviewer.DocumentationGenerator;
import play.mvc.Controller;

/**
 * Controller to render project documentation. Allows to embed textile docs
 * inside project and access it via browser in development mode.
 *
 * @author Marek Piechut
 */
@NoTransaction
public class ProjectDocumentation extends Controller {

    public static DocumentationGenerator generator = new DocumentationGenerator();

    public static void index() throws Exception {
        String indexHtml = generator.generateIndex();

        // We need trailing slash or links won't work
        if (!request.url.endsWith("/")) {
            redirect(request.url + "/");
        }
        renderHtml(indexHtml);
    }

    public static void page(String id) {
        String html = generator.generatePage(id);
        if (html == null) {
            notFound("Documentation page for " + id + " not found");
        }
        renderHtml(html);
    }

    public static void file(String name) {
        File file = new File(generator.projectDocsPath, "files/" + name);
        if (!file.exists()) {
            notFound();
        }
        renderBinary(file);
    }

    public static void image(String name) {
        File image = new File(generator.projectDocsPath, "images/" + name);

        if (!image.exists()) {
            notFound();
        }
        renderBinary(image);
    }
}
