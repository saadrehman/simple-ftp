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
	byte[] allzeroes = {0, 0};
	byte[] type = {10, 10};

	public Server() throws Exception {
		socket = new DatagramSocket(7735);
		expectedPacket = 0;

//		try {
//			in = new BufferedReader(new FileReader("one-liners.txt"));
//		} catch (FileNotFoundException e) {
//			System.err.println("Could not open quote file. Serving time instead.");
//		}[
	}
	
	private boolean matchChecksum(byte[] packet){
		return true;
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
	                
	                /*
	                System.out.println(sequenceNum);
	                System.out.println(checksum);
	                System.out.println(type);
	                System.out.println(data);
	                */
	                	                
	                //If this is the expected packet and the checksum is valid
	                if (sequenceNum  == expectedPacket && matchChecksum(buf)){
	                	//System.out.println("Received: " + data + " Buffer size now: " + receivedData.size());
	                	
	                	if (data.contains("\u001a")){
	                		data = data.replaceAll("\\u001a", "");
	                		//data = data.replace('\u001a', '\0');
	                		keepReceiving = false;
	                	}
	                	receivedData.add(data);
	                	expectedPacket++;
	                }
	                ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
	                outputStream.write( ByteBuffer.allocate(4).putInt(this.expectedPacket).array() );
	                outputStream.write( this.allzeroes );
	                outputStream.write( this.type );

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
		
		if (args.length != 3) {
		    System.out.println("Usage: java Server port# file-name p");
		    return;
		}
		
		Server server = new Server();
		System.out.println("Server is up and running:-\n");
		server.startListening();
		server.writeAllDataToFile(args[1]);
		
	}

	private void writeAllDataToFile(String filename) throws Exception {
		
		String content = "";
		while(!receivedData.isEmpty()){
			content = content + receivedData.pop();
		}
		//System.out.println(content);
		File file = new File(".//" + filename);
		 
		if (!file.exists()) { file.createNewFile();	}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(content);
		bw.close();

		System.out.println("File is written.");
		
	}
	
}
