/**
 * 
 */
package bonafide.datastore.tables;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mapserver.RequestType;
import bonafide.datastore.AbstractDatastoreProxy;
import bonafide.datastore.ColumnProxy;
import bonafide.datastore.workloads.RequestLogEntry;
import bonafide.datastore.workloads.RequestLogWithDataInformation;

//TODO - It would be nice to have an implementation of this that did not require using the smart middleware, only the server implementation locally. It would be faster to perform Workload analysis. 

/**
* A Data store proxy client that records <b>RPC</b> requests characteristics for  <b>one</b> request. 
* Yeah, I known, one instance per/ request sounds lame. I didn't find a way to make this more efficient. However it does the job, and usually while recording 
* requests characteristics we do not  care about efficiency. At least in the use for which this class was designed for we don't.  
* <p>
* 
* Each instance of this class must keep a reference to a {@link RequestLogEntry }. For each request invocation   
* this RequestLogEntry will be set up with: 
* <ul>
* <li> {@link RequestLogEntry#getTimeStarted() timeStarted}     		the time (ns) at which the request was sent to the data store. </li>
* <li> {@link RequestLogEntry#getTimeEnded() timeEnded }         		the time (ns) at which a reply was received from the data store. </li>
* <li> {@link RequestLogEntry#getSizeOfRequest() sizeOfRequest}  		the size (bytes) of the byte array payload sent to the data store. </li>
* <li> {@link RequestLogEntry#getSizeOfResponse() sizeOfResponse}  	the size (bytes) of the reply payload sent from the data store </li>
* <li> {@link RequestLogEntry#getType() type} 			                the operation performed  on the data store</li>
* <li> </li>
* </ul>
* 
* The values of <code>timeStarted</code> and TimeEnded are taken before and after the invocation of {@link AbstractDatastoreProxy#invokeRequest(RequestType, byte[])}. There units are taken in nanoseconds.
* <p>    
* As for <code>sizeOfRequest</code> and <code>sizeOfResponse</code> they measure in bytes the request and reply <b>RPC</b> payload sent/received from the data store. 
* Please notice that this does not take into account: the total packet size sent, or the overhead of the data store middleware protocol in use (e.g., consensus data).  
* <p>
* Finally, <code>type</code> sets the <b>RPC</b> operation information ({@link RequestType}).   
* <p>
* 
*/


class WorkloadLoggerDataStoreProxy extends ColumnProxy{
	
	private RequestLogEntry logEntry;
	
	/**
	 * Create 
	 */
	public WorkloadLoggerDataStoreProxy(RequestLogEntry logEntry){
		super(0); //FIXME 
		this.logEntry = logEntry; 
	}
	
	/**
	 * Records requests characteristics in a {@link RequestLogEntry}.
	 * <p>
	 * Characteristics recorded are: timeStarted/timeEnded (ns); sizeOfRequest/sizeofResponse (bytes) and type (RequestType).   
	 *  
	 * @see bonafide.datastore.AbstractDatastoreProxy#invokeRequest(mapserver.RequestType, byte[])
	 * @see WorkloadLoggerDataStoreProxy
	 */
	@Override
	protected byte[] invokeRequest(RequestType type, byte[] request) {
		logEntry.setTimeStarted(System.nanoTime());
		logEntry.setSizeOfRequest(request.length);
		logEntry.setType(type);
		byte[] result =  super.invokeRequest(type, request);
		logEntry.setTimeEnded(System.nanoTime());
		return result; 
	}
	
	/**
	 * Returns the RequestLogEntry used to capture the request characteristics.
	 * @return the RequestLogEntry used.   
	 */
	public RequestLogEntry getLogEntry(){
		return logEntry; 
	}
}



/**
 * @author fabiim
 *
 */
public class WorkloadLoggerTable<K,V>  implements KeyValueTable<K,V>{
	
	KeyValueTable<K,V> table;
	RequestLogEntry entry; 
	String tableName; 
	@Override
	public synchronized boolean remove(K key, V value) {
		boolean val = table.remove(key, value);
		logEntry(new RequestLogWithDataInformation.Builder().setTable(tableName).setKey(key.toString()).setValue(value.toString()).build(entry));
		return val; 
	}
	
	/**
	 * @param build
	 */
	private void logEntry(RequestLogWithDataInformation build) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public synchronized V putIfAbsent(K key, V value) {
		V val = table.putIfAbsent(key, value);
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setKey(key.toString()).
				setValue(value.toString()).
				setReturnedValue(val.toString()).
				build(entry));
		return val; 
	}
	@Override
	public synchronized void clear() {
		table.clear();
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				build()); 
	}
	@Override
	public synchronized boolean containsKey(K key) {
		Boolean val = table.containsKey(key);
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setKey(key.toString()).
				setReturnedValue(val.toString()).
				build(entry));
		return val; 

	}
	@Override
	public synchronized V remove(K key) {
		V val = table.remove(key);
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setKey(key.toString()).
				setReturnedValue(val.toString()).
				build(entry));
		return val; 
	}
	@Override
	public synchronized boolean containsValue(K value) {
		Boolean val = table.containsValue(value);
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setValue(value.toString()).
				setReturnedValue(val.toString()).
				build(entry));
		return val; 
	}
	@Override
	public synchronized Set<Entry<K, V>> entrySet() {
		Set<Entry<K,V>> entries = table.entrySet();
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setReturnedValue(entries.toString()).
				build());
		return entries; 
	}
	
	@Override
	public synchronized V put(K key, V value) {
		V val =  table.put(key, value);
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setKey(key.toString()).
				setValue(value.toString()).
				setReturnedValue(val.toString()).
				build()); 
		return val; 
	}
	
	@Override
	public synchronized boolean isEmpty() {
		Boolean val = table.isEmpty();
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				build());
		return val; 
	}
	@Override
	public synchronized Set<K> keySet() {
		Set<K> keys =  table.keySet();
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setReturnedValue(keys.toString()).
				build());
		return keys; 
	}
	@Override
	public synchronized V get(K key) {
		V val = table.get(key);
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setKey(key.toString()).
				setReturnedValue(val.toString()).
				build());
		return val; 
	}
	
	@Override
	public synchronized void putAll(Map<? extends K, ? extends V> m) {
		table.putAll(m);
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setKey(m.toString()).
				build());
	}
	
	@Override
	public synchronized boolean insert(K key, V value) {
		Boolean b = table.insert(key, value);
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setKey(key.toString()).
				setValue(value.toString()).
				setReturnedValue(b.toString()).
				build());
		return b; 
	}
	
	@Override
	public synchronized int size() {
		Integer val = table.size();
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setReturnedValue(val.toString()).
				build());
		return val; 
	}
	
	@Override
	public synchronized Collection<V> values() {
		Collection<V> values  =  table.values();
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setReturnedValue(values.toString()).
				build());
		return values; 
	}
	
	@Override
	public synchronized int getAndIncrement(String key) {
		
		Integer val = table.getAndIncrement(key);
		logEntry(new RequestLogWithDataInformation.Builder().
				setTable(tableName).
				setKey(key.toString()).
				setReturnedValue(val.toString()).build());
		return val; 
	}
}

class ColumnWorkloadLogger< K,V> extends WorkloadLoggerTable<K,V> implements ColumnTable<K,V>{

	/* (non-Javadoc)
	 * @see bonafide.datastore.tables.ColumnTable#getColumn(java.lang.Object, java.lang.String)
	 */
	@Override
	public <C> C getColumn(K key, String columnName) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see bonafide.datastore.tables.ColumnTable#setColumn(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	@Override
	public boolean setColumn(K key, String columnName, Object type) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
