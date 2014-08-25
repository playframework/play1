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

public interface FlashScope {
	//these two constants must match those used in the play Flash
	public static final String SUCCESS = "success"; 
	public static final String ERROR = "error"; 

	public abstract boolean hasSuccess();

	public abstract Object success();

	public abstract void putSuccess(Object message);

	public abstract boolean hasError();

	public abstract Object error();

	public abstract boolean contains(String key);

	public abstract Object get(String key);

}