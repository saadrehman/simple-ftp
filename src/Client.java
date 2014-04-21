import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.math.BigInteger;
//import java.util.ListIterator;
import java.net.*;
import java.nio.ByteBuffer;

// The client is the sender in our system
public class Client {

	int mss;
	int window_size;
	int start;
	int end;
	int nextPacket;
	long MAX_SEQ_NUMBER; //The maximum sequence number
	LinkedList<Packet> packet_buffer = new LinkedList<Packet>();
	
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
	

	
	
	public Client(int window_size,int mss)	{
		start = 0;
		end = window_size - 1;
		this.mss = mss;
		nextPacket = 0;
	}
	
	public void rdt_send(String filename, String server_host_name) throws Exception	{
		readFileIntoBuffer(filename);
		
		sendPacket(server_host_name);
	}

	private void readFileIntoBuffer(String filename)
			throws FileNotFoundException, IOException {
		
		InputStream file_stream = new FileInputStream(new File(filename));
		MAX_SEQ_NUMBER = ((new File(filename).length()) / mss);
		byte[] chunk = new byte[mss]; //1 chunk of MSS bytes
		int i = 0;
		while(file_stream.read(chunk) != -1){
			Packet p = new Packet();
			p.sequence_num = i++;
			p.type = 21845;		//Decimal equivalent of 0101010101010101
			p.checksum = 21845;
			p.data = chunk.clone();
			ByteBuffer.allocate(mss);
			Arrays.fill(chunk, (byte)'\u001a');
			packet_buffer.add(p);
			//TODO Put in the checksum functionality
		}
		
		file_stream.close();
	}

	private void sendPacket(String server_host_name) throws SocketException,
			UnknownHostException, IOException {
		while(nextPacket <= end){
			
			//ARQ
			Packet p = packet_buffer.get(nextPacket);
			//Packet p = packet_buffer.
			if(nextPacket > start){
				
				//TODO see if they change in case of timeout
				end = end + (nextPacket - start);
				start = nextPacket;
			}
			
			// get a datagram socket
	        DatagramSocket socket = new DatagramSocket();
	        socket.setSoTimeout(500);
	
	        // send packet
	        byte[] buf = new byte[mss + 8]; 
	        
	        System.arraycopy(p.getSequenceNum(), 0, buf, 0, 4);
	        System.arraycopy(p.getChecksum(), 0, buf, 4, 2);
	        System.arraycopy(p.getType(), 0, buf, 6, 2);
	        System.arraycopy(p.data, 0, buf, 8, mss);
	        
	        InetAddress address = InetAddress.getByName(server_host_name);
	        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 7735);
	        //System.out.println("SENT  Seq: "+p.sequence_num);
	        socket.send(packet);
	    
	        // get ACK
	        packet = new DatagramPacket(buf, buf.length);
	        try {
	        	socket.receive(packet);
	        } catch (SocketTimeoutException e) {
	        	//resend data
	        	//System.out.println("//////////TIMEOUT Seq: " + p.sequence_num +" , Resending.." + p.sequence_num +"../////////////");
	        	//socket.send(packet);
	        	continue;
	        }
	
		    // display response
	        int ack = new BigInteger(Arrays.copyOfRange(packet.getData(), 0, 4)).intValue();
	        //System.out.println(Arrays.toString(Arrays.copyOfRange(packet.getData(), 4, 6)));
	        //System.out.println(Arrays.toString(Arrays.copyOfRange(packet.getData(), 6, 8)));
	        //System.out.println("Next Packet to Send: " + ack);
	        nextPacket = ack;
	        //System.out.println("ACK received: " + ack);
	        socket.close();
	        if (ack > MAX_SEQ_NUMBER){
	        	break;
	        }
		}
	}
	

	public static void main(String args[])
	{
		if (args.length != 5) {
		    System.out.println("Usage: java Client server-host-name server-port# file-name N MSS");
		    return;
		}
		
		String server_host_name = args[0];
		//int server_port = Integer.valueOf(args[1]);
		String filename = ".//files//" + args[2];
		int window_size = Integer.valueOf(args[3]);;
		int mss = Integer.valueOf(args[4]); //bytes
		
		Client cli = new Client(window_size, mss);
		System.out.println("Client is up and running!:\n");
	

		try {
			cli.rdt_send(filename, server_host_name);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
