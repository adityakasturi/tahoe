import java.net.Inet4Address;

/**
 * fcntcp.java
 * 
 * This is the Main Class file which runs the project -2 of
 * CSCI - 651 - 03
 * 
 * Same program is used to run it for client and server
 * 
 * @author adityakasturi
 *
 */
public class fcntcp {
	/**
	 * 
	 * @param args which passes the type of program client or server
	 * 
	 */
	public static void main(final String[] args) throws Exception {
		/**
		 * The server object which is used to invoke all the Server Methods
		 */
		Server server = null;
		/**
		 * The client Object which are used to send the data .
		 */
		Client client = null;
		/**
		 * 
		 */
		
		String serverIP = args[args.length - 2];
		final String serverIP2 = (Inet4Address.getLocalHost().getHostAddress());
		final int port = Integer.parseInt(args[args.length - 1]);
		/**
		 * variables to run as client 
		 */
		boolean runAsClient = false;
		/**
		 * variables to run as server
		 */
		boolean runAsServer = false;
		/**
		 * 
		 */
		String file = null;
		int timeout = 1000;
		boolean quiet = false;
		String algorithm = "tahoe";
		System.out.println("IP Address - " + serverIP2 );
		if(args[args.length - 2].length()<8){
			serverIP = serverIP2;
		}

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-c")) {
				runAsClient = true;
			} else if (args[i].equals("-s")) {
				runAsServer = true;
			} else if (args[i].equals("-f") || args[i].equals("--file")) {
				file = args[i + 1];
			} else if (args[i].equals("-t") || args[i].equals("--timeout")) {
				timeout = Integer.parseInt(args[i + 1]);
			} else if (args[i].equals("-q") || args[i].equals("--quiet")) {
				quiet = true;
			} else if (args[i].equals("-a") || args[i].equals("--algorithm")) {
				algorithm = args[i + 1];
			}
		}
		if (runAsServer) {
			server = new Server(port, quiet);
			server.startServer();
		}
		/**
		 * If true Initialize all the argument in the server and start it 
		 */
		if (runAsClient) {
			client = new Client(serverIP, port, file, timeout, quiet, algorithm);
			client.startClient();
		}
	}
}