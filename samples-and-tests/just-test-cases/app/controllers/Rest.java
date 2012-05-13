package controllers;

import java.io.File;
import java.util.*;

import play.Logger;
import play.mvc.Controller;

public class Rest extends Controller {
    

   	private static Map<String, String> RESPONSEMAP = new HashMap<String, String>();
	static {
		RESPONSEMAP.put("ééééééçççççç汉语漢語", "对!");
		RESPONSEMAP.put("foobar", "toto");
		RESPONSEMAP.put("æøå", "ÆØÅ");
	}

	public static void get(String id) {
		if (RESPONSEMAP.containsKey(id)){
			renderText(RESPONSEMAP.get(id));
		} else{
			error("expected id are: " + RESPONSEMAP.keySet());
		}
	}
	//Create a new resource
	public static void postOrPut(String id, long timestamp, boolean cachable, String[] multipleValues){
		String error = testParams();
		if (error.length() > 0) {
			Logger.info("postOrPut error: " + error);
			error("ERROR : " + error);
		}
		renderJSON("{id: 101}");
	}
	private static String testParams(){
		Logger.info("Params: " + params);
		String error = "";
		if (!("名字").equals(params.get("id")))  error += "id : was '"+ params.get("id") +"', expected "+ ("'名字") +"'\n";
		if (params.get("timestamp", Long.class)!=1200000L) error += "timestamp : was "+ params.get("timestamp") +", expected 1200000L\n";
		if (!params.get("cachable", Boolean.class)) error += "cachable is false, should have been true\n";
		String[] multipleValues = params.getAll("multipleValues");
		if (multipleValues.length != 3) error += "multipleValues should have been a length of 3 and was " + multipleValues.length + "\n";
		if (multipleValues.length > 0 && !multipleValues[0].equals("欢迎")) error += "multipleValues[0] : was '" + multipleValues[0] + "', expected  '名字'\n";
		if (multipleValues.length > 1 && !multipleValues[1].equals("dobrodošli")) error += "multipleValues[1] : was '"+ multipleValues[1] + "', expected ' dobrodošli'\n";
		if (multipleValues.length > 2 && !multipleValues[2].equals("ยินดีต้อนรับ")) error += "multipleValues[2] : was '"+ multipleValues[2] + "', expected 'ยินดีต้อนรับ'\n";
		
		Logger.info("error: " + error);
		return error;
	}
	public static void postOrPutFile(File file){
		testFile(file);
		renderText("POSTED!");
	}
	//test post multipart + string entity
	public static void postOrPutFileAndParams(File file){
		testFile(file);
		String error = testParams();
		if (error.length()> 0)error("ERROR : "+error);
		renderText("FILE AND PARAMS POSTED!");
	}
	
	private static void testFile(File file){
		if (file == null) error("File is null");
		if (file.length() != 749) error("File length is not 749 bytes as expected.");
		assert(file.getName().equals("éç欢迎.txt"));
		//read file
	}
	
	public static void returnParam() {
	    String param = params.get("paramÆØÅ");
	    Logger.info("Serverside: params: " + param);
	    renderText("param: " + param);
	}
	
	public static void echo(String id) {
        String r = id;
		Map<String, String[]> map = new TreeMap<String, String[]>(params.all());
        for (String key : map.keySet()) {
            String[] values = map.get(key);
            for (String v : values) {
                if (v == null) {
                    v = "flag";
                }
                r += "|" + key + "|" + v; 
            }
        }
        renderText(r);
    }

    public static void echoHttpMethod(String a, String b) {
        String r = request.method;
        if (a != null) {
            r += " a="+a;
        }
        if (b != null) {
            r += " b="+b;
        }
        renderText(r);
    }
	
}
