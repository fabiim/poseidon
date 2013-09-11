package datastore.column;

import java.io.Serializable;
import java.util.Map;

import com.google.common.base.Function;

import datastore.Datastore;






public class Table<K ,V extends TrackableColumns> implements TableType<K,V>{
	private Datastore ds;
	private KeyValueColumnObject<V> serializator;
	
	@Override
	public void clear() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean containsKey(K key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<K, V> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V get(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V get(K key, boolean v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean put(K key, V value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean put(K key, V value, boolean b) {
		// TODO Auto-generated method stub
		return false;
	}

	

	@Override
	public V remove(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean replace(K key, V existentValue, V newValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean remove(K key, V value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public V putIfAbsent(K key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V getAndIncrement(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsColumn(K key, String column) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	@Override
	public <C> C getColumn(K key, String column, Class<C> type) {
		 return serializator.getColumn(column, getCol(), type); 
	}
	
	private Map<String,byte[]> dataStoreSimulation(){
		return null; 
	}
	private byte[] getCol(){
		return null; 
	}
	
	

	@Override
	public <C> void replaceColumn(K key, String column, Class<C> type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <C> void removeColumnIfEqual(K key, String column, C value,
			Class<C> type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <C> void putIfAbsent(K key, String column, C value, Class<C> type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <C> void putColumn(K key, String column, C columnValue, Class<C> type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <C> void removeColumn(K key, String column) {
		// TODO Auto-generated method stub
		
	}
	
}
