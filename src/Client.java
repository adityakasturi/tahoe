import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
/**
 * Client.java
 * 
 * The Client of the class which is used to send the data to the server 
 * By connecting it on the IP and Source.
 * The Communication is done using UDP
 * 
 * TCP TAHOE and RENO are Implemented here
 * 
 * @author adityakasturi
 *
 */
public class Client {
	/**
	 * The Parameters of the Client to connect to the server
	 */
	final private String serverName;
	final private InetAddress _IPAddress;
	final private int portNumber;
    final private String fileName;
	/**
	 * The server timeout option which is set on the socket inorder to 
	 * set the Socket Timeout Option
	 */
	final private int timeout;
	/**
	 * This is used to keep the client quiet
	 */
	final private boolean quiet;
	/**
	 * This is to set the Algorithm 
	 */
	final private String algorithm;
	/**
	 * the Packet Sequence of the packets which are sent
	 * 
	 */
	private Map<Integer, Boolean> acknowledgedPackets;
	private Map<Integer, Packet> packetSequence;
	/**
	 * To keep the track of three ACKS
	 */
	private int threeAcks;

		
	
	public Client() {
		this.serverName = null;
		this._IPAddress = null;
		this.portNumber = 0;
		this.fileName = null;
		this.timeout = 0;
		this.quiet = false;
		this.algorithm = "";
		this.threeAcks = 0;
	}

	/**
	 * Parameterized Constructor
	 *
	 * @param hostname
	 * @param portNumber
	 * @param fileName
	 * @param timeout
	 * @param quiet
	 * @throws UnknownHostException
	 */
	public Client(final String hostname, final int port, final String fileName, final int timeout,
			final boolean quiet, final String algorithm) throws UnknownHostException {
		this.serverName = hostname;
		this._IPAddress = InetAddress.getByName(this.serverName);
		this.portNumber = port;
		this.fileName = fileName;
		this.timeout = timeout;
		this.quiet = quiet;
		this.acknowledgedPackets = new HashMap<Integer, Boolean>();
		this.packetSequence = new HashMap<Integer, Packet>();
		this.algorithm = algorithm;
		this.threeAcks = 0;
	}

	/**
	 * Starts the RDT sender
	 *
	 * @throws Exception
	 */
	public void startClient() throws Exception {

		// Creates the socket
		final DatagramSocket socket = new DatagramSocket();

		// Reads file and copies to fileInBytes using FileInputStream
		final File file = new File(this.fileName);
		final byte[] fileInBytes = new byte[(int) file.length()];
		final FileInputStream fis = new FileInputStream(file);
		fis.read(fileInBytes);

		final double fileSize = fileInBytes.length;
		final double packetSize = 1024;
		final int totalPackets = (int) Math.ceil(fileSize / packetSize);

		if (!this.quiet) {
			System.out.println("Total Packets: " + totalPackets);
		}
		// Calculates Checksum
		fis.close();

		// This is the SSThreshold - I have set it to 100
		float ssthresh = 100;

		/**
		 *  The congestion window of the client which is sending the packet
		 */
		float cwnd = 1;
		int counter = 0;
		
		/**
		 * The sendPackets is used to keep the track of the send Packets
		 */
		final Map<Packet, DatagramPacket> sendPackets = new LinkedHashMap<Packet, DatagramPacket>();
		final Map<Packet, DatagramPacket> tempStoredPacket = new HashMap<Packet, DatagramPacket>();
		
		/**
		 * Send the packets until the last packet is adjusted with the data
		 * iterates with the data
		 * 
		 */
		for (int i = 0, sequenceNumber = 0; i < fileInBytes.length; i = i + 1024, sequenceNumber++) {
			final Packet packet = new Packet();
			/**
			 * Calculating the total number of packets and adding it 
			 */
			packet.setTotal(totalPackets);
			/**
			 * Adding the sequence number of the packet to the Packet Object
			 */
			packet.setSequenceNumber(sequenceNumber);
			/**
			 * This is the logic to track the last packet of the File
			 * 
			 */
			if (i + 1024 < fileInBytes.length) {
				packet.setLastPacket(false);
			} else if (i + 1024 >= fileInBytes.length) {
				packet.setLastPacket(true);
				packet.setFileName(fileName);
			}

			final boolean isLastPacket = packet.isLastPacket();
			if (!isLastPacket) {
				final byte[] tempPacket = new byte[1024];
				for (int j = 0; j < 1024; j++) {
					tempPacket[j] = fileInBytes[i + j];
				}
				packet.setPacket(tempPacket);
				/**
				 * Calculate the checksum of the packet and add it to the
				 * packet Object
				 */
				final Checksum packetChecksum = new CRC32();
				packetChecksum.update(tempPacket, 0, tempPacket.length);
				packet.setChecksum(packetChecksum.getValue());

			} else if (isLastPacket) {
				final byte[] lastPacket = new byte[fileInBytes.length - i];
				for (int j = 0; j < fileInBytes.length - i; j++) {
					lastPacket[j] = fileInBytes[i + j];
				}
				packet.setPacket(lastPacket);
				final Checksum packetChecksum = new CRC32();
				packetChecksum.update(lastPacket, 0, lastPacket.length);
				packet.setChecksum(packetChecksum.getValue());
			}

			// This is used to send the packet using BAOS object. I am sending
			// the packet object
			this.packetSequence.put(sequenceNumber, packet);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(packet);
			oos.flush();
			oos.close();
			final byte[] packetInBytes = baos.toByteArray();
			final DatagramPacket sendPacket = new DatagramPacket(packetInBytes, packetInBytes.length, this._IPAddress,
					this.portNumber);

			if (!this.quiet) {
				// printFile(packet.getPacket());
			}

			if (this.algorithm.equals("tahoe")) {
				// Sliding window protocol implementation
				if (counter < cwnd) {
					if (!this.quiet) {
						System.out.println(
								"Checksum for packet " + packet.getSequenceNumber() + ": " + packet.getChecksum());
					}

					// Copy the contents of tempStoredPacket to sendPackets
					if (tempStoredPacket.size() != 0) {
						for (final Map.Entry<Packet, DatagramPacket> entry : tempStoredPacket.entrySet()) {
							sendPackets.put(entry.getKey(), entry.getValue());
						}
						counter++;
						tempStoredPacket.clear();
					}

					// Store each window's packet inside the sendPackets map
					sendPackets.put(packet, sendPacket);
					counter++;

					// Sequence numbers of window packets
					if (!this.quiet) {
						for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
							System.out.println("Sequence: " + entry.getKey().getSequenceNumber());
						}
					}

					// Sending last packets
					if (packet.isLastPacket()) {

						new Thread() {
							@Override
							public void run() {
								// Send packets
								for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
									try {
										socket.send(entry.getValue());
									} catch (final Exception e) {
										e.printStackTrace();
									}

									if (!Client.this.quiet) {
										System.out.println("Sent packet " + entry.getKey().getSequenceNumber());
									}
								}
							}
						}.start();

						final boolean ackRecievedCorrect = false;

						// Wait for acks
						for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
							while (!ackRecievedCorrect) {
								final int ackNumber = entry.getKey().getSequenceNumber();
								this.receiveAcknowledgement(ackNumber, socket, this.algorithm);

								if (this.acknowledgedPackets.get(ackNumber) == null) {
									final DatagramPacket resendPacket = entry.getValue();
									socket.send(resendPacket);
									System.out.println("Resending: Packet " + ackNumber);
								} else {
									break;
								}
							}
						}
					}
				} else {

					new Thread() {
						@Override
						public void run() {
							// Send packets
							for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
								try {
									socket.send(entry.getValue());
								} catch (final Exception e) {
									e.printStackTrace();
								}

								if (!Client.this.quiet) {
									System.out.println("Sent " + (isLastPacket ? "last " : "") + "packet "
											+ (isLastPacket ? "==> " : "") + entry.getKey().getSequenceNumber());
								}
							}
						}
					}.start();

					final boolean ackRecievedCorrect = false;

					for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {

						// Will run until the ack is received
						while (!ackRecievedCorrect) {
							final int ackNumber = entry.getKey().getSequenceNumber();

							if (!this.quiet) {
								System.out.println("acknumber is:" + ackNumber);
							}
							final boolean ack = this.receiveAcknowledgement(ackNumber, socket, this.algorithm);

							if (ack) {
								if (cwnd < ssthresh) {
									cwnd += 1;
								} else {
									cwnd += 1 / cwnd;
								}
							} else {
								ssthresh = cwnd / 2;
								cwnd = 1;
							}
							if (this.acknowledgedPackets.get(ackNumber) == null) {
								final DatagramPacket resendPacket = entry.getValue();
								socket.send(resendPacket);
								System.out.println("Resending: Packet " + ackNumber);
							} else {
								break;
							}
						}
					}

					if (!this.quiet) {
						System.out.println();
						System.out.println();
					}

					sendPackets.clear();
					tempStoredPacket.put(packet, sendPacket);
					counter = 0;
				}
			} else if (this.algorithm.equals("reno")) {

				// Sliding window protocol implementation
				if (counter < cwnd) {
					if (!this.quiet) {
						System.out.println(
								"Checksum for packet " + packet.getSequenceNumber() + ": " + packet.getChecksum());
					}

					// Copy the contents of tempStoredPacket to sendPackets
					if (tempStoredPacket.size() != 0) {
						for (final Map.Entry<Packet, DatagramPacket> entry : tempStoredPacket.entrySet()) {
							sendPackets.put(entry.getKey(), entry.getValue());
						}
						counter++;
						tempStoredPacket.clear();
					}

					// Store each window's packet inside the sendPackets map
					sendPackets.put(packet, sendPacket);
					counter++;

					// Sequence numbers of window packets
					if (!this.quiet) {
						for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
							System.out.println("Sequence: " + entry.getKey().getSequenceNumber());
						}
					}

					// Sending last packets
					if (packet.isLastPacket()) {

						new Thread() {
							@Override
							public void run() {
								// Send packets
								for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
									try {
										socket.send(entry.getValue());
									} catch (final IOException e) {
										e.printStackTrace();
									}

									if (!Client.this.quiet) {
										System.out.println("Sent packet " + entry.getKey().getSequenceNumber());
									}
								}
							}
						}.start();

						final boolean ackRecievedCorrect = false;

						// Wait for acks
						for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
							while (!ackRecievedCorrect) {
								final int ackNumber = entry.getKey().getSequenceNumber();
								this.receiveAcknowledgement(ackNumber, socket, this.algorithm);

								if (this.acknowledgedPackets.get(ackNumber) == null) {
									final DatagramPacket resendPacket = entry.getValue();
									socket.send(resendPacket);
									System.out.println("Resending: Packet " + ackNumber);
								} else {
									break;
								}
							}
						}
					}
				} else {

					new Thread() {
						@Override
						public void run() {
							// Send packets
							for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
								try {
									socket.send(entry.getValue());
								} catch (final IOException e) {
									e.printStackTrace();
								}

								if (!Client.this.quiet) {
									System.out.println("Sent " + (isLastPacket ? "last " : "") + "packet "
											+ (isLastPacket ? "==> " : "") + entry.getKey().getSequenceNumber());
								}
							}
						}
					}.start();

					final boolean ackRecievedCorrect = false;

					for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {

						// Will run until the ack is received
						while (!ackRecievedCorrect) {
							final int ackNumber = entry.getKey().getSequenceNumber();

							if (!this.quiet) {
								System.out.println("acknumber is:" + ackNumber);
							}
							final boolean ack = this.receiveAcknowledgement(ackNumber, socket, this.algorithm);

							if (ack) {
								if (cwnd < ssthresh) {
									cwnd += 1;
								} else {
									cwnd += 1 / cwnd;
								}
							} else {
								cwnd = cwnd / 2;
								ssthresh = cwnd;
							}
							if (this.acknowledgedPackets.get(ackNumber) == null || !ack) {
								final DatagramPacket resendPacket = entry.getValue();
								socket.send(resendPacket);
								System.out.println("Resending: Packet " + ackNumber);
							} else {
								break;
							}
						}
					}

					if (!this.quiet) {
						System.out.println();
						System.out.println();
					}

					sendPackets.clear();

					if (!this.quiet) {
						System.out.println("Checksum for stored packet " + packet.getSequenceNumber() + ": "
								+ packet.getChecksum());
					}

					tempStoredPacket.put(packet, sendPacket);
					counter = 0;
				}

			} else if (this.algorithm.equals("custom")) {
				// Sliding window protocol implementation
				if (counter < cwnd) {
					if (!this.quiet) {
						System.out.println(
								"Checksum for packet " + packet.getSequenceNumber() + ": " + packet.getChecksum());
					}

					if (tempStoredPacket.size() != 0) {
						for (final Map.Entry<Packet, DatagramPacket> entry : tempStoredPacket.entrySet()) {
							sendPackets.put(entry.getKey(), entry.getValue());
						}
						counter++;
						tempStoredPacket.clear();
					}
					sendPackets.put(packet, sendPacket);
					counter++;
					if (!this.quiet) {
						for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
							System.out.println("Sequence: " + entry.getKey().getSequenceNumber());
						}
					}

					if (packet.isLastPacket()) {

						new Thread() {
							@Override
							public void run() {
								for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
									try {
										socket.send(entry.getValue());
									} catch (final IOException e) {
										e.printStackTrace();
									}

									if (!Client.this.quiet) {
										System.out.println("Sent " + (isLastPacket ? "last " : "") + "packet "
												+ (isLastPacket ? "==> " : "") + entry.getKey().getSequenceNumber());
									}
								}
							}
						}.start();

						final boolean ackRecievedCorrect = false;

						for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
							while (!ackRecievedCorrect) {
								final int ackNumber = entry.getKey().getSequenceNumber();
								this.receiveAcknowledgement(ackNumber, socket, this.algorithm);
								if (this.acknowledgedPackets.get(ackNumber)) {
									break;
								} else {
									final DatagramPacket resendPacket = entry.getValue();
									socket.send(resendPacket);
									if (!this.quiet) {
										System.out.println("Resending: Packet " + ackNumber);
									}
								}
							}
						}
					}
				}

				// This is used to send and receive acks
				else {
					for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
						socket.send(entry.getValue());

						if (!this.quiet) {
							System.out.println("Sent " + (isLastPacket ? "last " : "") + "packet "
									+ (isLastPacket ? "==> " : "") + entry.getKey().getSequenceNumber());
						}
					}

					final boolean ackRecievedCorrect = false;
					for (final Map.Entry<Packet, DatagramPacket> entry : sendPackets.entrySet()) {
						while (!ackRecievedCorrect) {
							final int ackNumber = entry.getKey().getSequenceNumber();
							this.receiveAcknowledgement(ackNumber, socket, this.algorithm);

							if (this.acknowledgedPackets.get(ackNumber) == null) {
								final DatagramPacket resendPacket = entry.getValue();
								socket.send(resendPacket);
								System.out.println("Resending: Packet " + ackNumber);
							} else {
								break;
							}

						}
					}

					if (!this.quiet) {
						System.out.println();
						System.out.println();
					}

					sendPackets.clear();

					if (!this.quiet) {
						System.out.println("Checksum for stored packet " + packet.getSequenceNumber() + ": "
								+ packet.getChecksum());
					}

					tempStoredPacket.put(packet, sendPacket);
					counter = 0;

					if (cwnd < ssthresh) {
						cwnd *= 2;
					} else {
						cwnd = 1;
						ssthresh /= 2;
					}
				}
			} else {
				
			}
		}
		long l = fileChecksum(fileInBytes);
		System.out.println("Checksum of file  " + Long.toHexString(l));
		
		
		socket.close();
	}

	/**
	 * Checksum calculation
	 *
	 * @param fileInBytes
	 * @return
	 */
	private long fileChecksum(final byte[] fileInBytes) {
		final Checksum packetChecksum = new CRC32();
		System.out.println("CRC32  -> " + this.calculateFileCheckSum(fileInBytes));
		System.out.println("MD5    -> " + this.calculateMD5CheckSum(fileInBytes));
		System.out.println("SHA-1  -> " + this.calculateSHACheckSum(fileInBytes));	
		packetChecksum.update(fileInBytes, 0, fileInBytes.length);
		return packetChecksum.getValue();
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
	 * Receive the ACK
	 *
	 * @param socket
	 * @param algorithm
	 */
	public boolean receiveAcknowledgement(final int sequenceNumber, final DatagramSocket socket, final String algorithm) {
		final byte[] ack = new byte[4];
		final DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
		try {
			socket.setSoTimeout(this.timeout);
			socket.receive(ackPacket);
			final int ackSequenceNumber = ByteBuffer.wrap(ackPacket.getData()).getInt();

			if (!this.quiet) {
				System.out.println("ACK Received #" + ackSequenceNumber);
			}

			if (this.acknowledgedPackets.containsKey(sequenceNumber)) {
				if (!this.quiet) {
					System.out.println("Duplicate received ACK for packet " + ackSequenceNumber);
				}
				if (algorithm.equals("reno")) {
					this.threeAcks++;
					if (this.threeAcks == 3) {
						if (!this.quiet) {
							System.out.println("Three duplicate received ACK for packet " + ackSequenceNumber);
						}
						this.threeAcks = 0;
						return false;
					}
				}
				return false;
			} else {
				// Map to store the ackPackets
				if (!this.quiet) {
					System.out.println("Acknowledgment Received for -> " + ackSequenceNumber);
				}
				this.acknowledgedPackets.put(ackSequenceNumber, true);
				return true;

			}
		} catch (final Exception e) {
			if (!this.quiet) {
				System.out.println("!!!Received ACK for Packet " + sequenceNumber);
			}
			return false;
		}
	}
}
