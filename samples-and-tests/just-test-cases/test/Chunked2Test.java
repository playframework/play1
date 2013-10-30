import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.junit.Test;
import play.test.UnitTest;

public class Chunked2Test extends UnitTest {
    private String callChunked2AndGetFirstLine() {
        String result = null;
        BufferedReader reader = null;
        try {
            URL url = new URL("http://localhost:9003/Application/writeChunks2");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = reader.readLine();
            if (line != null)
                result = line.trim();
        }
        catch (IOException e) {
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                }
            }
        }
        return result;
    }

    private void sleep() {
        try {
            Thread.sleep(5000);
        }
        catch (InterruptedException e) {
        }
    }

    @Test
    public void testChunked2() {
        assertEquals("Go, Go, Igo!", callChunked2AndGetFirstLine());
        sleep();
        assertEquals("Go, Go, Igo!", callChunked2AndGetFirstLine());
    }
}

