package edu.wisc.cs.sdn.simpledns;

import edu.wisc.cs.sdn.simpledns.packet.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SimpleDNS 
{
	public static void main(String[] args)
	{
        	System.out.println("Hello, DNS!"); 
		
		String[] Args = checkArgs(args);
		if(Args == null)
			System.exit(0);
		String rootIp = Args[0];
		String csv = Args[1];
		String line;
		String[] entry = new String [2];
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(csv)))) {
    			while ((line = br.readLine()) != null) {	
    				entry = line.split(",");
			}
		} catch( Exception e){
			System.err.println(e);
		}

		try{
			DatagramSocket socket = new DatagramSocket(8053);
			
			byte[] buffer = new byte[4096];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			DNS received = new DNS();
			List<DNSQuestion> questions = new ArrayList<DNSQuestion>();
			InetAddress self = InetAddress.getByName("localhost");
			InetAddress dnsRoot = InetAddress.getByName(rootIp);
			System.out.println(self + "  " + dnsRoot);
	
			while(true){
				socket.receive(packet);
				buffer = packet.getData();
				received = DNS.deserialize(buffer, buffer.length);
				System.out.println("DNS Packet: " + received);
				
				if(received.getOpcode() == 0 && received.isRecursionDesired()){
					questions = received.getQuestions();                                                     
                                	for(DNSQuestion q : questions){                         
                                        	System.out.println("Question: " + q);           
        					if(q.getType() == DNS.TYPE_A || q.getType() == DNS.TYPE_NS || q.getType() == DNS.TYPE_CNAME || q.getType() == DNS.TYPE_AAAA){
                                	
							System.out.println("Packet: " + received);
							


						}
					} 
				}
					
				
			}
		} catch(Exception e){
			System.err.println(e);
		}
		

	}


	public static String[] checkArgs(String[] args) {
        	String[] arguments = new String[2];
                ArrayList<Character> flags = new ArrayList<Character>();

                flags.add('r');
                flags.add('e');
            
                for (int i = 0; i < args.length; i++) {
                        if (args[i].charAt(0) == '-') {
                                if (args[i].length() < 2)
                                        return null;
                                if (!flags.contains(args[i].charAt(1))) {
                                        return null;
                                } else {
                                        switch (args[i].charAt(1)) {
                                        case 'r':
                                                arguments[0] = args[i + 1];
                                                i++;
                                                break;
                                        case 'e':
                                                arguments[1] = args[i + 1];
                                                i++;
                                                break;
                                        default:
                                                return null;
                                        }

                                }
                        } else {
                                return null;
                        }
                }
                return arguments;

	}
}
