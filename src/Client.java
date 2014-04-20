import java.io.*;
import java.util.LinkedList;
//import java.util.ListIterator;
import java.net.*;






// The client is the sender in our system
public class Client {

	int mss;
	int window_size;
	int window_start;
	int window_end;
	int nextPacket;
	LinkedList<Packet> packet_buffer = new LinkedList<Packet>();
	
	private class Packet{
		int sequence_num;
		short checksum;
		short type;
		byte[] data = new byte[1000];//upper limit of data is MSS
	}
	

	
	
	public Client(int window_size,int mss)	{
		window_start = 0;
		window_end = window_size - 1;
		this.mss = mss;
		nextPacket = 0;
	}
	
	public void rdt_send(String filename, String server_host_name) throws Exception	{
		// Reading data from the file
		InputStream file_stream = new FileInputStream(new File(filename));
		
		//ListIterator<Packet> packet_buffer_iterator = packet_buffer.listIterator();
		
		byte[] chunk = new byte[mss]; //1 chunk of MSS bytes
		int i = 0;
		while(file_stream.read(chunk) != -1){
			Packet p = new Packet();
			p.sequence_num = i++;
			p.type = 21845;		//Decimal equivalent of 0101010101010101
			p.checksum = 21845;
			p.data = chunk;
			packet_buffer.add(p);
			//TODO Put in the checksum functionality
		}
		file_stream.close();
		
		////////////////////////////////////////////////////////////////////////////s
		//Reading the file into packet_buffer complete now. 
		// Sending packet by packet not starting. 
		
		//int port;
		//InetAddress address;
		//DatagramSocket socket = null;
		//DatagramPacket packet;
		//byte[] sendBuf = new byte[256];
		
		Packet p = packet_buffer.pop();
		
	       // get a datagram socket
        DatagramSocket socket = new DatagramSocket();

            // send request
        byte[] buf = new byte[mss + 64]; 
        buf = p.data;
        InetAddress address = InetAddress.getByName(server_host_name);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 7735);
        socket.send(packet);
    
            // get response
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

	    // display response
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Next Packet to Send: " + received);
    
        socket.close();

		
	}
	

	public static void main(String args[])
	{
		if (args.length != 5) {
		    System.out.println("Usage: java Client server-host-name server-port# file-name N MSS");
		    return;
		}
		
		String server_host_name = args[0];
		int server_port = Integer.valueOf(args[1]);
		String filename = ".//files//" + args[2];
		int window_size = Integer.valueOf(args[3]);;
		int mss = Integer.valueOf(args[4]); //bytes
		
		Client cli = new Client(window_size, mss);
		System.out.println("Client is up and running!:\n");
	

		try {
			cli.rdt_send(filename,server_host_name);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
