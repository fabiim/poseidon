package datastore.column;

import java.io.Serializable;
import java.util.Map;



public interface KeyValueColumnObject<T extends TrackableColumns> {
	
	public Map<String,byte[]> toByteArrayMap(T type); 
	public  T fromByteArrayMap(Map<String,byte[]> fields);
	public <C> C getColumn(String name, byte[] val, Class<C> type);
}


