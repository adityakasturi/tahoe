import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
/**
 * Server.java
 * 
 * This class implements the functionality to receive the files from the client
 * and arrange it in an order to send it to the client and others.
 * 
 * @author adityakasturi
 *
 */
public class Server {
	/**
	 * The port number of the server
	 */
	final private int port;
	/**
	 * variable to display the diagostics
	 */
	final private boolean quiet;
	/**
	 * To Receive the packets in order
	 */
	private Map<Integer, Packet> receivedPackets;
	/**
	 * To receive the packets in order
	 */
	private Map<Integer, Integer> trackPackets;
	
	private String name;

	/**
	 * The Constructor which initializes the port and the print options
	 */
	public Server() {
		this.port = 0;
		this.quiet = false;
	}

	/**
	 * 
	 * Takes in the port number and the print options
	 * 
	 * @param port
	 *            Port number of the server which serves as a Server
	 * @param quiet
	 *            which implements the server
	 * 
	 */
	public Server(final int port, final boolean quiet) {
		this.port = port;
		this.quiet = quiet;
		this.receivedPackets = new TreeMap<Integer, Packet>();
		this.trackPackets = new TreeMap<Integer, Integer>();
	}

	/**
	 *
	 * @throws Exception
	 */
	public void startServer() throws Exception {
		final DatagramSocket socket = new DatagramSocket(this.port);
		final File file ; // the test file

		boolean lastPacket = false;
		boolean receivedAllPackets = false;
		int totalPackets = -1;
		int totalReached = 0;
		/**
		 * Iterate it over the loop until the last packet is checked.
		 * 
		 */
		while (!lastPacket && totalPackets != totalReached) {
			final byte[] packet = new byte[1500]; // packet size
			final DatagramPacket receivePacket = new DatagramPacket(packet, packet.length);
			socket.receive(receivePacket);
			final ByteArrayInputStream bais = new ByteArrayInputStream(packet);
			final ObjectInputStream ois = new ObjectInputStream(bais);
			/**
			 * This is the received packet from the server
			 */
			final Packet receivedPacket = (Packet) ois.readObject();
			/**
			 * Creating all the parameters of the object which is received on
			 * from the client.
			 * Ip Address, port number, Sequence Number and check for the last packet
			 * Calculating the CheckSum 
			 */
			final InetAddress ip = receivePacket.getAddress();
			final int port = receivePacket.getPort();
			final int sequenceNumber = receivedPacket.getSequenceNumber();
			final boolean isLastPacket = receivedPacket.isLastPacket();
			final long receivedPacketChecksum = receivedPacket.getChecksum();
			final byte[] receivedPacketData = receivedPacket.getPacket();
			/**
			 * 
			 * Checks if the received file has got the all the Packets from the 
			 * Client who is sending the packets.
			 * 
			 */
			if (!receivedAllPackets){
				totalPackets = receivedPacket.getTotal();
				receivedAllPackets = true;
			}
			// Calculate checksum
			final Checksum checksum = new CRC32();
			checksum.update(receivedPacketData, 0, receivedPacketData.length);
			final long packetChecksum = checksum.getValue();

			// This checks if received packet's checksum is the same as the
			// packet's checksum
			if (receivedPacketChecksum != packetChecksum) {
				continue;
			}

			if (!this.quiet) {
				System.out.println("Received " + (isLastPacket ? " LAST " : "") + "packet "
						+ (isLastPacket ? "--- " : "") + sequenceNumber);
			}

			// Use this to store all the received packets
			this.receivedPackets.put(sequenceNumber, receivedPacket);

			// Send ack to sender
			this.sendAcknowledgement(sequenceNumber, socket, ip, port);
			if (!this.trackPackets.containsKey(sequenceNumber)) {
				++totalReached;
				this.trackPackets.put(sequenceNumber, totalReached);
			}

			// If last packet then break loop
			if (isLastPacket) {
				lastPacket = true;
				this.name = receivedPacket.getFileName();
			}
		}
		socket.close();
		if (!this.quiet) {
			System.out.println("Packets Received: " + totalReached);
			System.out.println("Number of Packets: " + totalPackets);
		}
		String n = "project2_test-"+name;
		file = new File(n);
		final OutputStream outToFile = new FileOutputStream(file);
		/**
		 * This is to write it to a file
		 */
		for (final Map.Entry<Integer, Packet> entry : this.receivedPackets.entrySet()) {
			outToFile.write(entry.getValue().getPacket());
		}

		/**
		 * Close the output
		 */
		outToFile.close();
		/**
		 * Print all the CheckSum of the file
		 */
		System.out.println("The Checksum is - >  "+Long.toHexString(this.fileChecksum(file)));
		socket.close();
	}
	
	
	/**
	 * This Method will take in the bytes of the file and Calculate the Checksum
	 * of the file
	 * 
	 * @param fileBytes
	 * @return
	 */
	private long calculateFileCheckSum(byte[] fileBytes) {
		// Creating a checksum object to calculate the value
		Checksum cSum = new CRC32();
		// Loading the values to the object and updating
		cSum.update(fileBytes, 0, fileBytes.length);
		// Now getting the value
		return cSum.getValue();
	}
	
	
	/**
	 * 
	 * Calculate the checksum using MD5 Algorithm.
	 * 
	 * @param array
	 * @return
	 */
	public String calculateMD5CheckSum(byte[] array) {
		String checkSum = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(array, 0, array.length);
			byte[] hash = md.digest();
			checkSum = new BigInteger(1, hash).toString();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("No Such MD5 Algorithm exists Error");
			e.printStackTrace();
		}
		return checkSum;
	}

	/**
	 * 
	 * Calculate the checksum using SHA-1 Algorithm
	 * 
	 * @param array
	 * @return
	 */
	public String calculateSHACheckSum(byte[] array) {
		String checkSum = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(array, 0, array.length);
			byte[] hash = md.digest();
			checkSum = new BigInteger(1, hash).toString();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("No Such MD5 Algorithm exists Error");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return checkSum;
	}
	
	/**
	 * Calculates the checksum of the File
	 *
	 * @param file The File Object whose check sum to be calculated
	 * @return
	 * @throws IOException
	 */
	private long fileChecksum(final File file) throws IOException {
		final byte[] fileInBytes = new byte[(int) file.length()];
		final FileInputStream fis = new FileInputStream(file);
		fis.read(fileInBytes);
		fis.close();
		System.out.println("CRC32  -> " + this.calculateFileCheckSum(fileInBytes));
		System.out.println("MD5    -> " + this.calculateMD5CheckSum(fileInBytes));
		System.out.println("SHA-1  -> " + this.calculateSHACheckSum(fileInBytes));		
		final Checksum packetChecksum = new CRC32();
		packetChecksum.update(fileInBytes, 0, fileInBytes.length);
		return packetChecksum.getValue();
	}

	/**
	 * Send Acknowledgement method to the receiver to notify that the 
	 * packet is received.
	 */
	public void sendAcknowledgement(final int sequenceNumber, final DatagramSocket socket, final InetAddress ip, final int port)
			throws Exception {
		final byte[] ack = ByteBuffer.allocate(4).putInt(sequenceNumber).array();
		final DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, ip, port);
		socket.send(ackPacket);
		if (!this.quiet) {
			System.out.println("Sent ACK for packet " + ByteBuffer.wrap(ack).getInt());
		}
	}
}