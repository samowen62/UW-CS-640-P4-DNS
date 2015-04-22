package edu.wisc.cs.sdn.simpledns.packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DNSRtext implements DNSRdata
{
	private String text;
	
	public DNSRtext()
	{
		this.text = new String();
	}

	public void set(String t)
	{ this.text = t; }
	public DNSRtext(String t)
	{ this.text = t; }

	public int getLength(){
		return this.text.length() + 1;
	}
	
	public byte[] serialize(){
//		return this.text.getBytes();
		byte[] data = new byte[text.length()+ 1];
		ByteBuffer bb = ByteBuffer.wrap(data);
		
		bb.put((byte)(this.text.length()+1));
		bb.put(text.getBytes(StandardCharsets.US_ASCII));		
		return data;
		//return DNS.serializeName(this.text);
	}

	public String toString()
	{ return this.text;	}


}
