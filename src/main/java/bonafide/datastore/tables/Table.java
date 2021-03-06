/**
 * 
 */
package bonafide.datastore.tables;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bonafide.datastore.workloads.RequestLogEntry;

/**
 * @author fabiim
 *
 */
public interface Table<K,V> {
	public void clear() ;
	public boolean containsKey(K key);
	public boolean containsValue(K value);
	public Set<java.util.Map.Entry<K, V>> entrySet();

	public boolean isEmpty();
	public Set<K> keySet();

	public void putAll(Map<? extends K, ? extends V> m);

	public int size() ;
	public Collection<V> values();
	public int getAndIncrement(String key);
}

