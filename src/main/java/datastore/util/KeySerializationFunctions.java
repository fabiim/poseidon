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



import com.google.common.base.Function;
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

public static final com.google.common.base.Function<byte[], Short> SHORT_DESERIALIZE =
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

	
	
}
