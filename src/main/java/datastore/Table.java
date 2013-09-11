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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bonafide.datastore.workloads.RequestLogEntry;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import ch.qos.logback.classic.Level;


//TODO - There should be a factory producing tables with the same name... Singletons table should go out... 



public class Table<K extends Serializable ,V extends Serializable> {
	
	protected final String tableName;
	protected final Datastore datastore;
	
	
	protected Function<K, byte[]> serializeKeyFoo;
	protected Function<V, byte[]> serializeValueFoo;
	protected Function<byte[],K> deserializeKeyFoo; 
	public Function<V, byte[]> getSerializeValueFoo() {
		return serializeValueFoo;
	}

	public void setSerializeValueFoo(Function<V, byte[]> serializeValueFoo) {
		this.serializeValueFoo = serializeValueFoo;
	}

	public Function<byte[], V> getDeserializeValueFoo() {
		return deserializeValueFoo;
	}

	public void setDeserializeValueFoo(Function<byte[], V> deserializeValueFoo) {
		this.deserializeValueFoo = deserializeValueFoo;
	}


	protected Function<byte[],V> deserializeValueFoo;
	
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
		//TODO - add to constructor 
		this.serializeValueFoo = defaultSerializeValue; 
		this.deserializeValueFoo = defaultDeserializeValue; 
	}
	
	
	//////////////TODO For god sake , clean this up dude!  
	////////////TODO  For starters, defaults, should be initialzied with no attributes. Just initialize them. It is a bit verbose, but fuck that... 
	////////////TODO 
	//	//////////TODO 
	protected Function<V,byte[]> defaultSerializeValue = new Function< V, byte[]>(){	
		@Override 
		public byte[] apply(V v){
			try {
				return serializeO(v);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			return null; 
		}
	}; 
	
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

	protected Function<byte[],V> defaultDeserializeValue = new Function<byte[],V>(){
		@Override 
		public V apply(byte[] v){
			try {
				return (V) deserializeO(v);
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

	public boolean put(K key, V value, boolean b) {
		// TODO Auto-generated method stub
		if (key == null) return false;
		boolean r =  datastore.put(tableName, serializeKey(key), serialize(value), null);
		log.info("Put : key: " + key + "@" + tableName + " value: " + value );
		return r; 
	}


	public void putAll(Map<? extends K, ? extends V> m) {
		datastore.putAll(tableName, serialize(m), new RequestLogEntry(tableName));
	}

	
	public V remove(K key) {
		if (key == null) return null; 
		//TODO - boolean return as put?.
		
		return deserialize(datastore.remove(tableName, serializeKey(key), new RequestLogEntry(tableName, key.toString())));
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

	public V getAndIncrement(K key) {
		return deserialize(datastore.getAndIncrement(tableName, serializeKey(key), new RequestLogEntry(tableName, key.toString())));

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
	protected V deserialize(byte [] v){
		if (v == null ) return null; 
		return this.deserializeValueFoo.apply(v);
	}
	
	protected byte[] serialize(V value){
		//TODO: how about null values?

		return this.serializeValueFoo.apply(value); 

	}

	protected byte[] serializeKey(K val){
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
