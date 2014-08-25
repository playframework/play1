package cn.bran.play;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import play.data.validation.Error;
import play.data.validation.Validation;

public class FieldErrors extends ArrayList<Error> {
    
	public FieldErrors(Collection<? extends Error> c) {
		super(c);
	}

	public FieldErrors(Validation val) {
		if (val != null) {
			this.addAll(val.errors());
		}
	}
	
	public Error forKey(String key) {
        return Validation.error(key);
    }
    
    public List<Error> allForKey(String key) {
        return Validation.errors(key);
    }
    
    @Override
    public String toString() {
    	Error[] errs = this.toArray(new Error[] {});
    	String[] es = new String[errs.length];
    	for (int i = 0; i < es.length; i++) {
    		es[i] = errs[i].toString2();
    	}
    	return this.size() == 0 ? "" : "Field validation errors: " + StringUtils.join(es, ",");
    }
}