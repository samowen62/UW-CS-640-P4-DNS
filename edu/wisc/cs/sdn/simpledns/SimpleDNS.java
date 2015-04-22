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
//	public static DatagramSocket finalSocket;
//	public static DatagramPacket send;

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
						InetAddress origAddr = packet.getAddress();
						int origPort = packet.getPort();                                	
						//finalSocket = new DatagramSocket(origPort);						
						DatagramPacket answerPacket = null;

						if(received.isRecursionDesired())
							answerPacket = waitForResponseRecursive(dnsRoot, received);
						else
							answerPacket =  waitForNormalResponse(dnsRoot, received, packet);
						
						answerPacket.setAddress(origAddr);
						answerPacket.setPort(origPort);

						DNS answer = DNS.deserialize(answerPacket.getData(), answerPacket.getData().length);
						System.out.println("Sending Final Packet: "+answer+"\n Port: "+origPort);		
						socket.send(answerPacket);
					}
				} 
			}
		} catch(Exception e){
			System.err.println(e);
		}
		

	}

	public static DatagramPacket waitForResponseRecursive(InetAddress dnsRoot, DNS received){
		
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
		
			String name = "";
			int type = 0;
			if(received.getQuestions() != null){
				name = received.getQuestions().get(0).getName();
				type = received.getQuestions().get(0).getType();
			}

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
				DNSResourceRecord addRecord = null;
				boolean found = false;
				boolean looking = (type == DNS.TYPE_NS);
				short authType = 0;
				short addType = 0;

			//	System.out.println("ans: "+newReceived.getAnswers().size() + " auth: "+newReceived.getAuthorities().size() + "add: "+additional.size());
				if(newReceived.getAnswers().size() == 0){
					for(DNSResourceRecord auth : authorities){
						for(DNSResourceRecord add : additional){
							authType = auth.getType();
							addType = add.getType();
							if(type == DNS.TYPE_NS){
								System.out.println(add.getName()+"  ||||  "+name+ "||| "+add.getName().equals(name));

								addRecord = add;
								if(add.getName().equals(name)){
									found = true;
									looking = false;
									break;
								}
							} else
							if((addType == DNS.TYPE_AAAA || addType == DNS.TYPE_A) && authType == DNS.TYPE_NS){
								found = true;
								addRecord = add;
								break;
							}
						}
						if(found) break;
					}
					//System.out.println(found+" and "+addRecord);
					if(!found && !looking){
						//send = packetSend;
						//return null;
						//DNS retDns = new DNS();
						System.out.println("not found");


					}else{
						if(type == DNS.TYPE_NS && !looking){
							received.setAnswers(newReceived.getAdditional());
                                                	received.setRecursionAvailable(true);
                                                	received.setRecursionDesired(true);
                                                	received.setOpcode((byte)0);
                                                	received.setRcode((byte)0);
                                               	 	received.setAuthoritative(false);
                                                	received.setAuthenicated(false);
                                                	received.setCheckingDisabled(false);
                                                	received.setTruncated(false);
                                                	received.setQuery(false);

                                                	received.setQuestions(received.getQuestions());
                                        		System.out.println("GOING TO SEND NS: "+received);
                                                	DatagramPacket newPacket = new DatagramPacket(received.serialize(),received.getLength());
                                                	return newPacket;

						}
						InetAddress nextAddr = ((DNSRdataAddress)addRecord.getData()).getAddress();
	                                        DatagramPacket nextTry = new DatagramPacket(sendPacket.getData(),sendPacket.getLength(),nextAddr,53);
        	                                System.out.println("sending again on "+nextAddr+"\naddRecord: "+addRecord);
                	                        socket.send(nextTry);
					}
								
				}else{
					
				
					DNSResourceRecord answer = newReceived.getAnswers().get(0);
					DNSQuestion question = newReceived.getQuestions().get(0);

					if(answer.getType() != DNS.TYPE_CNAME){
						done = true;
						//if(question.getType() == DNS.TYPE_A)
							//add .getAnswers() to txt
						received.setAnswers(newReceived.getAnswers());
						received.setRecursionAvailable(true);
						received.setRecursionDesired(true);
						received.setOpcode((byte)0);
						received.setRcode((byte)0);
						received.setAuthoritative(false);
						received.setAuthenicated(false);
						received.setCheckingDisabled(false);
						received.setTruncated(false);
						received.setQuery(false);

						received.setQuestions(received.getQuestions());
					System.out.println("GOING TO SEND: "+received);	
						DatagramPacket newPacket = new DatagramPacket(received.serialize(),received.getLength());
						return newPacket;

					}
					else{
						DNS newQuery = new DNS();
						newQuery.setId(received.getId());
						newQuery.setQuery(true);
						newQuery.setTruncated(false);
						newQuery.setAuthenicated(false);
						newQuery.setRecursionDesired(true);
						newQuery.setOpcode((byte)0);
						
						DNSQuestion query = new DNSQuestion();
						query.setName(((DNSRdataName)answer.getData()).getName());
						query.setType(question.getType());
					
						List<DNSQuestion> newQueryQuest = new ArrayList<DNSQuestion>();
						newQueryQuest.add(query);
						newQuery.setQuestions(newQueryQuest);

						DatagramPacket newQueryPacket = new DatagramPacket(newQuery.serialize(), newQuery.getLength(), dnsRoot, 53);
						socket.send(newQueryPacket);

					}



				}



			}
		}
		catch(Exception e){
			System.out.println("error here:" + e);
		}
		return null;
	}

	public static DatagramPacket waitForNormalResponse(InetAddress dnsRoot, DNS received, DatagramPacket packet){
		try{
			byte[] data = new byte[4096];
			DatagramPacket send = new DatagramPacket(packet.getData(), packet.getLength(), dnsRoot, 53);
			DatagramPacket receive = new DatagramPacket(data, data.length);

			socket.send(send);
			socket.receive(receive);

			return receive;
		}catch(Exception e){
			System.err.println(e);
		}
		return null;
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
