package play;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import org.junit.Test;

public class CommandLineTest extends org.junit.Assert {

    File getFrameworkPath() {
        return new File(CommandLineTest.class.getResource("/play/CommandLineTest.class").getFile()).getParentFile().getParentFile().getParentFile().getParentFile();
    }

    @Test
    public void play() throws Exception {
        String play = new File(getFrameworkPath(), "play").getAbsolutePath();
        Process p = Runtime.getRuntime().exec(play);
        try {
            String out = read(p.getInputStream(), 100);
            assertTrue("Bad out ? " + out, out.contains("~ Usage: play command [path or current directory]"));
            String errors = read(p.getErrorStream(), 100);
            assertTrue("Error stream is not empty : " + errors, errors.length() == 0);
        } finally {
            p.destroy();
        }
    }

    @Test
    public void playNew() throws Exception {
        String play = new File(getFrameworkPath(), "play new " + new File(getFrameworkPath(), "framework/tests-tmp/hop")).getAbsolutePath();
        Process p = Runtime.getRuntime().exec(play);
        try {
            String out = read(p.getInputStream(), 100);
            assertTrue("No question ? " + out, out.contains("~ What is the application name ?"));
            write(p.getOutputStream(), "Hop\n");
            out = read(p.getInputStream(), 1000);
            assertTrue("Not created ? " + out, out.contains("~ Ok, the application is created."));
            String errors = read(p.getErrorStream(), 100);
            assertTrue("Error stream is not empty : " + errors, errors.length() == 0);
            assertTrue("hop/app/controllers does not exists", new File(getFrameworkPath(), "framework/tests-tmp/hop/app/controllers").exists());
            assertTrue("hop/app/models does not exists", new File(getFrameworkPath(), "framework/tests-tmp/hop/app/models").exists());
            assertTrue("hop/conf/application.conf does not exists", new File(getFrameworkPath(), "framework/tests-tmp/hop/conf/application.conf").exists());
            assertTrue("hop/conf/routes does not exists", new File(getFrameworkPath(), "framework/tests-tmp/hop/conf/routes").exists());
            assertTrue("hop/lib does not exists", new File(getFrameworkPath(), "framework/tests-tmp/hop/lib").exists());
        } finally {
            p.destroy();
        }
    }

    @Test
    public void playStart() throws Exception {
        String play = new File(getFrameworkPath(), "play start " + new File(getFrameworkPath(), "framework/tests-tmp/hop")).getAbsolutePath();
        Process p = Runtime.getRuntime().exec(play);
        try {
            String out = read(p.getInputStream(), 2000);
            assertTrue("Server not started ? " + out, out.contains("hop is started"));
        } finally {
            p.destroy();
        }
    }
    
    @Test
    public void testOneRequest() throws Exception {
        String content = read(new URL("http://localhost:9000/").openStream(), 2000);
        assertTrue("No welcome message ? " + content, content.contains("This application is empty"));
    }
    
    @Test
    public void playStop() throws Exception {
        String play = new File(getFrameworkPath(), "play stop " + new File(getFrameworkPath(), "framework/tests-tmp/hop")).getAbsolutePath();
        Process p = Runtime.getRuntime().exec(play);
        try {
            String out = read(p.getInputStream(), 2000);
            assertTrue("Server not stopped ? " + out, out.contains("hop is stopped"));
        } finally {
            p.destroy();
        }
    }

    String read(InputStream is, int maxWait) throws Exception {
        InputStreamReader reader = new InputStreamReader(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long s = System.currentTimeMillis();
        while (true) {
            if (reader.ready()) {
                baos.write(reader.read());
                s = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() > s + maxWait) { // Wait 100ms to something to read or return
                    break;
                }
            }
        }
        return new String(baos.toByteArray());
    }

    void write(OutputStream os, String content) throws Exception {
        os.write(content.getBytes());
        os.flush();
    }
}
