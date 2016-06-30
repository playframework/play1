package controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import helpers.CheatSheetHelper;
import helpers.LangMenuHelper;
import helpers.LangMenuHelper.LangMenu;
import play.Play;
import play.db.jpa.NoTransaction;
import play.libs.IO;
import play.modules.docviewer.DocumentationGenerator;
import play.mvc.Controller;
import play.mvc.Http;
import play.vfs.VirtualFile;

@NoTransaction
public class PlayDocumentation extends Controller {

  public static DocumentationGenerator generator = new DocumentationGenerator();

  public static void index() throws Exception {
    Http.Header header = request.headers.get("accept-language");
    String docLang = header != null ? header.value().split(",")[0] : "";
    docLang = docLang.length() > 2 ? docLang.substring(0, 2) : docLang;
    page("home", null, docLang);
  }

  @SuppressWarnings("unchecked")
  public static void page(String id, String module, String docLang) throws Exception {
    String docLangDir = (docLang != null && (!"en".equalsIgnoreCase(docLang) && !docLang.matches("en-.*")))
        ? "_" + docLang + "/" : "/";

    File page = new File(Play.frameworkPath, "documentation/manual" + docLangDir + id + ".textile");
    if (!page.exists()) {
      page = new File(Play.frameworkPath, "documentation/manual/" + id + ".textile");
    }

    if (module != null) {
      page = new File(Play.modules.get(module).getRealFile(), "documentation/manual/" + id + ".textile");
    }

    if (!page.exists()) {
      notFound("Manual page for " + id + " not found");
    }
    String textile = IO.readContentAsString(page);
    String html = generator.toHTML(textile);
    html = generator.stripBody(html);
    String title = generator.getTitle(textile);

    List<String> modules = new ArrayList();
    List<String> apis = new ArrayList();
    if (id.equals("home") && module == null) {
      for (String key : Play.modules.keySet()) {
        VirtualFile mr = Play.modules.get(key);
        VirtualFile home = mr.child("documentation/manual/" + "home.textile");
        if (home.exists()) {
          modules.add(key);
        }
        if (mr.child("documentation/api/index.html").exists()) {
          apis.add(key);
        }
      }
    }
    List<LangMenu> langMenuList = LangMenuHelper.getMenuList();
    render(id, html, title, modules, apis, module, docLang, langMenuList);
  }

  public static void cheatSheet(String category, String docLang) {
    File[] sheetFiles = CheatSheetHelper.getSheets(category, docLang);
    if (sheetFiles != null) {
      List<String> sheets = new ArrayList<String>();

      for (File file : sheetFiles) {
        String html = generator.toHTML(IO.readContentAsString(file));
        html = generator.stripBody(html);
        sheets.add(html);
      }

      String title = CheatSheetHelper.getCategoryTitle(category);
      Map<String, String> otherCategories = CheatSheetHelper.listCategoriesAndTitles(docLang);

      render(title, otherCategories, sheets, docLang);
    }
    notFound("Cheat sheet directory not found");
  }

  public static void image(String name, String module, String lang) {
    File image = new File(Play.frameworkPath, "documentation/images/" + name + ".png");
    if (module != null) {
      image = new File(Play.modules.get(module).getRealFile(), "documentation/images/" + name + ".png");
    }
    if (!image.exists()) {
      notFound();
    }
    renderBinary(image);
  }

  public static void file(String name, String module, String lang) {
    File file = new File(Play.frameworkPath, "documentation/files/" + name);
    if (module != null) {
      file = new File(Play.modules.get(module).getRealFile(), "documentation/files/" + name);
    }
    if (!file.exists()) {
      notFound();
    }
    renderBinary(file);
  }

  public static void releases(String id, String version, String docLang) throws Exception {
    String docLangDir = (docLang != null && (!"en".equalsIgnoreCase(docLang) && !docLang.matches("en-.*")))
        ? "_" + docLang + "/" : "/";

    File page = new File(Play.frameworkPath,
        "documentation/manual" + docLangDir + "releases/" + (version != null ? version + "/" : "") + id + ".textile");
    if (!page.exists()) {
      page = new File(Play.frameworkPath, "documentation/manual/" + "releases/" + id + ".textile");
    }

    if (!page.exists()) {
      notFound("Manual page for " + id + " not found");
    }
    String textile = IO.readContentAsString(page);
    String html = generator.toHTML(textile);
    html = generator.stripBody(html);
    String title = generator.getTitle(textile);

    List<String> modules = new ArrayList();
    List<String> apis = new ArrayList();
    if (id.equals("home")) {
      for (String key : Play.modules.keySet()) {
        VirtualFile mr = Play.modules.get(key);
        VirtualFile home = mr.child("documentation/manual/" + "home.textile");
        if (home.exists()) {
          modules.add(key);
        }
        if (mr.child("documentation/api/index.html").exists()) {
          apis.add(key);
        }
      }
    }
    List<LangMenu> langMenuList = LangMenuHelper.getMenuList();
    render(id, html, title, modules, apis, docLang, langMenuList);
  }
}
