package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.libs.F.*;
import play.mvc.Http.*;

import static play.mvc.Http.WebSocketEvent.*;

import java.util.*;

import models.*;

public class WebSocket extends Controller {
    
    public static void room(String user) {
        render(user);
    }

    public static class ChatRoomSocket extends WebSocketController {
        
        public static void join(String user) {
            
            ChatRoom room = ChatRoom.get();
            
            // Socket connected, join the chat room
            EventStream<Message> roomMessagesStream = room.join(user);
         
            // Loop while the socket is open
            while(inbound.isOpen()) {
                
                // Wait for an event (either something coming on the inbound socket channel, or ChatRoom messages)
                Either<WebSocketEvent,Message> e = await(Promise.waitEither(
                    inbound.nextEvent(), 
                    roomMessagesStream.nextEvent()
                ));
                
                // Case: TextEvent received on the socket
                for(String userMessage: TextFrame.match(e._1)) {
                    room.say(user, userMessage);
                }
                
                // Case: New messages on the chat room
                for(Message roomMessage : e._2) {
                    outbound.send("%s:%s", roomMessage.user, roomMessage.text);
                }
                
                // Case: The socket has been closed
                for(WebSocketClose closed : SocketClosed.match(e._1)) {
                    room.leave(user);
                    disconnect();
                }
                
            }
            
        }
        
    }
    
}

