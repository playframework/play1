package models;

/*
 * Copy below to play/test/ClassWithStaticFinalMap.java.xml
 */

import java.util.HashMap;
import java.util.Map;

import play.test.FixturesTest.MockModel;

public class ClassWithStaticFinalMap extends MockModel {
	public static final Map<String, Object> map = new HashMap<>();
	
	public String name;
	
	public ClassWithStaticFinalMap() {
		super();
	}
}
