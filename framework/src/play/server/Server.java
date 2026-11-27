package play.server;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import play.Logger;
import play.Play;
import play.Play.Mode;
import play.libs.IO;
import play.server.ssl.SslHttpServerPipelineFactory;

public class Server {

    public static int httpPort;
    public static int httpsPort;

    public static final String PID_FILE = "server.pid";

    private final List<Channel> channels = new ArrayList<>();
    private final List<MultiThreadIoEventLoopGroup> eventLoopGroups = new ArrayList<>();
    private final ThreadFactory threadFactory;

    private MultiThreadIoEventLoopGroup newEventLoopGroup() {
        return threadFactory == null
                ? new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
                : new MultiThreadIoEventLoopGroup(threadFactory, NioIoHandler.newFactory());
    }

    /**
     * Stops this server instance and releases the bound ports.
     * <p>
     * This is mainly useful when the server was started in-process (for example
     * by the JUnit launcher listener) and has to be shut down again without
     * terminating the JVM.
     */
    public void stop() {
        for (Channel channel : channels) {
            if (channel.isOpen()) {
                channel.close().syncUninterruptibly();
            }
        }
        channels.clear();
        for (MultiThreadIoEventLoopGroup group : eventLoopGroups) {
            group.shutdownGracefully().syncUninterruptibly();
        }
        eventLoopGroups.clear();
    }

    public Server(String[] args) {
        this(args, null);
    }

    /**
     * @param args command line arguments
     * @param threadFactory factory for the Netty event loop threads, or {@code null}
     *                      to use Netty's default non-daemon factory. Tests start the
     *                      server in-process and pass a daemon factory so that a
     *                      missing launcher-session close cannot keep the JVM alive.
     */
    public Server(String[] args, ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;

        System.setProperty("file.encoding", "utf-8");
        Properties p = Play.configuration;

        httpPort = Integer.parseInt(getOpt(args, "http.port", p.getProperty("http.port", "-1")));
        httpsPort = Integer.parseInt(getOpt(args, "https.port", p.getProperty("https.port", "-1")));

        if (httpPort == -1 && httpsPort == -1) {
            httpPort = 9000;
        }

        if (httpPort == httpsPort) {
            Logger.error("Could not bind on https and http on the same port " + httpPort);
            Play.fatalServerErrorOccurred();
        }

        InetAddress address = null;
        InetAddress secureAddress = null;
        try {
            if (p.getProperty("http.address") != null) {
                address = InetAddress.getByName(p.getProperty("http.address"));
            } else if (System.getProperties().containsKey("http.address")) {
                address = InetAddress.getByName(System.getProperty("http.address"));
            }

        } catch (Exception e) {
            Logger.error(e, "Could not understand http.address");
            Play.fatalServerErrorOccurred();
        }
        try {
            if (p.getProperty("https.address") != null) {
                secureAddress = InetAddress.getByName(p.getProperty("https.address"));
            } else if (System.getProperties().containsKey("https.address")) {
                secureAddress = InetAddress.getByName(System.getProperty("https.address"));
            }
        } catch (Exception e) {
            Logger.error(e, "Could not understand https.address");
            Play.fatalServerErrorOccurred();
        }

        try {
            if (httpPort != -1) {
                MultiThreadIoEventLoopGroup bossGroup = newEventLoopGroup();
                MultiThreadIoEventLoopGroup workerGroup = newEventLoopGroup();
                eventLoopGroups.add(bossGroup);
                eventLoopGroups.add(workerGroup);

                ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpServerPipelineFactory())
                    .childOption(ChannelOption.TCP_NODELAY, true);

                channels.add(bootstrap.bind(new InetSocketAddress(address, httpPort)).syncUninterruptibly().channel());

                if (Play.mode == Mode.DEV) {
                    if (address == null) {
                        Logger.info("Listening for HTTP on port %s (Waiting a first request to start) ...", httpPort);
                    } else {
                        Logger.info("Listening for HTTP at %2$s:%1$s (Waiting a first request to start) ...", httpPort, address);
                    }
                } else {
                    if (address == null) {
                        Logger.info("Listening for HTTP on port %s ...", httpPort);
                    } else {
                        Logger.info("Listening for HTTP at %2$s:%1$s  ...", httpPort, address);
                    }
                }
            }
        } catch (ChannelException e) {
            Logger.error(e, "Could not bind on port " + httpPort);
            Play.fatalServerErrorOccurred();
        }

        try {
            if (httpsPort != -1) {
                MultiThreadIoEventLoopGroup bossGroup = newEventLoopGroup();
                MultiThreadIoEventLoopGroup workerGroup = newEventLoopGroup();
                eventLoopGroups.add(bossGroup);
                eventLoopGroups.add(workerGroup);

                ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new SslHttpServerPipelineFactory())
                    .childOption(ChannelOption.TCP_NODELAY, true);

                channels.add(bootstrap.bind(new InetSocketAddress(secureAddress, httpsPort)).syncUninterruptibly().channel());

                if (Play.mode == Mode.DEV) {
                    if (secureAddress == null) {
                        Logger.info("Listening for HTTPS on port %s (Waiting a first request to start) ...", httpsPort);
                    } else {
                        Logger.info("Listening for HTTPS at %2$s:%1$s (Waiting a first request to start) ...", httpsPort, secureAddress);
                    }
                } else {
                    if (secureAddress == null) {
                        Logger.info("Listening for HTTPS on port %s ...", httpsPort);
                    } else {
                        Logger.info("Listening for HTTPS at %2$s:%1$s  ...", httpsPort, secureAddress);
                    }
                }

            }

        } catch (ChannelException e) {
            Logger.error(e, "Could not bind on port " + httpsPort);
            Play.fatalServerErrorOccurred();
        }
        if (Play.mode == Mode.DEV || Play.runningInTestMode()) {
           // print this line to STDOUT - not using logger, so auto test runner will not block if logger is misconfigured (see #1222)     
           System.out.println("~ Server is up and running");
        }
    }

    private String getOpt(String[] args, String arg, String defaultValue) {
        String s = "--" + arg + "=";
        for (String a : args) {
            if (a.startsWith(s)) {
                return a.substring(s.length());
            }
        }
        return defaultValue; 
    }

    private static void writePID(File root) {
        String pid = Long.toString(ProcessHandle.current().pid());
        File pidfile = new File(root, PID_FILE);
        if (pidfile.exists()) {
            throw new RuntimeException("The " + PID_FILE + " already exists. Is the server already running?");
        }
        IO.write(pid.getBytes(), pidfile);
    }

    public static void main(String[] args) throws Exception {
        try {
            File root = new File(System.getProperty("application.path", "."));
            if (System.getProperty("precompiled", "false").equals("true")) {
                Play.usePrecompiled = true;
            }
            if (System.getProperty("writepid", "false").equals("true")) {
                writePID(root);
            }

            Play.init(root, System.getProperty("play.id", ""));

            if (System.getProperty("precompile") == null) {
                new Server(args);
            } else {
                Logger.info("Done.");
            }
        }
        catch (Throwable e) {
            Logger.fatal(e, "Failed to start");
            System.exit(1);
        }
    }
}
