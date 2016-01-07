import java.io.Serializable;
/**
 * Packet.java
 * 
 * This class will check implement the Packet which is to be sent in the
 * program which will implement the check sum and the packet.
 * 
 * @author adityakasturi
 *
 */
public class Packet implements Serializable {
	/**
	 * The Parameters of the Packet which is used to send the data 
	 * Again
	 */
	private static final long serialVersionUID = 1L;
	private byte[] packet;
	private int sequenceNumber;
	private boolean lastPacket;
	private String fileName;
	private long checksum;
	private int total;

	public Packet() {
		this.sequenceNumber = 0;
		this.lastPacket = false;
		this.setChecksum(0);
	}

	/**
	 * @return the packet
	 */
	public byte[] getPacket() {
		return this.packet;
	}

	/**
	 * @param packet
	 *            the packet to set
	 */
	public void setPacket(final byte[] packet) {
		this.packet = packet;
	}

	/**
	 * @return the sequenceNumber
	 */
	public int getSequenceNumber() {
		return this.sequenceNumber;
	}

	/**
	 * @param sequenceNumber
	 *            the sequenceNumber to set
	 */
	public void setSequenceNumber(final int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	/**
	 * @return the lastPacket
	 */
	public boolean isLastPacket() {
		return this.lastPacket;
	}

	/**
	 * @param lastPacket
	 *            the lastPacket to set
	 */
	public void setLastPacket(final boolean lastPacket) {
		this.lastPacket = lastPacket;
	}

	/**
	 * @return the checksum
	 */
	public long getChecksum() {
		return this.checksum;
	}

	/**
	 * @param checksum
	 *            the checksum to set
	 */
	public void setChecksum(final long checksum) {
		this.checksum = checksum;
	}

	/**
	 * @return the total
	 */
	public int getTotal() {
		return this.total;
	}

	/**
	 * @param total
	 *            the total to set
	 */
	public void setTotal(final int total) {
		this.total = total;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}