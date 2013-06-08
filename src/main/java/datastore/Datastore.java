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
import org.python.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import bftsmart.tom.ServiceProxy;
import datastore.workloads.RequestLogEntry;
import datastore.workloads.logger.RequestLogger;

/**
 * @author fabiim
 *
 */

public class Datastore{

	public static final String DEVICE_MANAGER_LAST_COUNTER = "DEVICE_MANAGER_LAST_COUNTER"; 
	public static void initDatastoreFromController(Datastore ds){
		Table<String, Long>  t = ds.getTable(Datastore.CONTROLLERS_SYSTEM_INFO, datastore.util.KeySerializationFunctions.STRING_DESERIALIZE	, datastore.util.KeySerializationFunctions.STRING_SERIALIZE); 
		
		//DeviceIndex 
		if (!t.put(Datastore.DEVICE_MANAGER_LAST_COUNTER, 0L)){
			System.err.println("Device Manager Last Counter was initiliazed"); 
		}

		Table<String,String>  t2 = ds.getTable(Datastore.CONTROLLERS_SYSTEM_INFO, null, null); 
		t2.put("DEVICE_UNIQUE_INDEX_COUNTER", "0");
		t2.put("DEVICE_MULTI_INDEX_COUNTER", "0"); 
		//TODO - several copies of the datastore locally (é preciso aproveitar as threads) . 
		//TODO - cada uma das aplicações inicializa as cenas .
		
	}

	
	public static final String CONTROLLERS_SYSTEM_INFO = "CONTROL_SYSTEM_INFO";
	protected static Logger log = LoggerFactory.getLogger(TopologyManager.class);

	public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }
	
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
	
	
	public RequestLogger getBenchManager(){ return benchManager; }
	
	// Correct me .
	public <K extends Serializable,A extends Serializable> Table<K,A> getTable(String tableName, Function<byte[],K> deserialize, Function<K,byte[]> serialize){
		boolean created = true; 
		if (!this.containsTable(tableName,new RequestLogEntry(tableName))){ 
			created =this.createTable(tableName, new RequestLogEntry(tableName));
			log.info("creating a table: " + tableName + " Result : " + created);
		}
		return created ? new Table<K,A>(this, tableName, deserialize,serialize): null;   
	}
	

	public  <K extends Serializable,A extends Serializable> Table<K, A> getTableL(String tableName,
			Function<byte[], K> deserialize,
			Function<K, byte[]> serialize) {
		boolean created = true; 
		if (!this.containsTable(tableName,new RequestLogEntry(tableName))){ 
			created =this.createTable(tableName, new RequestLogEntry(tableName));
			log.info("creating a table: " + tableName + " Result : " + created);
		}
		return created ? new TableLearningSwitch<K,A>(this, tableName, deserialize,serialize): null;   
	
	}

	
	
	 /*************************************************
	  * General Functions: 
	  * Functions who do not address specific tables.
	  */
	
	 /**
	  * Create a table in the datastore. 
	  * @param tableName
	  * @return false if table exists.  
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
	 
	 /**
	  * 
	  * @param tableName
	  * @return false if table does not exists, or server communication has not completed correctly. 
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
	 
	 /**
	  * Check if the datastore contains a table
	  * @param tableName The name of the table. 
	  * @return 
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
	
	/**
	 * Clear a specified table. 
	 * @param table The table to be cleared. 
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
	
	/**
	 * Check if a table contains a key. 
	 * @param table
	 * @param key
	 * @param requestLogEntry 
	 * @return
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
			byte[] reply = invokeRequestOrdered(out,dos, type, r);
			return reply != null;
		} catch (IOException e) {
			log.error("Exception getting values from datastore: " + e.getMessage());
			return false;
		} 
	}
	
	
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
	}


}
