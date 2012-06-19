package play.server;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Useless channel buffer only used to wrap the input stream....
 */
public class MemoryChannelBuffer extends InputStreamChannelBuffer {
	
    private final ByteArrayInputStream is;
    
// --------------------------->

    public MemoryChannelBuffer(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        try {
            this.is = new ByteArrayInputStream(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
// --------------------------->

    public InputStream getInputStream() {
        return is;
    }
}
