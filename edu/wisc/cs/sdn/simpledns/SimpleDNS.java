package edu.wisc.cs.sdn.simpledns;

import edu.wisc.cs.sdn.simpledns.packet.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SimpleDNS 
{
	public static DatagramSocket socket;
	public static DatagramPacket send;

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
			socket = new DatagramSocket(8053);
			
			byte[] buffer = new byte[4096];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			
			DNS received = new DNS();

			List<DNSQuestion> questions = new ArrayList<DNSQuestion>();
			InetAddress self = InetAddress.getByName("localhost");
			InetAddress dnsRoot = InetAddress.getByName(rootIp);
	
			socket.receive(packet);
			received = DNS.deserialize(packet.getData(), packet.getLength());
			System.out.println("DNS Packet: " + received);
			
			if(received.getOpcode() == 0){
				questions = received.getQuestions();                                                     
                               	for(DNSQuestion q : questions){                         
                                        System.out.println("Question: " + q);           
        				if(q.getType() == DNS.TYPE_A || q.getType() == DNS.TYPE_NS || q.getType() == DNS.TYPE_CNAME || q.getType() == DNS.TYPE_AAAA){
                                	
						if(received.isRecursionDesired())
							waitForResponseRecursive(dnsRoot, received);
						//else
						//	waitForNormalResponse(dnsRoot, received);
					}
				} 
			}
		} catch(Exception e){
			System.err.println(e);
		}
		

	}

	public static void waitForResponseRecursive(InetAddress dnsRoot, DNS received){
		
		try{
			DNS queryReceived = new DNS();
			DNS newReceived = new DNS();
			queryReceived.setQuestions(received.getQuestions());			
			queryReceived.setQuery(true);
			queryReceived.setTruncated(false);
			queryReceived.setAuthenicated(false);
			queryReceived.setRecursionDesired(true);
			queryReceived.setOpcode((byte)0);

			boolean done = false;	
			byte[] bufferSend = new byte[4096];

	                List<DNSQuestion> questions = new ArrayList<DNSQuestion>();
			List<DNSResourceRecord> addrs = new ArrayList<DNSResourceRecord>();
			List<DNSResourceRecord> additional;
                        List<DNSResourceRecord> authorities;

			byte[] serRec = queryReceived.serialize();
                	DatagramPacket sendPacket = new DatagramPacket(serRec, serRec.length, dnsRoot, 53);
                	DatagramPacket packetSend = null;

			socket.send(sendPacket);
			System.out.println("sent that shit on" + dnsRoot);
		
			while(!done){
				packetSend = new DatagramPacket(bufferSend, bufferSend.length);			
	
				socket.receive(packetSend);
                		bufferSend = packetSend.getData();
                		newReceived = DNS.deserialize(bufferSend, bufferSend.length);
                		System.out.println("QUERY RESPONSE: "+ newReceived);
				
				additional = newReceived.getAdditional();
				authorities = newReceived.getAuthorities();
				boolean found = false;
				short authType = 0;
				short addType = 0;

				if(newReceived.getAnswers().size() == 0){
					for(DNSResourceRecord auth : authorities){
						for(DNSResourceRecord add : additional){
							authType = auth.getType();
							addType = add.getType();
							if((addType == DNS.TYPE_AAAA || addType == DNS.TYPE_A) && authType == DNS.TYPE_NS){
								found = true;
								break;
							}
						}
					}
					if(!found){
						send = packetSend;
						System.out.println("Ye a gay");
						return;
					}

				}else{



				}



			}
		}
		catch(Exception e){
			System.out.println("error here:" + e);
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
