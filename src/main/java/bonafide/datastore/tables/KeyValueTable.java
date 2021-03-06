/**
 * 
 */
package bonafide.datastore.tables;

/**
 * @author fabiim
 *
 */
public interface KeyValueTable<K, V> extends Table<K,V>{


	/**
	 * @param key
	 * @param value
	 * @return
	 */
	boolean remove(K key, V value);

	/**
	 * @param key
	 * @param value
	 * @return
	 */
	V putIfAbsent(K key, V value);

	/**
	 * @param key
	 * @return
	 */
	V remove(K key);

	/**
	 * @param key
	 * @param value
	 * @return
	 */
	V put(K key, V value);

	/**
	 * @param key
	 * @return
	 */
	V get(K key);
	
	boolean insert(K key, V value); 
	
}
