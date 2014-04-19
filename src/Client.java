import java.io.*;

import com.sun.java_cup.internal.runtime.Scanner;

// The client is the sender in our system


public class Client {

	int mss;
	int window_size;
	public Client()
	{
	    System.out.println("THis is the sender in our Simple FTP implementation. \n");
	}

	
	// Main method gets user input.
	public static void main(String args[])
	{
		Client cli = new Client();
		//System.out.println("Hi");
		String server_host_name = "127.0.0.1";
		int server_port_number = 7735;
		String filename = ".//files//transfer_me.txt";
		int window_size = 4;
		int mss = 1000;
		cli.window_size = window_size;
		cli.mss = mss;
		//System.out.println("Please enter the filename you want to transfer : \n");
		try {
			rdt_send(filename,server_host_name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void rdt_send(String filename, String server_host_name) throws Exception
	{
		// Reading data from the file
		// InputStream in = new FileInputStream(new File(filename));
		BufferedReader buff = new BufferedReader(new FileReader(filename));
	    String str;
	    while ((str = buff.readLine()) != null)
	        System.out.println(str);
	   buff.close();
	    //in.close
		
	}
}
