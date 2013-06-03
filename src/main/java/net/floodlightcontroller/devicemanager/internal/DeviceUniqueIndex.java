/**
*    Copyright 2012 Big Switch Networks, Inc. 
*    Originally created by David Erickson, Stanford University
* 
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package net.floodlightcontroller.devicemanager.internal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

import net.floodlightcontroller.devicemanager.IDeviceService.DeviceField;

import com.google.common.collect.Maps;

import datastore.Datastore;
import datastore.Table;

/**
 * An index that maps key fields of an entity uniquely to a device key
 */
public class DeviceUniqueIndex extends DeviceIndex {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
     * The index
     */
    //private ConcurrentHashMap<IndexedEntity, Long> index;
    private Table<IndexedEntity, Long> index; 
    
    /**
     * Construct a new device index using the provided key fields
     * @param keyFields the key fields to use
     */
    public DeviceUniqueIndex(EnumSet<DeviceField> keyFields, Datastore ds) {
        super(keyFields);
        Table<String,String> table = ds.getTable(Datastore.CONTROLLERS_SYSTEM_INFO, null	,null); 
        //TODO - initialization of counters
        String tableId;
        for (tableId =  table.get("DEVICE_UNIQUE_INDEX_COUNTER") ; table.replace("DEVICE_UNIQUE_INDEX_COUNTER", tableId,	tableId+1); tableId = (Long.decode(tableId)+ 1) + ""  ){
        	continue; 
        }
        index = ds.getTable("DEVICE_UNIQUE_INDEX_" + tableId, datastore.util.KeySerializationFunctions.INDEXED_ENTITY_DESERIALIZE	, datastore.util.KeySerializationFunctions.INDEXED_ENTITY_SERIALIZE);
        
        
    }

    // ***********
    // DeviceIndex
    // ***********

    @Override
    public Iterator<Long> queryByEntity(Entity entity) {
        final Long deviceKey = findByEntity(entity);
        if (deviceKey != null)
            return Collections.<Long>singleton(deviceKey).iterator();
        
        return Collections.<Long>emptySet().iterator();
    }
    
    @Override
    public Iterator<Long> getAll() {
        return index.getAll().values().iterator();
    }

    private Map<IndexedEntity, Boolean> semiBloom = Maps.newHashMap(); 
    @Override
    public boolean updateIndex(Device device, Long deviceKey) {
    	
    	
    	//TODO - bloom filter here.
    	//TODO - effects of non consistency entities? Quite sure no one ever deletes entities except in deleteDevice and cleanUp 
    	
        for (Entity e : device.entities) {
            IndexedEntity ie = new IndexedEntity(keyFields, e);
            if (!ie.hasNonNullKeys() || semiBloom.containsKey(ie)) continue;
            semiBloom.put(ie, true);
            Long ret = index.putIfAbsent(ie, deviceKey);
            if (ret != null && !ret.equals(deviceKey)) {
                // If the return value is non-null, then fail the insert 
                // (this implies that a device using this entity has 
                // already been created in another thread).
                return false;
            }
        }
        semiBloom.clear(); 
        return true;
    }
    
    @Override
    public void updateIndex(Entity entity, Long deviceKey) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        if (!ie.hasNonNullKeys()) return;
        index.put(ie, deviceKey);
    }

    @Override
    public void removeEntity(Entity entity) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        index.remove(ie);
    }

    @Override
    public void removeEntity(Entity entity, Long deviceKey) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        index.remove(ie, deviceKey);
    }

    // **************
    // Public Methods
    // **************

    /**
     * Look up a {@link Device} based on the provided {@link Entity}.
     * @param entity the entity to search for
     * @return The key for the {@link Device} object if found
     */
    public Long findByEntity(Entity entity) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        Long deviceKey = index.get(ie);
        if (deviceKey == null)
            return null;
        return deviceKey;
    }

}
