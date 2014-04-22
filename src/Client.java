import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.math.BigInteger;
//import java.util.ListIterator;
import java.net.*;
import java.nio.ByteBuffer;

// The client is the sender in our system
public class Client {

	int mss;
	int windowSize;
	int start;
	int end;
	int requestedPacket;
	long MAX_SEQ_NUMBER; //The maximum sequence number
	String EOF_TOKEN = "EOF";
	LinkedList<Packet> packet_buffer = new LinkedList<Packet>();
	boolean[] timeouts;
	
	DatagramSocket socket = new DatagramSocket();
	String serverName;
	String filename;
	int serverPort;
	
	boolean debugMode;

	//TODO Remove Packet Class.
	/* We don't need it because
	 * sequence_num is the index of the element 
	 * type is constant, 
	 * checksum is calculated on the spot, no need to store it,
	 * */

	private class Packet{

		int sequence_num;
		short checksum;
		short type;
		byte[] data = new byte[mss];//upper limit of data is MSS

		private byte[] getSequenceNum(){
			return ByteBuffer.allocate(4).putInt(this.sequence_num).array();
		}

		private byte[] getChecksum(){
			return ByteBuffer.allocate(2).putShort(this.checksum).array();
		}

		private byte[] getType(){
			return ByteBuffer.allocate(2).putShort(this.type).array();
		}
	}

	private class DataSender implements Runnable {

		public void run(){

			while(true){
				int current = requestedPacket;
				//Just Sliding the window ahead
				if (requestedPacket > start && end < MAX_SEQ_NUMBER) {
					end = end + (requestedPacket - start);
					start = requestedPacket;
				}
				// send all the packets in the window
				try {

					while (current <= end && current <= MAX_SEQ_NUMBER) {

						byte[] buf = new byte[mss + 8];
						
						Packet p = packet_buffer.get(current);
						System.arraycopy(p.getSequenceNum(), 0, buf, 0, 4);
						System.arraycopy(p.getChecksum(), 0, buf, 4, 2);
						System.arraycopy(p.getType(), 0, buf, 6, 2);
						System.arraycopy(p.data, 0, buf, 8, mss);
						//System.out.println(new String(p.data));
						InetAddress address;
						address = InetAddress.getByName(serverName);
						DatagramPacket packet = new DatagramPacket(buf,	buf.length, address, serverPort);
						
						//Do nothing if Not timed out
						if (!timeouts[current % windowSize]){
							continue;
						}
						socket.send(packet);
						
						//Timeout
						timeouts[current % windowSize] = false;
						Timer timer = new Timer();
						
						final int final_current = current;
						timer.schedule(new TimerTask() {
							  @Override
							  public void run() {
							    timeouts[final_current % windowSize] = true;
							    if (final_current >= requestedPacket){
							    	System.out.println("//////////Timeout, sequence# " + final_current);
							    }
							  }
							}, 2*1000);
						
						
						if (debugMode){ System.out.println("SENT  Seq: " + p.sequence_num);}
						
						
						
						current++;
					}
					/*InetAddress address = InetAddress.getByName(serverName);
					if (!address.isReachable(1000)){
						break;
					}*/
					if(requestedPacket > MAX_SEQ_NUMBER){
						break;
					}

					/*
					 * After finishing sending all packets that are in the window
					 * Wait for 1 second, then restart sending from the last requestedPacket
					 * 
					 * This is the actual timeout
					 * This is bad logic. Assumes that time required to send all packets
					 * in the window is insignificant compared to the timeout.
					 */
					//Thread.sleep(500);

				} catch (Exception e) {
					e.printStackTrace(System.out);
				}
			}//while loop
		}//run method
	}//class ends

	private class AckReceiver implements Runnable {
		public void run() {
			while (true) {
				// get ACK
				byte[] buf = new byte[8];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				try {
					//socket.setSoTimeout(500);
					socket.receive(packet);
				} catch (SocketTimeoutException e) {

				} catch (IOException e) {
					e.printStackTrace(System.out);
				}
				// Display response
				int ack = new BigInteger(Arrays.copyOfRange(packet.getData(), 0, 4)).intValue();
				//System.out.println(Arrays.toString(Arrays.copyOfRange(packet.getData(), 4, 6)));
				//System.out.println(Arrays.toString(Arrays.copyOfRange(packet.getData(), 6, 8)));
				if (debugMode){
					System.out.println("ACK received: " + ack);
				}
				requestedPacket = ack;
				if(requestedPacket > MAX_SEQ_NUMBER){
					break;
				}
			}

			//TODO Expire all timers up till ACK

		}

	}

	public Client(int window_size,int mss, String serverName, int serverPort, String filename)	throws Exception {
		start = 0;
		end = window_size - 1;
		this.mss = mss;
		this.windowSize = window_size;
		requestedPacket = 0;
		this.socket = new DatagramSocket();
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.filename = filename;
		timeouts = new boolean[window_size];
		Arrays.fill(timeouts, true);
	}

	public void rdt_send() throws Exception	{
		readFileIntoBuffer(filename);

		DataSender ds = new DataSender();
		Thread senderThread = new Thread(ds);
		senderThread.start();

		AckReceiver ar = new AckReceiver();
		Thread receiverThread = new Thread(ar);
		receiverThread.start();

	}

	private void readFileIntoBuffer(String filename)
			throws FileNotFoundException, IOException {
		File file = new File(filename);
		InputStream file_stream = new FileInputStream(file);
		long filesize =  file.length();
		MAX_SEQ_NUMBER = (filesize / mss);
		byte[] chunk = new byte[mss]; //1 chunk of MSS bytes
		Arrays.fill(chunk, (byte)'\u001a');
		int i = 0;
		while(file_stream.read(chunk) != -1){
			Packet p = new Packet();
			p.sequence_num = i++;
			p.type = 21845;		//Decimal equivalent of 0101010101010101
			p.checksum = 21845;
			p.data = chunk.clone();
			//ByteBuffer.allocate(mss);
			Arrays.fill(chunk, (byte)'\u001a');
			packet_buffer.add(p);
			//TODO Put in the checksum functionality
		}
		//Appending END_OF_FILE to the last packet
		Packet lastPacket = packet_buffer.getLast();
		 
		System.arraycopy(EOF_TOKEN.getBytes(), 0, lastPacket.data, (int) filesize % mss, EOF_TOKEN.length());
		/*System.out.println(new String(EOF_TOKEN.getBytes()) +" "+ new String(lastPacket.data) 
			+ " " + mss +" "+ EOF_TOKEN.length());
		 */
		file_stream.close();
	}

	public static void main(String args[]) throws Exception
	{
		if (args.length != 5) {
			System.out.println("Usage: java Client server-host-name server-port# file-name N MSS");
			return;
		}

		String server = args[0];
		int serverPort = Integer.valueOf(args[1]);
		String filename = ".//files//" + args[2];
		int window_size = Integer.valueOf(args[3]);;
		int mss = Integer.valueOf(args[4]); //bytes

		Client cli = new Client(window_size, mss, server, serverPort, filename);
		cli.debugMode = false;
		System.out.println("Client is up and running!:\n");
		try {
			cli.rdt_send();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		System.out.print("Transmission Complete");
	}


}
