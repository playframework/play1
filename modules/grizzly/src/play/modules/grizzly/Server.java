package play.modules.grizzly;

import com.sun.grizzly.http.embed.GrizzlyWebServer;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.logging.Level;
import play.Logger;
import play.Play;
import play.Play.Mode;

public class Server {

    final GrizzlyWebServer ws;

    public Server(File applicationDir, String frameworkId) {
        java.util.logging.Logger.getLogger("").setLevel(Level.OFF);
        Properties p = Play.configuration;
        int httpPort = Integer.parseInt(p.getProperty("http.port", "9000"));
        InetAddress address = null;
        if (System.getProperties().containsKey("http.port")) {
            httpPort = Integer.parseInt(System.getProperty("http.port"));
        }
        try {
            if(p.getProperty("http.address") != null) {
                address = InetAddress.getByName(p.getProperty("http.address"));
            }
            if (System.getProperties().containsKey("http.address")) {
                address = InetAddress.getByName(System.getProperty("http.address"));
            }
        } catch(Exception e) {
            Logger.error(e, "Could not understand http.address");
            System.exit(-1);
        }
        ws = new GrizzlyWebServer(httpPort);
        ws.useAsynchronousWrite(true);
        ws.addGrizzlyAdapter(new PlayGrizzlyAdapter(applicationDir, frameworkId, ""), new String[] {"/"});
        try {
            ws.start();
            if (Play.mode == Mode.DEV) {
                if(address == null) {
                    Logger.info("Listening for HTTP on port %s (Waiting a first request to start) ...", httpPort);
                } else {
                    Logger.info("Listening for HTTP at %2$s:%1$s (Waiting a first request to start) ...", httpPort, address);
                }
            } else {
                if(address == null) {
                    Logger.info("Listening for HTTP on port %s ...", httpPort);
                } else {
                    Logger.info("Listening for HTTP at %2$s:%1$s  ...", httpPort, address);
                }
            }
        } catch (IOException e) {
            Logger.error("Could not bind on port " + httpPort, e);
            ws.stop();
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws Exception {
        File root = new File(System.getProperty("application.path"));
        new Server(root, System.getProperty("play.id", ""));
    }

}