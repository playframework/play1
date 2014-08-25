/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils;

import java.util.*;

/**
 * A Collections utility class
 *
 * @author Paul King
 * @author Jim White
 */
public class GroovyCollections {

    /**
     * Finds all non-null subsequences of a list.
     * E.g. <code>subsequences([1, 2, 3])</code> would be:
     * [[1, 2, 3], [1, 3], [2, 3], [1, 2], [1], [2], [3]]
     *
     * @param items the List of items
     * @return the subsequences from items
     */
    public static <T> Set<List<T>> subsequences(List<T> items) {
        Set<List<T>> ans = new HashSet<List<T>>();
        for (T h : items) {
            Set<List<T>> next = new HashSet<List<T>>();
            for (List<T> it : ans) {
                List<T> sublist = new ArrayList<T>(it);
                sublist.add(h);
                next.add(sublist);
            }
            next.addAll(ans);
            List<T> hlist = new ArrayList<T>();
            hlist.add(h);
            next.add(hlist);
            ans = next;
        }
        return ans;
    }

    /**
     * get sub sequence of a specific length
     * 
     * @param <T>
     * @param items
     * @param length
     * @return
     */
    public static <T> Set<List<T>> subsequences(List<T> items, int length) {
    	Set<List<T>> sub = subsequences(items);
    	Iterator<List<T>> it = sub.iterator();
    	while(it.hasNext()) {
    		List<T> next =  it.next();
    		if (next.size() != length) {
    			it.remove();
    		}
    	}
    	return sub;
    }
    

}
