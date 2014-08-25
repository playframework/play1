import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;

public class BasicTest extends UnitTest {

	static final int MIL = 1024000;
    @Test
    public void aVeryImportantThingToTest() {
        assertEquals(2, 1 + 1);
    }

    @Test
    public void testMethodMemoryUsage() {
    	long totalMemory = Runtime.getRuntime().freeMemory();
    	System.out.println("Total mem at entry: " + totalMemory / MIL);
    	
    	Small[] smalls = new Small[MIL];
    	for(int i = 0; i < MIL; i++) {
    		smalls[i] = new Small();
    	}
    	
    	totalMemory = Runtime.getRuntime().freeMemory();
    	System.out.println("Total mem with smalls: " + totalMemory / MIL);
    	
    	Big[] bigs = new Big[MIL];
    	for(int i = 0; i < MIL; i++) {
    		bigs[i] = new Big();
    	}
    	totalMemory = Runtime.getRuntime().freeMemory();
    	System.out.println("Total mem with bigs: " + totalMemory / MIL);
    	
    	assertNotNull(smalls[MIL - 1]);
    	assertNotNull(bigs[MIL - 1]);
    }
    
    
    static class Small {}
    static class Big {
    	void method() {
    		int a = 0;
    		int b = a;
    		b++;
    		int c = b + a;
    		String[] sa = new String[c];
    		for (int i = 0; i < c; i++) {
    			sa[i] = "hello";
    		}
    		System.out.println(sa.length);
    	}
    }
    
    
}
