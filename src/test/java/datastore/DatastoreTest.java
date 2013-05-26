package datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import mapserver.MapSmart;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class DatastoreTest {
	private static int counter; 
	Datastore ds;
	private Table<String,String> table; 

	public DatastoreTest(){
		ds = new Datastore(); 
	}
	 	
	@BeforeClass 
	public static void startup(){
		MapSmart.main(new String[0]); 	
	}
	
	@Test
	public void testCreateTable() {
		assertTrue(ds.createTable("table_created"));
		assertFalse(ds.createTable("table_created")); //table already existed. Should not allow creation.
	}
	
	@Test 
	public void testCreateTableMaxSize(){
		String tableName="table_created";
		assertTrue(ds.createTable(tableName,2));  
		assertFalse(ds.createTable(tableName,2)); //table already existed. Should not allow creation.
		table = ds.getTable(tableName, null,null);
		table.put( "a", "a"); 
		table.put( "b", "b");
		assertTrue(table.containsKey( "a")); 
		assertTrue(table.containsKey("b"));
		table.put( "c", "c");  //Removes the eldest element a since max size is 2. 
		assertFalse(table.containsKey( "a")); 
		assertTrue(table.containsKey( "b"));
		assertTrue(table.containsKey( "c"));
	}
	@Test
	public void testRemoveTable() {
		assertFalse(ds.removeTable("this_table_does_not_exists")); 
		assertTrue(ds.createTable("table_to_remove"));
		assertTrue(ds.removeTable("table_to_remove"));
	}
	
	@Test
	public void testContainsTable() {
		assertTrue(ds.createTable("test_contains"));
		assertFalse(ds.containsTable("this_table_does_not_exists")); 
		assertTrue(ds.containsTable("test_contains")); 
	}

	
	@Test
	public void testClear() {
		ds.clear();
		assertTrue(ds.isEmpty()); 
	}
	
	@Test
	public void testClearString() {
		assertTrue(ds.createTable("test_clear"));
		table = ds.getTable("test_clear", null,null);
		assertTrue(table.put("test_clear","a"));
		Assert.assertEquals(table.size(),  1);
		ds.clear("test_clear"); 
		Assert.assertEquals((int) table.size(),  0);
	}
	
	@Test
	public void testContainsKey() {
		String tableName = "testContainsKey"; 
		ds.createTable(tableName); 
		table = ds.getTable(tableName, null, null);
		assertFalse(table.containsKey( "c"));
		table.put( "a", "a");
		assertTrue(table.containsKey( "a"));
	}

	@Test
	public void testGetString() {
		String tableName = "testGetString"; 
		ds.createTable(tableName);
		table = ds.getTable(tableName, null, null);
		Assert.assertNotNull(table.getAll());
		Assert.assertEquals(table.getAll().size(), 0); 
		table.put( "a", "a"); 
		Map<String,String> values= table.getAll(); 
		Assert.assertEquals(values.size(), 1);
		Assert.assertTrue(values.containsKey("a")); 
		table.put( "b", "b");
		values = table.getAll(); 
		Assert.assertEquals(values.size(), 2);
		Assert.assertTrue(values.containsKey("b"));
		Assert.assertTrue(values.containsKey("a"));
	}
	
	interface cenas {
		enum a implements Serializable{
			V0, V1
		};
	}; 
	@Test 
	public void enumSetTest(){
		ds.clear(); 
		Table<EnumSet<cenas.a>, EnumSet<cenas.a>> table = ds.getTable("CENAS", null, null);
		assertNotNull(table.getAll());
		table.put(EnumSet.of(cenas.a.V0), EnumSet.of(cenas.a.V1));
		assertNotNull(table.getAll());
	}
	
	@Test
	public void testGetAll(){
		ds.clear(); 
		String tableName = "testGetStringObject"; 
		ds.createTable(tableName);
		table = ds.getTable(tableName, null, null);
		
		// test get All
		
		Assert.assertNotNull(table.getAll());
		Assert.assertEquals(table.getAll().size(), 0);
		table.put("a", "b"); 
		Assert.assertEquals(table.getAll().size(), 1);
		Assert.assertEquals(table.getAll().containsKey("a"), true); 
		Assert.assertTrue(table.getAll().values().contains("b"));
	}
	
	@Test
	public void testGetStringObject() {
		String tableName = "testGetStringObject"; 
		ds.createTable(tableName);
		table = ds.getTable(tableName, null, null);
		assertNull(table.get("a"));
		table.put("a","a_value");
		assertEquals(table.get("a"), "a_value");
	}

	@Test
	public void testIsEmpty() {
		ds.clear(); 
		
		assertTrue(ds.isEmpty());
		ds.createTable("aTable"); 
		assertFalse(ds.isEmpty());
	}

	@Test
	public void testIsEmptyString() {
		String tableName= "testIsEmptyString";
		ds.createTable(tableName); 
		table = ds.getTable(tableName, null	, null);
		assertTrue(table.isEmpty());
		ds.createTable(tableName); 
		table = ds.getTable(tableName, null, null);
		assertTrue(table.isEmpty());
		table.put("some", "bar"); 
		assertFalse(table.isEmpty()); 
	}

	@Test
	public void testPut() {
		String tableName = "put";
		ds.createTable(tableName);
		table = ds.getTable(tableName, null, null);
		assertTrue(table.put( "a", "b"));
		assertEquals(table.get("a"), "b" ); 
	}

	@Test
	public void testPutAll() {
		String tableName ="putAll"; 
		Map<String,String> values = new HashMap<String,String>();
		values.put("1", "1");
		values.put("2", "2");
		ds.createTable(tableName); 
		table = ds.getTable(tableName, null, null);
		table.putAll( values);
		assertEquals(table.get( "2"), "2");
		assertEquals(table.get("1"), "1");
		assertNull(table.get("3"));
		assertEquals((int) table.size(), 2);
	}

	@Test
	public void testRemove() {
		String tableName= "table2RemoveFrom"; 
		ds.createTable(tableName); 
		table = ds.getTable(tableName, null, null);
		table.put( "1", "1"); 
		assertEquals(table.remove( "1"), "1");
		assertNull(table.remove( "1"));
		assertNull(table.get("1"));
	}

	@Test
	public void testSize() {
		ds.createTable("size");
		table = ds.getTable("sizes", null, null);
		assertEquals(table.size(), 0);
		table.put("1", "1");
		assertEquals(table.size(), 1);
		table.put("2", "2");
		assertEquals(table.size(), 2);
	}
	
	
	@Test 
	public void testAtomicREplace(){
		ds.createTable("atomicReplace"); table = ds.getTable("atomicReplace", null, null); 
		
		assertFalse(table.replace("a", "old", "new")); //should not replace, "a" is not mapped to  nothing
		assertFalse(table.replace("a", "old", "new"));// still.. does not replace. 
		assertFalse(table.containsKey("a")); //should not contain
		
		
		table.put("a", "v0");
		assertTrue(table.replace("a", "v0", "v1")); 
		assertEquals(table.get("a"), "v1"); 
		assertFalse(table.replace("a", "v0", "v2"));
		assertEquals(table.get("a"), "v1"); 
	}
	@Test 
	public void testAtomicREmove(){
		ds.clear(); 
ds.createTable("atomicReplace"); table = ds.getTable("atomicReplace", null, null); 
		
		
		assertFalse(table.remove("a", "old")); //should not remove, "a" is not mapped to  nothing
		assertFalse(table.remove("a", "old"));// still.. does not remove. 
		assertFalse(table.containsKey("a")); //should not contain
		
		
		table.put("a", "v0");
		assertTrue(table.remove("a", "v0")); 
		assertFalse(table.containsKey("a"));
	}
	
	@Test
	public void testAtomicPUTIfAbsent(){
		ds.clear();ds.createTable("atomicReplace"); table = ds.getTable("atomicReplace", null, null); 
		
		assertNull(table.putIfAbsent("a", "old")); // no value there before
		assertEquals(table.putIfAbsent("a", "old"), "old");// still.. does not remove. 
		assertTrue(table.containsKey("a")); //should not contain


	}
	
	
	@After
	public  void cleantable(){
		ds.clear(); 
		assertTrue(ds.isEmpty());
		
	}

}
