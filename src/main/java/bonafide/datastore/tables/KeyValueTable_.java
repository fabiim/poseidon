/**
 * 
 */
package bonafide.datastore.tables;

import java.util.Collection;

import com.google.common.collect.Lists;

import bonafide.datastore.KeyValueDatastoreProxy;
import bonafide.datastore.util.Serializer;

/**
 * @author fabiim
 *
 */
public class KeyValueTable_<K, V> extends AbstractTable<K,V> implements KeyValueTable<K,V>{
	
	
	//Shadow "re-declaration" of datastore. Because we need a more specific type. 
	protected KeyValueDatastoreProxy datastore;
	protected Serializer<V> valueSerializer;
	
	
	public static <K,V> KeyValueTable_<K,V> getTable(KeyValueDatastoreProxy proxy, String tableName,
			Serializer<K> keySerializer, Serializer<V> valueSerializer) {
		return new KeyValueTable_<K,V>(proxy,tableName,  keySerializer, valueSerializer); 
	}
	
	
	/**
	 * @param proxy
	 * @param tableName
	 * @param keySerializer
	 * @param valueSerializer
	 */
	protected KeyValueTable_(KeyValueDatastoreProxy proxy, String tableName,
			Serializer<K> keySerializer, Serializer<V> valueSerializer) {
		super(proxy, tableName, keySerializer);
		this.valueSerializer = valueSerializer; 
		datastore = proxy; 
	}
	
	

	
	/* 
	 * @see bonafide.datastore.tables.Table#remove(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean remove(K key, V value) {
		return datastore.remove(tableName, serializeKey(key), serializeValue(value));
	}
	
	/* (non-Javadoc)
	 * @see bonafide.datastore.tables.Table#putIfAbsent(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V putIfAbsent(K key, V value) {
		byte[] result =  datastore.putIfAbsent(tableName, serializeKey(key), serializeValue(value));
		return result != null? valueSerializer.deserialize(result): null; 
	}

	/* (non-Javadoc)
	 * @see bonafide.datastore.tables.Table#remove(java.lang.Object)
	 */
	@Override
	public V remove(K key) {
		byte[] result = datastore.remove(tableName, serializeKey(key));
		return result != null? valueSerializer.deserialize(result) : null; 
	}

	/* (non-Javadoc)
	 * @see bonafide.datastore.tables.Table#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V put(K key, V value) {
		byte[] result = datastore.put(tableName, serializeKey(key), serializeValue(value));
		return result != null?  valueSerializer.deserialize(result) : null; 
	}

	/* (non-Javadoc)
	 * @see bonafide.datastore.tables.KeyValueTable#insert(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean insert(K key, V value) {
		return datastore.insert(tableName, serializeKey(key), serializeValue(value));
	}


	/* (non-Javadoc)
	 * @see bonafide.datastore.tables.Table#get(java.lang.Object)
	 */
	@Override
	public V get(K key) {
		//XXX
		if (key == null) return null; 
		byte[] result = datastore.get(tableName, serializeKey(key));
		return result != null ? valueSerializer.deserialize(result): null;
	}

	@Override
	public Collection<V> values(){
		Collection<byte[]> byte_values = datastore.values(tableName); 
		Collection<V>  values = Lists.newArrayList(); 
		for (byte[] ba : byte_values){
			values.add(valueSerializer.deserialize(ba));
			
		}
		return values; 
	}

	/**
	 * @param value
	 * @return
	 */
	protected byte[] serializeValue(V value) {
		return valueSerializer.serialize(value);
	}


	

	
}
