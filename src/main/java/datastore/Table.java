package datastore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import net.floodlightcontroller.devicemanager.internal.Device;

import org.python.google.common.base.Function;
import org.python.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import datastore.workloads.RequestLogEntry;



public class Table<K extends Serializable ,V extends Serializable> {	
	private final String tableName;
	private final Datastore datastore;
	private Function<K, byte[]> serializeKeyFoo;
	private Function<byte[],K> deserializeKeyFoo; 
	protected static Logger log = LoggerFactory.getLogger(Table.class);
	//protected static Logger log = LoggerFactory.getLogger("");
	
	public byte[] serializeO(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }
	
    public  Object deserializeO(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return o.readObject();
    }

	
	public Table(Datastore ds, String tableName, Function<byte[],K> deserialize, Function<K,byte[]> serialize){
		
		this.tableName = tableName;
		this.datastore = ds;
		
		this.serializeKeyFoo = serialize == null ? defaultSerializeKey : serialize;
		
		this.deserializeKeyFoo = deserialize == null ?  defaultDeserializeKey : deserialize; 
	}

	
	protected Function<K,byte[]> defaultSerializeKey = new Function< K, byte[]>(){
		
		@Override 
		public byte[] apply(K k){
			try {
				return serializeO(k);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			return null; 
		}
	};
	protected Function<byte[],K> defaultDeserializeKey = new Function<byte[],K>(){
		@Override 
		public K apply(byte[] v){
			try {
				return (K) deserializeO(v);
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

	public void setDeserializeKeyFunction(Function<byte[],K> foo){
		deserializeKeyFoo= foo; 
	}
	
	public void setSerializeKeyFunction(Function<K,byte[]> foo){
	   serializeKeyFoo = foo; 
	}
	
	public void setSerializeFunctions(Function<K,byte[]> serialFoo, Function<byte[],V> deserialFoo){
		this.setSerializeKeyFunction(serializeKeyFoo);
		this.setDeserializeKeyFunction(deserializeKeyFoo); 
	}
	
	/*
	 * Delegate methods to datastore.  
	 */
	public void clear() {
		datastore.clear(tableName, new RequestLogEntry(tableName));
	}

	public boolean containsKey(K key) {
		if (key == null) return false;  //TODO maybe not the most correct behaviour
		boolean result = datastore.containsKey(tableName, serializeKey(key), new RequestLogEntry(tableName)); 
		log.info("contains key: " + key + "from table: " + tableName + " result: " + result );
		return result; 
	}
	
	public Map<K, V> getAll() {
		log.info("Get All from " + tableName); 
		Map<byte[],byte[]> serializedMap = datastore.getAll(tableName, new RequestLogEntry(tableName));
		Map<K,V>  result = deserialize(serializedMap); 
		log.info("request for all values from table: " + tableName + " result size: "  +  (result != null ? result.size(): 0));
		return result; 
	}
	
	public V get(K key) {
		if (key == null) return null;
		V val = deserialize(datastore.get(tableName,serializeKey(key), new RequestLogEntry(tableName, key.toString())));
		log.info("Request for value: " + key + " at table: " + tableName + " result: " + val);
		return val; 
	}
	public V get(K key, boolean v) {
		if (key == null) return null;
		V val = deserialize(datastore.get(tableName,serializeKey(key), new RequestLogEntry("DUMMY")));
		//log.info("Request for value: " + key + " at table: " + tableName + " result: " + val);
		return val; 
	}



	public boolean isEmpty() {
		return datastore.isEmpty(tableName, new RequestLogEntry(tableName));
	}


	public boolean put(K key, V value) {
		if (key == null) return false;
		boolean r =  datastore.put(tableName, serializeKey(key), serialize(value), new RequestLogEntry(tableName, key.toString(), value.toString()));
		log.info("Put : key: " + key + "@" + tableName + " value: " + value );
		return r; 
	}


	public void putAll(Map<? extends K, ? extends V> m) {
		datastore.putAll(tableName, serialize(m), new RequestLogEntry(tableName));
	}

	
	public V remove(K key) {
		if (key == null) return null; 
		//TODO - boolean return as put?.
		
		return deserialize(datastore.remove(tableName, serializeKey(key), new RequestLogEntry(tableName)));
	}


	public int size() {
		return datastore.size(tableName, new RequestLogEntry(tableName));
	}

	
	public boolean replace(K key, V existentValue, V newValue) {
		log.info("Replace: " + key + " -> " + newValue + " if : " + existentValue); 
		return datastore.replace(tableName,serializeKey(key), serialize(existentValue), serialize(newValue), new RequestLogEntry(tableName, key.toString(),newValue.toString(), existentValue.toString())); 
		
	}
	
	
	public boolean remove(K key, V value) {
		return datastore.remove(tableName, serializeKey(key), serialize(value), new RequestLogEntry(tableName, key.toString(), value.toString()));
	}

	

	public V putIfAbsent(K key, V value) {
		log.info("Put if absent : " + key + " -> " +  value); 
		return  deserialize(datastore.putIfAbsent(tableName,serializeKey(key), serialize(value), new RequestLogEntry(tableName, key.toString(), value.toString())));
	}
	
	// Private methods //

		//SERIALIZE/DESERIALIZE// 
	private Map<K, V> deserialize(Map<byte[], byte[]> values){
		
		if (values != null){
		Map<K,V> result = Maps.newHashMap(); 
		for (Entry<byte[],byte[]> en : values.entrySet()){
			result.put(deserializeKey(en.getKey()), deserialize(en.getValue())); 
		}
		
		return result;
		}
		return null; 
	}
	
	@SuppressWarnings("unchecked")
	private V deserialize(byte [] v){
		if (v == null ) return null; 
		try {
			return (V) deserializeO(v);
		} catch (IOException e) { //FIXME 
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace(); 
		}
		return null; 
	}
	
	
	private byte[] serialize(V value){
		//TODO: how about null values?
			try {
				return serializeO(value);
			} catch (IOException e) {

				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		return null; // FIXME 
	}

	private byte[] serializeKey(K val){
		return this.serializeKeyFoo.apply(val); 
	}
	
	private K deserializeKey(byte[] val){
		
		return this.deserializeKeyFoo.apply(val);
	}
	
	private Map<byte[], byte[]> serialize(Map<? extends K, ? extends V> values) {
		Map<byte[],byte[]> result = Maps.newHashMap(); 
		for (Entry<? extends K,? extends V> en : values.entrySet()){
			result.put(serializeKey(en.getKey()), serialize(en.getValue())); 
		}
		return result;
	}

	public void test(V oldDevice, V device, String s) {
		if (!Arrays.equals(serialize(oldDevice), serialize(device))){
			System.out.println(s);
			System.out.println(device); 
			System.out.println(oldDevice);
		}
		
	}

	
	
		// Others //
}
