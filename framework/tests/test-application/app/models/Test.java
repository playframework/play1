package models;

public class Test {
	
	public String prop = "Youpi";
	public String f = null;
	
	public static String test() {
		return "----- F -----";
	}

	
	public static String test2() {
		return "Tutu";
	}
	 	
	public void setProp(String v) {
		this.prop = v.toUpperCase();
	}
	
}