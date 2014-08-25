/**
 * Copyright 2010 Bing Ran<bing_ran@hotmail.com> 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package cn.bran.play.tags;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import cn.bran.japid.tags.SimpleTag;

import play.cache.Cache;

/**
 * include tag for including a action result to the parent template
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class Include extends SimpleTag {

	public Include(StringBuilder out) {
		super(out);
	}

	/**
	 * an action call with cache
	 * 
	 * @param ac
	 *            the action invocation wrapper
	 * @param key
	 *            user specified key, the user needs to explicitly compose a key
	 *            from the action and arguments. The template transformer can
	 *            put the action calling string as prefix of the key, e.g., if
	 *            the acton is Application.show(post), the key should be compose
	 *            of "Application.show()-" + post.toString(). Note we cannot use
	 *            "post" literally since post as the var name can be anything
	 *            that the template author chooses to use. Problem: Application
	 *            needs to be imported or all controllers package must be
	 *            imported. Controllers in sub packages needs special care in
	 *            importing since a simple import controllers.* won't work for
	 *            controllers in sub packages.
	 * @param ttl
	 *            expiration Ex: 10s, 3mn, 8h
	 */
	public void render(ActionCall ac, String key, String ttl) {
		String co = (String) Cache.get(key);
		if (co == null) {
			co = ac.callAction();
			Cache.set(key, co, ttl);
		}
		p(co);
		return;
	}

	/**
	 * a passthrough action call
	 * 
	 * @param ac
	 */
	public void render(ActionCall ac) {
		String co = ac.callAction();
		p(co);
		return;
	}

	/**
	 * the caller will wrap the action invocation in the callAction()
	 * 
	 * @author Bing Ran<bing_ran@hotmail.com>
	 * 
	 */
	public static interface ActionCall {
		/**
		 * 
		 * @return the render result
		 */
		public String callAction();
	}
}
