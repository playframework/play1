package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;

import java.io.*;

public class PlayDocumentation extends Controller {
    
    public static void index() throws Exception {
        page("home");
    }
    
    public static void page(String id) throws Exception {
        File page = new File(Play.frameworkPath, "documentation/manual/"+id+".textile");
        if(!page.exists()) {
            notFound("Manual page for "+id+" not found");
        }
        String textile = IO.readContentAsString(page);
        String html = toHTML(textile);
        String title = getTitle(textile);
        render(id, html, title);
    }
    
    public static void image(String name) {
        File image = new File(Play.frameworkPath, "documentation/images/"+name+".png");
        if(!image.exists()) {
            notFound();
        }
        renderBinary(image);
    }
    
    public static void file(String name) {
        File file = new File(Play.frameworkPath, "documentation/files/"+name);
        if(!file.exists()) {
            notFound();
        }
        renderBinary(file);
    }    
    
    //
    
    static String toHTML(String textile) {
        String html = new org.eclipse.mylyn.wikitext.core.parser.MarkupParser(new org.eclipse.mylyn.wikitext.textile.core.TextileLanguage()).parseToHtml(textile);
        html = html.substring(html.indexOf("<body>") + 6, html.lastIndexOf("</body>"));
        return html;
    }
    
    static String getTitle(String textile) {
        if(textile.length() == 0) {
            return "";
        }
        return textile.split("\n")[0].substring(3).trim();
    }
    
}