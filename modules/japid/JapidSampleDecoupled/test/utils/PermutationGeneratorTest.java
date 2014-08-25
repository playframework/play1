package utils;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PermutationGeneratorTest {
	  
    @Test
    public void testPerm() {
    	List<String> ss = Arrays.asList("1", "2", "3", "4", "5", "6");
    	PermutationGenerator<String> pg = new PermutationGenerator(ss);
    	int l = 0;
    	while (pg.hasNext()) {
    		List<String> next = pg.next();
    		l++;
    		System.out.println(next.toString());
    	}
    	
    	System.out.println("total lines: " + l);
    }
    
    @Test
    public void testComboCount() {
    	BigInteger combinationsCount = PermutationGenerator.getCombinationsCount(5, 3);
    	assertEquals(10, combinationsCount.intValue());
    	combinationsCount = PermutationGenerator.getCombinationsCount(7, 5);
    	assertEquals(21, combinationsCount.intValue());
    }
}
