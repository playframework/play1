package play.libs;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import play.libs.F.ArchivedEventStream;
import play.libs.F.IndexedEvent;
import play.libs.F.Promise;

/**
 * @author olger
 *
 */
public class ArchivedEventStreamTest {

    private static final String VALUE_1 = "F";
    private static final String VALUE_2 = "FF";
    
    private ArchivedEventStream<String> stream;
    
    @Before
    public void setUp() {
        stream = new ArchivedEventStream<>(100);
    }

    /**
     * Test method for {@link play.libs.F.ArchivedEventStream#publish(java.lang.Object)}.
     */
    @Test
    public void testPublishMultiple() throws InterruptedException, ExecutionException {
        Promise<List<IndexedEvent<String>>> p1 = stream.nextEvents(0);
        Promise<List<IndexedEvent<String>>> p2 = stream.nextEvents(0);
        Promise<List<IndexedEvent<String>>> p3 = stream.nextEvents(0);
        
        stream.publish(VALUE_1);
        assertTrue(p1.isDone());
        assertTrue(p2.isDone());
        assertTrue(p3.isDone());
        
        assertEquals(1, p1.get().size());
        assertEquals(VALUE_1, p1.get().get(0).data);
        assertEquals(1, p2.get().size());
        assertEquals(VALUE_1, p2.get().get(0).data);
        assertEquals(1, p2.get().size());
        assertEquals(VALUE_1, p2.get().get(0).data);
        
        stream.publish(VALUE_2);
        assertTrue(p1.isDone());
        assertTrue(p2.isDone());
        assertTrue(p3.isDone());
        
        assertEquals(1, p1.get().size());
        assertEquals(VALUE_1, p1.get().get(0).data);
        assertEquals(1, p2.get().size());
        assertEquals(VALUE_1, p2.get().get(0).data);
        assertEquals(1, p2.get().size());
        assertEquals(VALUE_1, p2.get().get(0).data);
    }

}
