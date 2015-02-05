package controllers;

import java.io.IOException;

import play.mvc.Controller;


public class EventSources extends Controller {

    public static void index() {
        render();
    }
    
    public static void chunkTest() {
        response.setContentTypeIfNotSet("text/plain");
        response.writeChunk("Hello");
    
        // hack to make chrome start displaying (needed in both 1.2.5 and 1.3.x)
        for (int x = 0; x < 5000; x++)
            response.writeChunk(" ");
    
        int i = 0;
        while (i++ < 10) {
            await(1000);
            response.writeChunk(11 - i + " Why Hello!\n");
        }
    
        await(1000);
        response.writeChunk("...Blastoff?\n");
    
    }
    
    public static void events() throws IOException {
        response.encoding = "UTF-8";
        response.contentType = "text/event-stream";
        int id = 0;    
        int i = 0;
        while (i++ < 3) {
            await(1000);
            response.writeChunk("event: message\n"+ "id: " + ++id + "\n"+ "retry: 5000\n"  + "data: hello " + i + "\n\n");
        }
    
        await(1000);
        response.writeChunk("event: message\n" + "id: " + ++id + "\n" + "data: bye\r\n\n");
    }

}
