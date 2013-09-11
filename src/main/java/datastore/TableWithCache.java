package datastore;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;



public class TableWithCache<K extends Serializable,V extends Serializable> extends Table<K,V>{

	//TODO - extends the final map to take care of values for you. 
	static class TimeStampedValue<V>{
		//TODO check weak references... 
		final V value;
		final long timestamp; 
		
		TimeStampedValue(V v){
			value = v; 
			timestamp = System.currentTimeMillis(); 
		}
	}
	
	Map<K,TimeStampedValue<V>> cache = Maps.newConcurrentMap();
	
	@SuppressWarnings("unchecked")
	public TableWithCache(Datastore ds, String tableName, Function<byte[],K> deserialize,
			Function<K,byte[]> serialize) {
		super(ds, tableName, deserialize, serialize);
	}
	
	public V get(K key, long timestamp) {
		TimeStampedValue<V> cached_value = cache.get(key);
		if (cached_value != null){
			if ( System.currentTimeMillis() - cached_value.timestamp < timestamp){
				return cached_value.value;
			}
		}
		V value = super.get(key);
		cached_value = new TimeStampedValue<V>(value); 
		return value; 
	}
	
	public V getCached(K key){
		TimeStampedValue<V> cached_value = cache.get(key);
		if (cached_value != null){
			return cached_value.value;
		}
		else 
			return null; 
	}

	@Override
	public boolean put(K key, V value) {
		cache.remove(key);
		return super.put(key, value);
	}

	@Override
	public boolean put(K key, V value, boolean b) {
		cache.remove(key); 
		return super.put(key, value, b);
	}

	@Override
	public V remove(K key) {
		cache.remove(key);
		// TODO Auto-generated method stub
		return super.remove(key);
	}

	@Override
	public boolean replace(K key, V existentValue, V newValue) {
		cache.remove(key);
		return super.replace(key, existentValue, newValue);
	}

	@Override
	public boolean remove(K key, V value) {
		cache.remove(key); 
		// TODO Auto-generated method stub
		return super.remove(key, value);
	}
	
}
