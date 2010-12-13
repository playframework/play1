package play.mvc.results;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class Stream extends Result {

    ChunkedInput source;

    public Stream(ChunkedInput source) {
        this.source = source;
    }

    public void apply(Request request, Response response) {
        response.setHeader("Transfer-Encoding", "chunked");
        response.direct = source;
    }

    public static abstract class ChunkedInput {

        public abstract boolean hasNextChunk();
        public abstract Object nextChunk();
        public abstract boolean isEndOfInput();
        public abstract void close();

        private List<ChunkedInputListener> listeners = new ArrayList<ChunkedInputListener>();
        public void notifyNewChunks() {
            for(ChunkedInputListener listener : listeners) {
                listener.onNewChunks();
            }
        }

        public void addListener(ChunkedInputListener listener) {
            this.listeners.add(listener);
        }

        public static interface ChunkedInputListener {
            void onNewChunks();
        }

        public Object createChunk(String message) {
            try {
                StringWriter writer = new StringWriter();
                Integer l = message.getBytes("utf-8").length + 2;
                writer.append(Integer.toHexString(l)).append("\r\n").append(message).append("\r\n\r\n");
                return writer.toString();
            } catch(UnsupportedEncodingException e) {
                throw new UnexpectedException(e);
            }
        }

    }

}
