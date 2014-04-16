
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.List;

import org.junit.*;


import play.test.*;
import models.*;

public class BasicTest extends UnitTest {

    @Test
    public void simpleStreamTest() {    
        List<Integer> doubles = Arrays.asList(1, 2, 3)
                .stream()
                .map(e -> e*2)
                .collect(Collectors.toList());
        
        assertEquals(Arrays.asList(2, 4, 6), doubles);
    }

}
