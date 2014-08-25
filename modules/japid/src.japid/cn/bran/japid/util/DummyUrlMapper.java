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


public class DummyUrlMapper implements UrlMapper{
	
	/**
	 * must be multi-thread safe
	 */
	@Override
	public String lookup(String action, Object[] args) {
		if (args == null) {
			return action;
		}
		return "dummyaction?id=1";
	}

	/**
	 * must be multi-threaded safe
	 * 
	 */
	@Override
	public String lookupAbs(String action, Object[] args) {
		if (args == null) {
			return "/" + action;
		}
		return "http://dummp/action?id=1";
	}

	@Override
	public String lookupStatic(String resource) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookupStaticAbs(String resource) {
		// TODO Auto-generated method stub
		return null;
	}

}
