package play.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.apache.asyncweb.common.HttpCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.stream.StreamWriteFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import play.Logger;
import play.Play;
import play.Play.Mode;

/**
 * Play! server
 */
public class Server {

    private SocketAcceptor acceptor;
    public static int port;

    public Server() {
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
        if (Play.mode == Mode.DEV) {
            acceptor = new NioSocketAcceptor(1);
        } else {
            acceptor = new NioSocketAcceptor();
        }
        Server.port = httpPort;
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new HttpCodecFactory()));
        acceptor.getFilterChain().addLast("stream", new StreamWriteFilter());
        acceptor.setReuseAddress(true);
        acceptor.getSessionConfig().setReuseAddress(true);
        acceptor.getSessionConfig().setReceiveBufferSize(1024 * 100);
        acceptor.getSessionConfig().setSendBufferSize(1024 * 100);
        acceptor.getSessionConfig().setTcpNoDelay(true);
        acceptor.getSessionConfig().setSoLinger(-1);
        acceptor.setBacklog(50000);
        acceptor.setHandler(new HttpHandler());
        try {
            acceptor.bind(new InetSocketAddress(address, httpPort));
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
            acceptor.dispose();
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        File root = new File(System.getProperty("application.path"));
        Play.init(root, System.getProperty("play.id", ""));
        if(System.getProperty("precompile") == null) {
            new Server();
        } else {
            Logger.info("Done.");
        }
    }
}
