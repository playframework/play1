package controllers;

import java.io.File;

import play.mvc.Controller;

public class Rest extends Controller {

	public static void get(String id) {
		String expectedId = "ééééééçççççç汉语漢語";
		if (expectedId.equals(id)){
			renderText("对!");
		} else{
			error("expected id : "+expectedId+" and is "+id);
		}
	}
	//Create a new resource
	public static void postOrPut(String id, long timestamp, boolean cachable, String[] multipleValues){
		String error = testParams();
		if (error.length()> 0)error("ERROR : "+error);
		renderJSON("{id: 101}");
	}
	private static String testParams(){
		String error = "";
		if (!"名字".equals(params.get("id")))  error += "id : was '"+params.get("id")+"', expected '名字'\n";
		if (params.get("timestamp", Long.class)!=1200000L) error += "timestamp : was "+params.get("timestamp")+", expected 1200000L\n";
		if (!params.get("cachable", Boolean.class)) error += "cachable is false, should have been true\n";
		String[] multipleValues = params.getAll("multipleValues");
		if (multipleValues.length != 3) error += "multipleValues should have been a length of 3 and was "+multipleValues.length+"\n";
		if(!multipleValues[0].equals("欢迎")) error += "multipleValues[0] : was '"+multipleValues[0]+"', expected '名字'\n";
		if(!multipleValues[1].equals("dobrodošli")) error += "multipleValues[1] : was '"+multipleValues[1]+"', expected 'dobrodošli'\n";
		if(!multipleValues[2].equals("ยินดีต้อนรับ")) error += "multipleValues[2] : was '"+multipleValues[2]+"', expected 'ยินดีต้อนรับ'\n";
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
		if (file.length() != 749) error("File length is not 749 bytes as expected.");
		assert(file.getName().equals("éç欢迎.txt"));
		//read file
	}
	
}
