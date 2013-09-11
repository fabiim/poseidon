package datastore.column;

import java.util.Map;

public interface TableType<K,V> {
	//Key - Value Store. 
	public  void clear();
	public  boolean containsKey(K key);
	public  Map<K, V> getAll();
	public  V get(K key);
	//DUBIOUS METHOD. 
	public  V get(K key, boolean v);
	public  boolean isEmpty();
	public  boolean put(K key, V value);
	//DUBIOUS METHOD
	public  boolean put(K key, V value, boolean b);
	public  V remove(K key);
	public  int size();
	public  boolean replace(K key, V existentValue, V newValue);
	public  boolean remove(K key, V value);
	public  V putIfAbsent(K key, V value);
	public  V getAndIncrement(K key);

	//Column Methods
	public boolean containsColumn(K key, String column);
	
	public <C> C getColumn(K key, String column, Class<C> type);
	public <C> void putColumn(K key, String column, C columnValue, Class<C> type);
	
	public <C> void removeColumn(K key, String column); 
	public <C> void replaceColumn(K key, String column, Class<C> type);
	public <C> void removeColumnIfEqual(K key, String column, C value, Class<C> type);
	public <C> void putIfAbsent(K key,String column, C value, Class<C> type);
	
	
}