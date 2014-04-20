import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;



public class Server {
	
	protected DatagramSocket socket = null;
	protected BufferedReader in = null;
	protected int mss = 8;
	protected boolean keepReceiving = true;
	protected int expectedPacket;
	LinkedList<String> receivedData = new LinkedList<String>();
	short type = (short) 43690;

	public Server() throws Exception {
		socket = new DatagramSocket(7735);
		expectedPacket = 0;

//		try {
//			in = new BufferedReader(new FileReader("one-liners.txt"));
//		} catch (FileNotFoundException e) {
//			System.err.println("Could not open quote file. Serving time instead.");
//		}
	}
	
	public void startListening() {
		 while (keepReceiving) {
	            try {
	                byte[] buf = new byte[mss + 8];

	                // receive packet
	                DatagramPacket packet = new DatagramPacket(buf, buf.length);
	                socket.receive(packet);
	                
	                int sequenceNum = new BigInteger(Arrays.copyOfRange(buf, 0, 4)).intValue();
	                short checksum = new BigInteger(Arrays.copyOfRange(buf, 4, 6)).shortValue();
	                short type = new BigInteger(Arrays.copyOfRange(buf, 6, 8)).shortValue();
	                String data = new String(Arrays.copyOfRange(buf, 8, 8 + mss));
	                
	                System.out.println(sequenceNum);
	                System.out.println(checksum);
	                System.out.println(type);
	                System.out.println(data);

	                //if this is the expected packet
	                if (sequenceNum  == expectedPacket){
	                	receivedData.add(data);
	                	expectedPacket++;
	                }
	                short allZeroes = 0;
	                ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
	                outputStream.write( ByteBuffer.allocate(4).putInt(this.expectedPacket).array() );
	                outputStream.write( ByteBuffer.allocate(2).putShort(allZeroes).array() );
	                outputStream.write( ByteBuffer.allocate(2).putShort(this.type).array() );

	                buf = outputStream.toByteArray( );

	                // send the response to the client at "address" and "port"
	                InetAddress address = packet.getAddress();
	                int port = packet.getPort();
	                packet = new DatagramPacket(buf, buf.length, address, port);
	                socket.send(packet);
	            } catch (IOException e) {
	                e.printStackTrace();
			keepReceiving = false;
	            }
	        }
	        socket.close();
	}
	
	
	
	public static void main(String args[]) throws Exception	{
		Server server = new Server();
		System.out.println("Server is up and running:-\n");
		server.startListening();
		
	}
	
}
