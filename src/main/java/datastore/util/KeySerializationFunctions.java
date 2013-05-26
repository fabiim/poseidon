package datastore.util;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.EnumSet;

import net.floodlightcontroller.devicemanager.IDeviceService.DeviceField;
import net.floodlightcontroller.devicemanager.internal.Entity;
import net.floodlightcontroller.devicemanager.internal.IndexedEntity;
import net.floodlightcontroller.topology.NodePortTuple;

import org.python.google.common.base.Function;

import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;



public class KeySerializationFunctions {
	
	public static final Function<Long, byte[]> LONG_SERIALIZE = 
			new Function<Long,byte[]>(){
		
	public byte[] apply(Long c){
		return Longs.toByteArray(c);
	}
}; 

public static final Function<byte[] ,Long> LONG_DESERIALIZE =
			new Function<byte[] ,Long>(){
	public Long apply(byte[]  c){
		return Longs.fromByteArray(c);
	}
}; 
public static final Function<Short, byte[]> SHORT_SERIALIZE = 
new Function<Short,byte[]>(){

public byte[] apply(Short c){
return Shorts.toByteArray(c);
}
}; 

public static final Function<byte[] ,Short> SHORT_DESERIALIZE =
new Function<byte[] ,Short>(){
public Short apply(byte[]  c){
return Shorts.fromByteArray(c);
}
}; 

	public static final Function<String, byte[]> STRING_SERIALIZE = 
			new Function<String ,byte[] >(){
				public byte[]  apply(String  c){
					return c.getBytes(); 
				}
	}; 
	
	public static final Function<byte[],String> STRING_DESERIALIZE = 
			new Function<byte[] ,String>(){
				public String  apply(byte[]  c){
					return new String(c); 
				}
		};
		

	public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
	        return b.toByteArray();
	}
		
    public  static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
	        return o.readObject();
	}

    public static final Function<NodePortTuple, byte[] > NODE_PORT_TUPLE_SERIALIZE = 
			new Function<NodePortTuple, byte[] >(){
				
			public byte[]  apply (NodePortTuple c){
				try {
					return KeySerializationFunctions.serialize(c);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null; 
		}
}; 

public static final Function<byte[] ,NodePortTuple> NODE_PORT_TUPLE_DESERIALIZE =
			new Function<byte[] ,NodePortTuple>(){
	public NodePortTuple apply (byte[]  c){
		try {
			return (NodePortTuple) KeySerializationFunctions.deserialize(c);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null; 
	}
};
public static final Function<IndexedEntity, byte[] > INDEXED_ENTITY_SERIALIZE = 
		new Function<IndexedEntity, byte[] >(){
			
		public byte[]  apply (IndexedEntity c){
			try {
		        ByteArrayOutputStream b = new ByteArrayOutputStream();
		        ObjectOutputStream out = new ObjectOutputStream(b);
		
				out.writeObject(c.getKeyFields());
				for (DeviceField f : c.getKeyFields()){
					
					switch(f){
					case IPV4:
						out.writeObject(c.getEntity().getIpv4Address());
						break;
					case MAC:
						out.writeObject(new Long(c.getEntity().getMacAddress()));
						break;
					case PORT:
						out.writeObject(c.getEntity().getSwitchPort());
						break;
					case SWITCH:
						out.writeObject(c.getEntity().getSwitchDPID());
						break;
					case VLAN:
						out.writeObject(c.getEntity().getVlan());
						break;
					}
					
				}
				out.flush(); 
				out.close();
				return b.toByteArray(); 
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null; 
	}
}; 

public static final Function<byte[] ,IndexedEntity> INDEXED_ENTITY_DESERIALIZE =
		new Function<byte[] ,IndexedEntity>(){

	public IndexedEntity apply (byte[]  c){
		try {
        ByteArrayInputStream b = new ByteArrayInputStream(c);
        ObjectInputStream o = new ObjectInputStream(b);
		EnumSet<DeviceField> fields = (EnumSet<DeviceField>) o.readObject();
		IndexedEntity ie = new IndexedEntity();
		ie.setKeyFields(fields);
		Long macAddress=null;  
		Short vlan =null;
        Integer ipv4Address=null; 
		Long switchDPID = null; 
		Integer switchPort = null; 
        Date lastSeenTimestamp =null;
		for (DeviceField f: fields){
			switch(f){
			case IPV4:
				ipv4Address = (Integer) o.readObject(); 
				break;
			case MAC:
				macAddress = (Long) o.readObject(); 
				break;
			case PORT:
				switchPort = (Integer) o.readObject(); 
				break;
			case SWITCH:
				switchDPID = (Long) o.readObject(); 
				break;
			case VLAN:
				vlan = (Short) o.readObject(); 
				break;
			
			}
		}
		Entity e = new Entity(macAddress, vlan, ipv4Address, switchDPID, switchPort, lastSeenTimestamp);
		ie.setEntity(e);
		return ie; 
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return null; 
}
}; 
	
	
}
