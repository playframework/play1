package controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import helpers.CheatSheetHelper;
import org.pegdown.PegDownProcessor;
import play.Play;
import play.libs.IO;
import play.mvc.Controller;
import play.vfs.VirtualFile;

public class PlayDocumentation extends Controller {

    public static void index() throws Exception {
        page("home", null);
    }

    public static void page(String id, String module) throws Exception {
        boolean isMarkdown = false;
        File page = new File(Play.frameworkPath, "documentation/manual/"+id+".textile");
        if(module != null) {
             page = new File(Play.modules.get(module).getRealFile(), "documentation/manual/"+id+".textile");
        }
        if(!page.exists() && module != null) {
            // try markdown before giving up, but only for modules
            page = new File(Play.modules.get(module).getRealFile(), "documentation/manual/"+id+".markdown");
            isMarkdown = page.exists();
        }
        if(!page.exists()) {
            notFound("Manual page for "+id+" not found");
        }

        String markup = IO.readContentAsString(page);
        String html = toHTML(markup, isMarkdown);
        String title = getTitle(markup);

        List<String> modules = new ArrayList();
        List<String> apis = new ArrayList();
        if(id.equals("home") && module == null) {
            for(String key : Play.modules.keySet()) {
                VirtualFile mr = Play.modules.get(key);
                VirtualFile home = mr.child("documentation/manual/home.textile");
                VirtualFile homeInMarkdown = mr.child("documentation/manual/home.markdown");
                if(home.exists()) {
                    modules.add(key);
                } else if (homeInMarkdown.exists()) {
                    modules.add(key);
                }
                if(mr.child("documentation/api/index.html").exists()) {
                    apis.add(key);
                }
            }
        }

        render(id, html, title, modules, apis, module);
    }

    public static void cheatSheet(String category) {
        File[] sheetFiles = CheatSheetHelper.getSheets(category);
        if(sheetFiles != null) {
            List<String> sheets = new ArrayList<String>();

            for (File file : sheetFiles) {
                sheets.add(toHTML(IO.readContentAsString(file), false));
            }

            String title = CheatSheetHelper.getCategoryTitle(category);
            Map<String, String> otherCategories = CheatSheetHelper.listCategoriesAndTitles();

            render(title, otherCategories, sheets);
        }
        notFound("Cheat sheet directory not found");
    }

    public static void image(String name, String module) {
        File image = new File(Play.frameworkPath, "documentation/images/"+name+".png");
        if(module != null) {
             image = new File(Play.modules.get(module).getRealFile(),"documentation/images/"+name+".png");
        }
        if(!image.exists()) {
            notFound();
        }
        renderBinary(image);
    }

    public static void file(String name, String module) {
        File file = new File(Play.frameworkPath, "documentation/files/"+name);
        if(module != null) {
             file = new File(Play.modules.get(module).getRealFile(),"documentation/files/"+name);
        }
        if(!file.exists()) {
            notFound();
        }
        renderBinary(file);
    }

    static String toHTML(String markup, boolean isMarkdown) {
        String html = null;
        if(isMarkdown) {
            html = new PegDownProcessor().markdownToHtml(markup);
        } else {
            html = new jj.play.org.eclipse.mylyn.wikitext.core.parser.MarkupParser(new jj.play.org.eclipse.mylyn.wikitext.textile.core.TextileLanguage()).parseToHtml(markup);
            html = html.substring(html.indexOf("<body>") + 6, html.lastIndexOf("</body>"));
        }
        return html;
    }

    static String getTitle(String textile) {
        if(textile.length() == 0) {
            return "";
        }
        return textile.split("\n")[0].substring(3).trim();
    }

}