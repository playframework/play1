package controllers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import play.Play;
import play.libs.IO;
import play.mvc.Before;
import play.mvc.Controller;
import play.modules.tracer.service.TraceService;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

public class Traces extends Controller {
	@Before
	public static void check() {
		
	}
	
	public static void list() {
		Collection<String> list = TraceService.list();
		render(list);
	}
	
	public static void show(String id) {
		if(id == null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			notFound("trace with id '" + id + "'");
		String trace = TraceService.loadSavedTrace(id);
		render(id, trace);
	}
	
	public static void jsonTrace(String id) {
		if(id == null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			notFound("trace with id '" + id + "'");
		String trace = TraceService.loadSavedTrace(id);
		response.contentType = "text/json";
		renderTemplate("Traces/jsonTrace.json", trace);
	}
	
	public static void getClassSource(String name) {
		renderText(Play.classes.getApplicationClass(name).javaSource);
	}
	
	public static void getTemplateSource(String name) {
		VirtualFile vf = VirtualFile.fromRelativePath(name);
		renderText(TemplateLoader.load(vf).source);
	}
	
	public static void getFile(String path) throws IOException {
		File f = new File(path);
		String contents = IO.readContentAsString(f);
		renderText(contents);
	}
}
