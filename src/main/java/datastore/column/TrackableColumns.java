package datastore.column;

import java.util.Map;
import java.util.WeakHashMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public interface TrackableColumns {

	/**
	 * Get the column name of the Object reference passed. 
	 * Be aware that this might be the ugliest thing every created.
	 * The objective of this method is to couple object instances that are stored in the KeyValueStore @ref DataStore with the fields that compose the instance.
	 * As such, this method should return the Column in the datastore where we can find the passed referenced object.    
	 * @param ref
	 * @return
	 */
	public String columnName(Object ref); 
}


class ColumnObject implements TrackableColumns{
	BiMap<Object,Column> columns = HashBiMap.create(); 
	
	@Override 
	public String columnName(Object ref){
		Column col = columns.get(ref); 
		if (col != null){
			return col.name; 
		}
		//XXX this may not be optiomal, but in testing it is great to find bugs. Maybe have a configuration file wich sets this behaviour on and off. 
		throw new RuntimeException("Request for object not trackable: " + ref); 
	}
	
	private enum TypeOfColumn{
		SINGULAR, 
		GROUP, 
	}
		
	public  class Column{
		private String name;
		private TypeOfColumn type;
		private Object ref; 
		
		public Column(String name, Object ref){
			this.name = name; 
			this.ref = ref; 
		}
		
		
		
		public String getName(){
			return name; 
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Column other = (Column) obj;
			if (getName() == null) {
				if (other.getName() != null)
					return false;
			} else if (!getName().equals(other.getName()))
				return false;
			return true;
		}
		
		public void setObject(Object ref){
			if (getRef() != ref){
				//Replacing the object associated with this column. 
				columns.forcePut(ref, this);
			}
		}
		
		private Object getRef() {
			return ref; 
		}
		
	}
	
	//This will help implementations of this class with keeping track of changes to the objects. 
	public static class ArrayColumnReference{
		
	}
	
	public static class ObjectColumnReference{
		
	}
	
	
}