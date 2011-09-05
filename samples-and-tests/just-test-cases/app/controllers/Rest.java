package controllers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.mvc.Controller;

public class Rest extends Controller {
    

    // filtering all strings through this method - this makes it possible
    // to replace strings that does work in utf-8 but not in iso-8859-1.
    // This enables us to run this testsuite when Play! is using different encoding..
    public static String filterString(String s) {
//        if (!play.Play.defaultWebEncoding.equalsIgnoreCase("utf-8")) {
//            if ("ééééééçççççç汉语漢語".equals(s)) {
//                return "æøå1";
//            }
//            if ("对!".equals(s)) {
//                return "æøå2";
//            }
//            if ("名字".equals(s)) {
//                return "æøå3";
//            }
//            if ("dobrodošli".equals(s)) {
//                return "æøå4";
//            }
//            if ("欢迎".equals(s)) {
//                return "æøå5";
//            }
//            if ("ยินดีต้อนรับ".equals(s)) {
//                return "æøå6";
//            }
//            if ("éç欢迎".equals(s)) {
//                return "æøå7";
//            }
//        }
        return s;
    }

	private static Map<String, String> RESPONSEMAP = new HashMap<String, String>();
	static {
		RESPONSEMAP.put(filterString("ééééééçççççç汉语漢語"), filterString("对!"));
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
		if (!filterString("名字").equals(params.get("id")))  error += "id : was '"+params.get("id")+"', expected "+filterString("'名字")+"'\n";
		if (params.get("timestamp", Long.class)!=1200000L) error += "timestamp : was "+params.get("timestamp")+", expected 1200000L\n";
		if (!params.get("cachable", Boolean.class)) error += "cachable is false, should have been true\n";
		String[] multipleValues = params.getAll("multipleValues");
		if (multipleValues.length != 3) error += "multipleValues should have been a length of 3 and was "+multipleValues.length+"\n";
		if (multipleValues.length > 0 && !multipleValues[0].equals(filterString("欢迎"))) error += "multipleValues[0] : was '"+multipleValues[0]+"', expected "+filterString("'名字")+"'\n";
		if (multipleValues.length > 1 && !multipleValues[1].equals(filterString("dobrodošli"))) error += "multipleValues[1] : was '"+multipleValues[1]+"', expected '"+filterString("dobrodošli")+"'\n";
		if (multipleValues.length > 2 && !multipleValues[2].equals(filterString("ยินดีต้อนรับ"))) error += "multipleValues[2] : was '"+multipleValues[2]+"', expected '"+filterString("ยินดีต้อนรับ")+"'\n";
		
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
		assert(file.getName().equals(filterString("éç欢迎")+ ".txt"));
		//read file
	}
	
	public static void returnParam() {
	    String param = params.get("paramÆØÅ");
	    Logger.info("Serverside: params: " + param);
	    renderText("param: " + param);
	}
	
	public static void echo(String id) {
        String r = id;

        for ( String key : params.all().keySet()) {
            String[] values = params.all().get(key);
            for( String v : values) {
                if ( v == null) {
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
