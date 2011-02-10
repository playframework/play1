package play.server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import play.Logger;
import play.Play;
import play.Play.Mode;
import play.server.ssl.SslHttpServerPipelineFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

public class Server {

	public static int[] httpPorts;
	public static int[] httpsPorts;

    public Server(String[] args) {

        System.setProperty("file.encoding", "utf-8");
        final Properties p = Play.configuration;

        httpPorts = propertyToIntArray(args, "http.port", -1);
        httpsPorts = propertyToIntArray(args, "https.port", -1);

		if (((httpPorts.length == 1) && (httpPorts[0] == -1)) && ((httpsPorts.length == 1) && (httpsPorts[0] == -1))) {
			httpPorts = new int[] { 9000 };
		}

		// look for any colliding ports
		for (int httpPort : httpPorts) {
			for (int httpsPort : httpsPorts) {
				if (httpPort == httpsPort) {
					Logger.error("Could not bind on https and http on the same port " + httpPort);
					System.exit(-1);
				}
			}
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
            System.exit(-1);
        }
        try {
            if (p.getProperty("https.address") != null) {
                secureAddress = InetAddress.getByName(p.getProperty("https.address"));
            } else if (System.getProperties().containsKey("https.address")) {
                secureAddress = InetAddress.getByName(System.getProperty("https.address"));
            }
        } catch (Exception e) {
            Logger.error(e, "Could not understand https.address");
            System.exit(-1);
        }
        ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
        );
		if (httpPorts[0] != -1) {
			bootstrap.setPipelineFactory(new HttpServerPipelineFactory());
			List<Integer> boundedPorts = new LinkedList<Integer>();
			for (int httpPort : httpPorts) {
				try {
					bootstrap.bind(new InetSocketAddress(address, httpPort));
					boundedPorts.add(httpPort);
				} catch (ChannelException e) {
					Logger.error("Could not bind on port " + httpPort, e);
				}
			}
			bootstrap.setOption("child.tcpNoDelay", true);

			String port = boundedPorts.size() > 1 ? "ports" : "port";
			String boundedPortsString = integerListToCommaSeperatedString(boundedPorts, ", ");

			if (Play.mode == Mode.DEV) {
				if (address == null) {
					Logger.info("Listening for HTTP on %s %s (Waiting a first request to start) ...", port, boundedPortsString);
				} else {
					Logger.info("Listening for HTTP at %s on %s %s (Waiting a first request to start) ...", address, port, boundedPortsString);
				}
			} else {
				if (address == null) {
					Logger.info("Listening for HTTP on %s %s ...", port, boundedPortsString);
				} else {
					Logger.info("Listening for HTTP at %s on %s %s  ...", httpPorts, address, port, boundedPortsString);
				}
			}

		}

        bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
        );


		if ((httpsPorts.length > 0) && (httpsPorts[0] != -1)) {
			bootstrap.setPipelineFactory(new SslHttpServerPipelineFactory());
			List<Integer> boundedPorts = new LinkedList<Integer>();
			for (int httpsPort : httpsPorts) {
				try {
					bootstrap.bind(new InetSocketAddress(secureAddress, httpsPort));
					boundedPorts.add(httpsPort);
				} catch (ChannelException e) {
					Logger.error("Could not bind on port " + httpsPort, e);
				}
			}
			bootstrap.setOption("child.tcpNoDelay", true);

			String port = boundedPorts.size() > 1 ? "ports" : "port";
			String boundedPortsString = integerListToCommaSeperatedString(boundedPorts, ", ");

			if (Play.mode == Mode.DEV) {
				if (secureAddress == null) {
					Logger.info("Listening for HTTPS on %s %s (Waiting a first request to start) ...", port, boundedPortsString);
				} else {
					Logger.info("Listening for HTTPS at %s on %s %s (Waiting a first request to start) ...", secureAddress, port, boundedPortsString);
				}
			} else {
				if (secureAddress == null) {
					Logger.info("Listening for HTTPS on %s %s ...", port, boundedPortsString);
				} else {
					Logger.info("Listening for HTTPS at %s on %s %s  ...", httpPorts, secureAddress, port, boundedPortsString);
				}
			}

		}
    }
    
    /**
	 * Generates a String containing the Integers held in the list separated by
	 * whatever separator is past in. For example, for the list {1,2,3} you
	 * could use the separator " -- " and get "1 -- 2 -- 3" returned.
	 * 
	 * @param integerList
	 *            List of Integers that should be used to build the returned
	 *            String
	 * @param separator
	 *            The String that is to be used to separate each Integer in the
	 *            integerList.
	 * @return A String containing the Integers in the integerList separated by
	 *         the separator String. If integerList is null, then an empty
	 *         String will be returned.
	 */
	private String integerListToCommaSeperatedString(List<Integer> integerList, String separator) {
		StringBuffer sb = new StringBuffer();

		if (integerList != null) {
			for (int i = 0; i < integerList.size(); i++) {
				if (i > 0) {
					// not the first element, so add a separator
					sb.append(separator);
				}
				sb.append(integerList.get(i));
			}
		}

		return sb.toString();
	}

	/**
	 * Converts the value of a property into an array of primitive ints. The
	 * property must have a comma separated list of ints. If there is only one
	 * value, and so isn't comma separated then only one value will be returned
	 * in the array. If no property is found then the defaultValue will be used.
	 * 
	 * @param propertyName
	 *            The name of the property to lookup
	 * @param defaultValue
	 *            The value to use if no value for the property can be found.
	 * @return An array of ints that are extracted from the comma separated
	 *         value of the property with name equal to propertyName.
	 */
	private int[] propertyToIntArray(String[] args, String propertyName, int defaultValue) {
		final Properties p = Play.configuration;

		if (propertyName == null) {
			// no property name, so just return a -1 value
			return new int[] { -1 };
		}

		String value = getOpt(args, propertyName, p.getProperty(propertyName, String.valueOf(defaultValue)));

		String[] valueArray = value.split(",");

		int[] intArray = stringArrayToIntArray(valueArray);

		if ((intArray == null) || (intArray.length < 1)) {
			// intArray has not been set, so use the default value
			intArray = new int[] { defaultValue };
		}

		return intArray;
	}

	/**
	 * Converts an array of String objects into an array of ints. If a String
	 * does not parse into an Int then an error is logged and we move onto the
	 * next element in the array.
	 * 
	 * @param stringArray
	 *            The array of strings to parse into Integer objects.
	 * @return An array of itns containing the parsed value of each element in
	 *         the stringArray.
	 */
	private int[] stringArrayToIntArray(String[] stringArray) {
		List<Integer> intList = new LinkedList<Integer>();

		if (stringArray != null) {
			for (String element : stringArray) {
				try {
					Integer intValue = Integer.parseInt(element.trim());
					intList.add(intValue);
				} catch (NumberFormatException e) {
					// failed to parse
					Logger.error("%s is not a valid integer, this will be ignored.", element);
				}
			}
		}

		return toIntArray(intList);
	}

	/**
	 * Converts an array of Integer objects to an array of int primitives
	 * 
	 * @param intList
	 *            List of Integer objects to copy
	 * 
	 * @return Array of int primitives. If intList is null then an empty array
	 *         will be returned.
	 */
	private int[] toIntArray(List<Integer> intList) {

		if (intList == null) {
			return new int[] {};
		}

		int[] intArray = new int[intList.size()];

		for (int i = 0; i < intList.size(); i++) {
			intArray[i] = intList.get(i);
		}

		return intArray;
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

    public static void main(String[] args) throws Exception {
        File root = new File(System.getProperty("application.path"));
        if (System.getProperty("precompiled", "false").equals("true")) {
            Play.usePrecompiled = true;
        }
        Play.init(root, System.getProperty("play.id", ""));
        if (System.getProperty("precompile") == null) {
            new Server(args);
        } else {
            Logger.info("Done.");
        }
    }
}
