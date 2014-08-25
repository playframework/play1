package utils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class GroovyCollectionsTest {

	@Test
	public void testSubsequences() {
		Set<List<String>> sub = GroovyCollections.subsequences(Arrays.asList("a", "b", "c", "d", "e"), 3);
		System.out.println(sub.toString());
	}

}
