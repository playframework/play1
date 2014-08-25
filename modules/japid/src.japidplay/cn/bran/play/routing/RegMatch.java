/**
 * 
 */
package cn.bran.play.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * a match from a Matcher's successful match()
 * @author bran
 *
 */
public class RegMatch {
	String group;
	List<String> subgroups;
	
	/**
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param matcher a matcher that is in a matched status
	 * @return a RegMatch representing a single match
	 */
	public static RegMatch from(Matcher matcher) {
		RegMatch m = new RegMatch();
		m.group = matcher.group();
		int c = matcher.groupCount();
		m.subgroups = new ArrayList<String>();
		for (int i = 1; i <= c; i++) {
			m.subgroups.add(matcher.group(i));
		}
		return m;
	}
	
	/**
	 * extract all the match info from a newly created matcher.
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param matcher
	 * @return
	 */
	public static List<RegMatch> allMatches(Matcher matcher) {
		List<RegMatch> ret = new ArrayList<RegMatch>();
		
		while(matcher.find()) {
			ret.add(from(matcher));
		}
		return ret;
	}

	/**
	 * get all the group(0) matches in string.
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param p
	 * @param string
	 * @return
	 */
	static List<String> findAllIn(Pattern p, String string) {
		List<String> ret = new ArrayList<String>();
		Matcher matcher = p.matcher(string);
		while (matcher.find()) {
			ret.add(matcher.group());
		}
		return ret;
	}

	/**
	 * get all the match data from all the matches. Sub group info is encapsulated in the RegMatch.
	 *  
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param p
	 * @param string
	 * @return
	 */
	static List<RegMatch> findAllMatchesIn(Pattern p, String string) {
		Matcher matcher = p.matcher(string);
		return allMatches(matcher);
	}

	/**
	 * get all the match data from all the matches. Sub group info is encapsulated in the RegMatch.
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param ps
	 * @param string
	 * @return
	 */
	static List<RegMatch> findAllMatchesIn(String ps, String string) {
		Matcher matcher = Pattern.compile(ps).matcher(string);
		return allMatches(matcher);
	}
	
}
