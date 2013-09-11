package datastore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import mapserver.RequestType;
import net.floodlightcontroller.topology.TopologyManager;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import ch.qos.logback.classic.Level;
import bftsmart.tom.ServiceProxy;
import bonafide.datastore.workloads.RequestLogEntry;
import datastore.workloads.logger.RequestLogger;

/**
 * @author fabiim
 *
 */

public class Datastore implements DataStoreInterface{

	public static void initDatastoreFromController(Datastore ds){
		Table<String, Long>  t = ds.getTable(Datastore.CONTROLLERS_SYSTEM_INFO, datastore.util.KeySerializationFunctions.STRING_DESERIALIZE	, datastore.util.KeySerializationFunctions.STRING_SERIALIZE); 
		
		//DeviceIndex 
		if (!t.put(Datastore.DEVICE_MANAGER_LAST_COUNTER, 0L)){
			System.err.println("Device Manager Last Counter was initiliazed"); 
		}

		Table<String,String>  t2 = ds.getTable(Datastore.CONTROLLERS_SYSTEM_INFO, null, null); 
		t2.put("DEVICE_UNIQUE_INDEX_COUNTER", "0");
		t2.put("DEVICE_MULTI_INDEX_COUNTER", "0"); 
		//TODO - several copies of the datastore locally (� preciso aproveitar as threads) . 
		//TODO - cada uma das aplica��es inicializa as cenas .
		
	}

	
	protected static Logger log = LoggerFactory.getLogger(TopologyManager.class);

	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#serialize(java.lang.Object)
	 */
	public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }
	
	
    /* (non-Javadoc)
	 * @see datastore.DataStoreInterface#deserialize(byte[])
	 */
	
    public  Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return o.readObject();
    }
    
	protected ServiceProxy clientProxy = null;
	protected RequestLogger  benchManager;
	protected Configuration config; 

	public static int clientId=0;
	
	public Datastore() { 
		((ch.qos.logback.classic.Logger) log).setLevel(Level.ERROR); 

		// create and load default properties
		try{
			config = new PropertiesConfiguration("datastore.config"); 
		} catch (ConfigurationException e) {
			System.err.println("Could not read configuration file");
			System.exit(-1);
		}
		
		clientProxy = new ServiceProxy(clientId++); 
		if (config.getBoolean("benchmark")){
			RequestLogger.startRequestLogger(config.getString("benchmark.output"));
	    	benchManager = RequestLogger.getRequestLogger(); 
		}
	 }
	
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#getBenchManager()
	 */
	public RequestLogger getBenchManager(){ return benchManager; }
	
	// Correct me .
	public <K extends Serializable,A extends Serializable> TableInterface<K, A> getTable(String tableName, Function<byte[],K> deserialize, Function<K,byte[]> serialize){
		boolean created = true; 
		if (!this.containsTable(tableName,new RequestLogEntry(tableName))){ 
			created =this.createTable(tableName, new RequestLogEntry(tableName));
			log.info("creating a table: " + tableName + " Result : " + created);
		}
		return created ? new Table<K,A>(this, tableName, deserialize,serialize): null;   
	}
	

	public  <K extends Serializable,A extends Serializable> TableInterface<K, A> getTableL(String tableName,
			Function<byte[], K> deserialize,
			Function<K, byte[]> serialize) {
		boolean created = true; 
		if (!this.containsTable(tableName,new RequestLogEntry(tableName))){ 
			created =this.createTable(tableName, new RequestLogEntry(tableName));
			log.info("creating a table: " + tableName + " Result : " + created);
		}
		return created ? new TableWithCache<K,A>(this, tableName, deserialize,serialize): null;   
	
	}

	
	
	 /*************************************************
	  * General Functions: 
	  * Functions who do not address specific tables.
	  */
	
	 /* (non-Javadoc)
	 * @see datastore.DataStoreInterface#createTable(java.lang.String, datastore.workloads.RequestLogEntry)
	 */
	 public boolean createTable(String tableName, RequestLogEntry r){
		 RequestType type = RequestType.CREATE_TABLE;  
		 ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		 DataOutputStream dout = new DataOutputStream(out);
		 try{
			 dout.writeInt(type.ordinal());
			 
			 dout.writeUTF(tableName);
			 byte[] reply = invokeRequestOrdered(out,dout, type, r);
			 
			 if (reply != null){ 
				 return true; 
			 }
			 else return false; 
		 }catch(IOException e ){
			log.error("Exception getting values from datastore: " + e.getMessage());
			return false; //TODO - throw exception. 
		 }
	 }
	 
	 /* (non-Javadoc)
	 * @see datastore.DataStoreInterface#createTable(java.lang.String, int, datastore.workloads.RequestLogEntry)
	 */
	public boolean createTable(String tableName, int maxSize, RequestLogEntry r){
		 RequestType type = RequestType.CREATE_TABLE_MAX_SIZE;  
		 ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		 DataOutputStream dout = new DataOutputStream(out);
		 try{
			 dout.writeInt(type.ordinal()); 
			 dout.writeUTF(tableName);
			 dout.writeInt(maxSize); 
			 byte[] reply = invokeRequestOrdered(out,dout, type, r);
			 if (reply != null){ 
				 return true; 
			 }
			 else return false; 
		 }catch(IOException e ){
			log.error("Exception getting values from datastore: " + e.getMessage());
			return false; //TODO - throw exception. 
		 }
	 }
	 
	 /* (non-Javadoc)
	 * @see datastore.DataStoreInterface#removeTable(java.lang.String, datastore.workloads.RequestLogEntry)
	 */
	 public boolean removeTable(String tableName, RequestLogEntry r){
		 log.info("Removing table: " + tableName);
		 try{
			 throw new Exception(); 
		 }catch (Exception e){
			 e.printStackTrace(System.err); 
		 }
		 RequestType type = RequestType.REMOVE_TABLE; 
		 ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		 DataOutputStream dout = new DataOutputStream(out); 
		 
		 try{
			 dout.writeInt((type.ordinal())); 
			 dout.writeUTF(tableName); 
			 byte[] reply = invokeRequestOrdered(out,dout,type, r);
			 if (reply != null){ 
				 return true; 
			 }
			 else return false; 
		 }catch(IOException e ){
			log.error("Exception getting values from datastore: " + e.getMessage());
			return false; 
		 }
	 }
	 
	 /* (non-Javadoc)
	 * @see datastore.DataStoreInterface#containsTable(java.lang.String, datastore.workloads.RequestLogEntry)
	 */
	 public boolean containsTable(String tableName, RequestLogEntry r){
		 RequestType type = RequestType.CONTAINS_TABLE; 
		 ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		 DataOutputStream dout = new DataOutputStream(out);
		 
		 try{
			 dout.writeInt((type.ordinal())); 
			 dout.writeUTF(tableName); 
			 byte[] reply = invokeRequestOrdered(out,dout,type, r);
			 if (reply != null){ 
				 return true; 
			 }
			 else return false; 
		 }catch(IOException e ){
			log.error("Exception getting values from datastore: " + e.getMessage());
			return false; 
		 }
	 }
	 
	 /* (non-Javadoc)
	 * @see datastore.DataStoreInterface#clear(datastore.workloads.RequestLogEntry)
	 */
	public void clear(RequestLogEntry r) {
		 RequestType type = RequestType.CLEAR_DATASTORE; 
			ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			DataOutputStream dos = new DataOutputStream(out);
			try {
				dos.writeInt(type.ordinal());
				invokeRequestOrdered(out,dos,type, r);
				return ; //TODO - close streams. 
			} catch (IOException e) {
				log.error("Exception getting values from datastore: " + e.getMessage()); //TODO - Exceptions. 
				return; //OH .... 
			} 
		}
	 
	 /////////////MAP Interface ///////////////
	 //////////////////////////////////////////
	 
	 /**
	  * Clear all database, including tables names. 
	  */
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#clear(java.lang.String, datastore.workloads.RequestLogEntry)
	 */
	public void clear(String table, RequestLogEntry r){
		RequestType type = RequestType.CLEAR_TABLE; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);

		try {
			dos.writeInt(type.ordinal());
			dos.writeUTF(table); 
			invokeRequestOrdered(out,dos,type, r);
			return ; //TODO - close streams. 
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return; //OH .... 
		} 
	}
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#containsKey(java.lang.String, byte[], datastore.workloads.RequestLogEntry)
	 */
	public boolean containsKey(String table, byte[] key, RequestLogEntry requestLogEntry) {
		RequestType type = RequestType.CONTAINS_KEY_IN_TABLE; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);
		
		try {
			dos.writeInt(type.ordinal());
			dos.writeUTF(table); 
			dos.writeInt(key.length);
			dos.write(key);
			byte[] reply = invokeRequestOrdered(out,dos,type, requestLogEntry);
			return reply !=null;
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return false; //OH .... 
		} 
	}
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#getAll(java.lang.String, datastore.workloads.RequestLogEntry)
	 */
	@SuppressWarnings("unchecked")
	public Map<byte[],byte[]> getAll(String table, RequestLogEntry r){
		RequestType type = RequestType.GET_TABLE;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		try{
			dos.writeInt(type.ordinal());
			
			dos.writeUTF(table); //CHECK - seems safe but also waste. Why do Map interface gets Object in get?
			byte[] reply = invokeRequestOrdered(out, dos,type, r ); //Check - Not sure it needs the two arguments.
			try {
				if (reply != null) 
					return (Map<byte[],byte[]>) (deserialize(reply));
				else 
					return null; 
				
			} catch (ClassNotFoundException e) {
				log.error("Could not deserialize result " + e.getMessage());
				return null; 
			} 
		}catch(IOException e ){
			log.error("Exception retrieving all the values from the datastore" + e.getMessage());
			return null; 
		}
	}
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#get(java.lang.String, byte[], datastore.workloads.RequestLogEntry)
	 */
	@SuppressWarnings("unchecked")
	public byte[] get(String table, byte[] key, RequestLogEntry r) {
		RequestType type = RequestType.GET_VALUE_IN_TABLE; 
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		try{
			dos.writeInt(type.ordinal());
			dos.writeUTF(table);
			dos.writeInt(key.length); 
			dos.write(key);
			return  invokeRequestOrdered(out, dos,type, r); //Check - Not sure it needs the two arguments. 

			//TODO close streams. 
		}catch(IOException ioe){
			log.error("Exception getting values from datstore: " + ioe.getMessage()); 
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#isEmpty(datastore.workloads.RequestLogEntry)
	 */
	public boolean isEmpty(RequestLogEntry r) {
		RequestType type = RequestType.IS_DATASTORE_EMPTY; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeInt(type.ordinal());
			byte[] reply = invokeRequestOrdered(out,dos,type, r);
			return reply != null; 
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return false; //OH .... 
		} 
	}
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#isEmpty(java.lang.String, datastore.workloads.RequestLogEntry)
	 */
	public boolean isEmpty(String table, RequestLogEntry r){
		RequestType type = RequestType.IS_TABLE_EMPTY; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);

		try {
			dos.writeInt(type.ordinal()); 
			dos.writeUTF(table); 
			byte[] reply = invokeRequestOrdered(out,dos,type, r);
			return reply != null; 
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return false; //OH .... 
		} 
	}
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#put(java.lang.String, byte[], byte[], datastore.workloads.RequestLogEntry)
	 */
	
	//TODO - return existent value. 
	public boolean put(String table, byte[] key, byte[] value, RequestLogEntry r) {
		RequestType type = RequestType.PUT_VALUE_IN_TABLE; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);
		
		try {
			
			dos.writeInt(type.ordinal());
			dos.writeUTF(table); 
			dos.writeInt(key.length); 
			dos.write(key);
			dos.write(value);
			System.out.println( "Sizes: value - " + value.length  +  "key - " + key.length);
			byte[] reply = invokeRequestOrdered(out,dos, type, r);
			return reply != null;
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return false;
		} 
	}
	
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#replace(java.lang.String, byte[], byte[], byte[], datastore.workloads.RequestLogEntry)
	 */
	public boolean replace(String tableName, byte[] key,
			byte[] oldValue, byte[] newValue, RequestLogEntry r) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);
		try{
			dos.writeInt(RequestType.ATOMIC_REPLACE_VALUE_IN_TABLE.ordinal());
			dos.writeUTF(tableName);
			dos.writeInt(key.length);
			dos.write(key);
			dos.writeInt(oldValue.length);
			dos.write(oldValue); 
			dos.write(newValue);
			byte[] reply = invokeRequestOrdered(out, dos, RequestType.ATOMIC_REPLACE_VALUE_IN_TABLE, r); 
			return reply != null; 
		}catch(IOException e){
			log.error("Exception getting values from datastore: " + e.getMessage());
		}
		return false; 
	}
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#remove(java.lang.String, byte[], byte[], datastore.workloads.RequestLogEntry)
	 */
	public boolean remove(String tableName, byte[] key, byte[] val, RequestLogEntry r) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);
		try{
			dos.writeInt(RequestType.ATOMIC_REMOVE_IF_VALUE.ordinal());
			dos.writeUTF(tableName);
			dos.writeInt(key.length);
			dos.write(key); 
			dos.write(val);
			byte[] reply = invokeRequestOrdered(out, dos, RequestType.ATOMIC_REMOVE_IF_VALUE, r); 
			return reply != null; 
		}catch(IOException e){
			log.error("Exception getting values from datastore: " + e.getMessage());
		}
		return false; 
	}

	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#putAll(java.lang.String, java.util.Map, datastore.workloads.RequestLogEntry)
	 */
	public void putAll(String table, Map<? extends byte[], ? extends byte[]> m, RequestLogEntry r) {
		RequestType type = RequestType.PUT_VALUES_IN_TABLE; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out); 
		try {
			dos.writeInt(type.ordinal());
			dos.writeUTF(table);
			dos.write(serialize(m));
			invokeRequestOrdered(out,dos,type, r); 
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return;
		} 
	}
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#remove(java.lang.String, byte[], datastore.workloads.RequestLogEntry)
	 */
	public byte[] remove(String table, byte[] key, RequestLogEntry r) {
		RequestType type = RequestType.REMOVE_VALUE_FROM_TABLE; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeInt(type.ordinal()); 
			dos.writeUTF(table); 
			dos.writeInt(key.length); 
			dos.write(key); 
			byte[] reply = invokeRequestOrdered(out,dos,type, r);
			return reply; 
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return null;
		} 
	}
	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#putIfAbsent(java.lang.String, byte[], byte[], datastore.workloads.RequestLogEntry)
	 */
	public byte[] putIfAbsent(String tableName, byte[] key , byte[] val, RequestLogEntry r) {
		RequestType type = RequestType.ATOMIC_PUT_IF_ABSENT; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeInt(type.ordinal()); 
			dos.writeUTF(tableName);
			dos.writeInt(key.length); 
			dos.write(key); 
			dos.write(val);
			byte[] reply = invokeRequestOrdered(out,dos,type, r);
			return reply; 
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return null;
		} 
	
	}

	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#getAndIncrement(java.lang.String, byte[], datastore.workloads.RequestLogEntry)
	 */
	public byte[] getAndIncrement(String tableName,
			byte[] key, RequestLogEntry r) {
		RequestType type = RequestType.GET_AND_INCREMENT; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);
		
		try {
			dos.writeInt(type.ordinal());
			dos.writeUTF(tableName);
			dos.writeInt(key.length); 
			dos.write(key);
			
			byte[] reply = invokeRequestOrdered(out,dos, type, r);
			return reply;
		}catch(IOException e){
			return null; 
		}
	}

	
	/* (non-Javadoc)
	 * @see datastore.DataStoreInterface#size(java.lang.String, datastore.workloads.RequestLogEntry)
	 */
	public int size(String table, RequestLogEntry r) {
		RequestType type = RequestType.SIZE_OF_TABLE; 
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(out);
		
		try {
			dos.writeInt(type.ordinal());
			dos.writeUTF(table); 
			byte[] reply = invokeRequestOrdered(out,dos, type, r);
			if (reply != null){
				ByteArrayInputStream in = new ByteArrayInputStream(reply);
				DataInputStream dis = new DataInputStream(in);
				return  dis.readInt();
			}
			return -1; 
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return -1; //Well this interface (i.e., Map) was not made for RPC clearly. that is why RMI adds another exception. 
		} 
	}



	
	//////////////////////////////////////////////////
	////////////////PRIVATE METHODS///////////////////
	//////////////////////////////////////////////////
	

	
	private void stop(RequestLogEntry r, int size){
		r.endedNow();
		r.setSizeOfResponse(size);
		benchManager.addRequest(r); 
	}
	

	private byte[] invokeRequestOrdered(ByteArrayOutputStream out,
		DataOutputStream dos, RequestType t, RequestLogEntry r) throws IOException {
		dos.flush(); 
		dos.close();
		
		
		if (r != null)
			start(out, t, r); 
		byte[] reply = clientProxy.invokeOrdered(out.toByteArray());
		if (r != null)
		stop(r,reply != null ? reply.length : 0); //Benchmarking 
		return reply;
	}
		
	/**
	 * @param out
	 * @param t
	 * @param r
	 */
	private void start(ByteArrayOutputStream out, RequestType t,
			RequestLogEntry r) {
		r.setType(t); 
		r.setTimeStarted(System.currentTimeMillis());
		r.setSizeOfRequest(out.size());
		System.out.println(" FINAL   - Total size: " + out.size());
	}


}
