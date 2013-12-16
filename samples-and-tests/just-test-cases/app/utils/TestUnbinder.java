package utils;

import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;

import play.data.binding.*;

public class TestUnbinder implements TypeUnbinder<String> {

    @Override
    public boolean unBind(Map<String, Object> result, Object src,
	    Class<?> srcClazz, String name, Annotation[] annotations)
	    throws Exception {
	String value = (String) src;
	if(value != null){
	    value = value.replaceAll("^--", "##").replaceAll("--$", "##");
	}
	result.put(name, value);
	return true;
    }    
}

