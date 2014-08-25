package cn.bran.play;

import java.util.Map;

import play.templates.Template;

/**
 * used to report error in DEV mode
 * 
 * @author bran
 *
 */
public class JapidPlayTemplate extends Template {

	@Override
	public void compile() {
	}

	@Override
	protected String internalRender(Map<String, Object> args) {
		return null;
	}

}
