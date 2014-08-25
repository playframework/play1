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
package cn.bran.japid.util;

/**
 * mappping action calling notation to URL
 * 
 * @author bran
 * 
 */
public interface UrlMapper {

	/**
	 * map an action to a relative url Note: the method internally need to be
	 * multi-threaded safe
	 * 
	 * @param action
	 *            name of static resource or action name, depending the args
	 *            param
	 * @param args
	 *            null indicates this is a static resource lookup, otherwise
	 *            it's an action lookup
	 * @return
	 */
	String lookup(String action, Object[] args);

	/**
	 * map an action to an absolute url Note: the method internally need to be
	 * multi-threaded safe
	 * 
	 * @param action
	 * @return
	 */
	String lookupAbs(String action, Object[] args);

	/**
	 * lookup statics
	 * 
	 * @param resource
	 * @return
	 */
	String lookupStatic(String resource);
	String lookupStaticAbs(String resource);
	

}
