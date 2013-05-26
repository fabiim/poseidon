package net.floodlightcontroller.topology;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Date;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import datastore.Table;

public class TopologyInstanceWrapper {

	TopologyInstance c ; 
	String hash; 
	Date lastUpdateTime; 
	Table<String,String> table;
	public TopologyInstanceWrapper(TopologyInstance c){
		init(c);
	}

	public TopologyInstanceWrapper(Table<String,String> controllersTable) {
		table = controllersTable; 
	}

	
	private void init(TopologyInstance c){
		this.c = c; 
		lastUpdateTime = new Date(); 
		hash = hash(c);
		table.put("currentHash", hash);
	}
	
	public synchronized boolean isUpdated(){
		return hash.equals(table.get("currentHash")); 
	}
		
	public synchronized void replace(TopologyInstance c){
		init(c); 
	}
	
	public synchronized TopologyInstance getC() {
		return c;
	}

	public synchronized String getHash() {
		return hash;
	}

	public synchronized Date getLastUpdateTime() {
		return lastUpdateTime;
	}
	
	
	private String hash(TopologyInstance nt) {
    	try {
    		
			MessageDigest mda = MessageDigest.getInstance("SHA");
			ByteOutputStream stream = new ByteOutputStream();
			ObjectOutputStream st = new ObjectOutputStream(stream);
			st.flush(); 
			st.writeObject(nt);
			stream.close(); 
			st.close();
			
			StringBuffer hexString = new StringBuffer();
			byte[] hash = mda.digest(stream.getBytes());

			for (int i = 0; i < hash.length; i++) {
				if ((0xff & hash[i]) < 0x10) {
					hexString.append("0"
							+ Integer.toHexString((0xFF & hash[i])));
				} else {
			                hexString.append(Integer.toHexString(0xFF & hash[i]));
			    }
			}
			return hexString.toString(); 
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null; //fixme
	}

}
