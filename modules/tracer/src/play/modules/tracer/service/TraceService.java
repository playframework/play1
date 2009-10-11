package play.modules.tracer.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Map;

import play.modules.tracer.Trace;

import play.Logger;
import play.Play;
import play.libs.IO;

public class TraceService {
	public final static File TRACER_DIRECTORY;
	
	static {
		TRACER_DIRECTORY = new File(Play.tmpDir, "tracer");
		if(!TRACER_DIRECTORY.exists())
			TRACER_DIRECTORY.mkdir();
	}
	
	public static String loadSavedTrace(String id) {
		File f = new File(TRACER_DIRECTORY, id + ".json");
		try {
			return IO.readContentAsString(f);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void save(Trace trace) {
		FileWriter fw = null;
		try {
			File ftrace = new File(TRACER_DIRECTORY, trace.id + ".json");
			ftrace.createNewFile();
			fw = new FileWriter(ftrace);
			fw.write(trace.toJSON());
			fw.flush();
			Logger.info("Tracer: trace written in " + ftrace.getCanonicalPath());
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if(fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 
	 * @return a list of strings denoting the saved traces' id, the last saved first
	 */
	public static Collection<String> list() {
		Map<Long, String> result = new TreeMap<Long, String>();
		for(File f : TRACER_DIRECTORY.listFiles()) {
			result.put(f.lastModified(), f.getName().replace(".json", ""));
		}
		return result.values();
	}
	
	public static void delete(String id) {
		File f = new File(TRACER_DIRECTORY, id);
		if(f.exists())
			f.delete();
	}
	
	public static void clean() {
		for(File f : TRACER_DIRECTORY.listFiles()) {
			f.delete();
		}
	}
}
