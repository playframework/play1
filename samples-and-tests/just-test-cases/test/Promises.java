import org.junit.*;

import java.util.*;

import play.test.*;
import play.jobs.*;
import play.libs.*;
import play.libs.F.*;

public class Promises extends UnitTest {
    
    public static class DoSomething extends play.jobs.Job<F.Option<String>> {
        
        long d;
        
        public DoSomething(long d) {
            this.d = d;
        }
        
        public F.Option<String> doJobWithResult() throws Exception {
            Thread.sleep(d);
            if(d > 200) {
                return F.Option.Some("-> " + d);
            }
            return F.Option.None();
        }
        
    }
    
    public static class DoSomething2 extends play.jobs.Job<String> {
        
        long d;
        
        public DoSomething2(long d) {
            this.d = d;
        }
        
        public String doJobWithResult() throws Exception {
            Thread.sleep(d);
            return "-> " + d;
        }
        
    }
    
    @Test
    public void waitAny() throws Exception {
        
        boolean p = false;
        for(String s : Promise.waitAny(new DoSomething(300).now(), new DoSomething(250).now()).get()) {
            assertEquals("-> 250", s);
            p = true;
        }
        assertTrue("Loop missed?", p);
        
        for(String s : Promise.waitAny(new DoSomething(100).now(), new DoSomething(250).now()).get()) {
            fail("Oops");
        }
        
    }
    
    @Test
    public void waitEither() throws Exception {
        
        F.Either<F.Option<String>,String> e = Promise.waitEither(new DoSomething(201).now(), new DoSomething2(300).now()).get();
        
        boolean p = false;
        for(F.Option<String> o : e._1) {
            for(String s : o) {
                assertEquals("-> 201", s);
                p = true;
            }
        }
        assertTrue("Loop missed?", p);
        
        for(String s : e._2) {
            fail("Oops");
        }
        
        e = Promise.waitEither(new DoSomething(201).now(), new DoSomething2(100).now()).get();
        
        for(F.Option<String> o : e._1) {
            for(String s : o) {
                fail("Oops");
            }
        }
        
        p = false;
        for(String s : e._2) {
            assertEquals("-> 100", s);
            p = true;
        }
        assertTrue("Loop missed?", p);
        
    }
    
    @Test
    public void waitAll() throws Exception {
        
        List<String> s = Promise.waitAll(new DoSomething2(200).now(), new DoSomething2(10).now()).get();
        assertEquals(2, s.size());
        assertEquals("-> 200", s.get(0));
        assertEquals("-> 10", s.get(1));
        
    }
    
    @Test
    public void wait2() throws Exception {
        
        F.Tuple<String, F.Option<String>> t = Promise.wait2(new DoSomething2(200).now(), new DoSomething(10).now()).get();
        
        assertEquals("-> 200", t._1);
        for(String s : t._2) {
            fail("Oops");
        }
        
    }
    
    @Test
    public void eventStream() throws Exception {
        
        final EventStream<String> stream = new EventStream<String>();
        
        new Thread() {
            
            public void run() {
                try {
                    Thread.sleep(300);
                    stream.publish("Héhé");
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }                
            }
            
        }.start();
        
        for(int i=0; i<6; i++) {
            
            String eventReceived = Promise.waitAny(stream.nextEvent(), new DoSomething2(100).now()).get();
            System.out.println(i + " -> " + eventReceived);
            
            switch(i) {
                case 0:
                    assertEquals("-> 100", eventReceived);
                    Thread.sleep(500);
                    break;
                case 1:
                    assertEquals("Héhé", eventReceived);
                    break;  
                case 2:
                    assertEquals("-> 100", eventReceived);
                    stream.publish("Coco");
                    stream.publish("Kiki");
                    break;
                case 3:
                    assertEquals("Coco", eventReceived);
                    break;
                case 4:
                    assertEquals("Kiki", eventReceived);
                    break;
                case 5:
                    assertEquals("-> 100", eventReceived);
                    break;
            }
        }        
        
    }
    
    @Test
    public void bufferedEventStream() throws Exception {
        
        IndexedEvent.resetIdGenerator();
        final ArchivedEventStream<String> stream = new ArchivedEventStream<String>(5);
        
        new Thread() {
            
            public void run() {
                try {
                    Thread.sleep(300);
                    stream.publish("Héhé");
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }                
            }
            
        }.start();
        
        long lastSeen = 0;
        
        for(int i=0; i<10; i++) {
            
            F.Either<List<IndexedEvent<String>>,String> eventReceived = Promise.waitEither(stream.nextEvents(lastSeen), new DoSomething2(100).now()).get();
            System.out.println(i + " -> " + eventReceived);
            
            switch(i) {
                case 0:
                    assertEquals("-> 100", eventReceived._2.get());
                    Thread.sleep(500);
                    break;
                case 1:
                    assertEquals(1, eventReceived._1.get().size());
                    assertEquals("Héhé", eventReceived._1.get().get(0).data);
                    assertEquals((Long)1L, eventReceived._1.get().get(0).id);
                    break;  
                case 2:
                    assertEquals(1, eventReceived._1.get().size());
                    assertEquals("Héhé", eventReceived._1.get().get(0).data);
                    assertEquals((Long)1L, eventReceived._1.get().get(0).id);
                    lastSeen = eventReceived._1.get().get(0).id;
                    break;
                case 3:
                    assertEquals("-> 100", eventReceived._2.get());
                    stream.publish("Coco");
                    stream.publish("Kiki");
                    break;
                case 4:
                    assertEquals(2, eventReceived._1.get().size());
                    assertEquals("Coco", eventReceived._1.get().get(0).data);
                    assertEquals((Long)2L, eventReceived._1.get().get(0).id);
                    lastSeen = eventReceived._1.get().get(0).id;
                    break;
                case 5:
                    assertEquals(1, eventReceived._1.get().size());
                    assertEquals("Kiki", eventReceived._1.get().get(0).data);
                    assertEquals((Long)3L, eventReceived._1.get().get(0).id);
                    lastSeen = eventReceived._1.get().get(0).id;
                    break;
                case 6:
                    assertEquals("-> 100", eventReceived._2.get());
                    stream.publish("Boum");
                    stream.publish("Yop");
                    stream.publish("Paf");
                    break;
                case 7:
                    assertEquals(3, eventReceived._1.get().size());
                    assertEquals("Boum", eventReceived._1.get().get(0).data);
                    assertEquals((Long)4L, eventReceived._1.get().get(0).id);
                    lastSeen = 0;
                    break;
                case 8:
                    assertEquals(5, eventReceived._1.get().size());
                    assertEquals("Coco", eventReceived._1.get().get(0).data);
                    assertEquals((Long)2L, eventReceived._1.get().get(0).id);
                    lastSeen = 100;
                    break;
                case 9:
                    assertEquals("-> 100", eventReceived._2.get());
                    break;
            }
        }
        
    }
    
    
}