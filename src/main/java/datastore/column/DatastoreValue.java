package datastore.column;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

public interface DatastoreValue extends Serializable {
	
	public <T extends Serializable> Map<String, Pair<Class<T>, T>> getColumns(); 
	
}
