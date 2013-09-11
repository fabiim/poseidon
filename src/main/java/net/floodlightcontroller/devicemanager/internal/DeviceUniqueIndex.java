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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

import net.floodlightcontroller.devicemanager.IDeviceService.DeviceField;
import bonafide.datastore.ColumnProxy;
import bonafide.datastore.tables.AnnotatedColumnObject;
import bonafide.datastore.tables.ColumnTable_;
import bonafide.datastore.util.JavaSerializer;

import com.google.common.collect.Maps;

import datastore.Datastore;
import datastore.Table;

/**
 * An index that maps key fields of an entity uniquely to a device key
 */
public class DeviceUniqueIndex {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected EnumSet<DeviceField> keyFields;
	/**
     * The index
     */
    //private ConcurrentHashMap<IndexedEntity, Long> index;
    private ColumnTable_<IndexedEntity, Device> index; 
    
    /**
     * Construct a new device index using the provided key fields
     * @param keyFields the key fields to use
     */
    public DeviceUniqueIndex(EnumSet<DeviceField> keyFields, Datastore ds) {
        this.keyFields = keyFields; 
        index = ColumnTable_.getTable(
        		new ColumnProxy((int) Thread.currentThread().getId())
        		, "DEVICE_UNIQUE_INDEX" + getControllerID(),  
        		IndexedEntity.SERIALIZER, 
        		AnnotatedColumnObject.newAnnotatedColumnObject(Device.class)); 
    }
    
    
    
    // ***********
    // DeviceIndex
    // ***********

    
    /**
	 * @return
	 */
	private String getControllerID() {
		//FIXME: 
		return  "CURRENT_CONTROLLER"; 
	}

	public Iterator<Device> queryByEntity(Entity entity) {
        final Device deviceKey = findByEntity(entity);
        if (deviceKey != null)
            return Collections.<Device>singleton(deviceKey).iterator();
        
        return Collections.<Device>emptySet().iterator();
    }
    
    
    public Iterator<Device> getAll() {
    	return index.values().iterator(); 
    }
    
    private Map<IndexedEntity, Boolean> semiBloom = Maps.newHashMap(); 
    
    public boolean updateIndex(Device device) {
    	
    	
    	//TODO - bloom filter here.
    	//TODO - effects of non consistency entities? Quite sure no one ever deletes entities except in deleteDevice and cleanUp 
    	
        for (Entity e : device.getEntities()) {
            IndexedEntity ie = new IndexedEntity(keyFields, e);
            if (!ie.hasNonNullKeys() || semiBloom.containsKey(ie)) continue;
            semiBloom.put(ie, true);
            Device ret = index.putIfAbsent(ie, device);
            if (ret != null && !ret.equals(device)) {
                // If the return value is non-null, then fail the insert 
                // (this implies that a device using this entity has 
                // already been created in another thread).
                return false;
            }
        }
        return true;
    }
    
    
    public void updateIndex(Entity entity, Device deviceKey) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        if (!ie.hasNonNullKeys()) return;
        index.put(ie, deviceKey);
    }


    public void removeEntity(Entity entity) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        index.remove(ie);
    }

    
    public void removeEntity(Entity entity, Device deviceKey) {
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
    public Device findByEntity(Entity entity) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        Device deviceKey = index.get(ie);
        if (deviceKey == null)
            return null;
        return deviceKey;
    }
    
    public boolean removeEntityIfNeeded(Entity entity, Device device,
    		Collection<Entity> others) {
    	IndexedEntity ie = new IndexedEntity(keyFields, entity);
    	for (Entity o : others) {
    		IndexedEntity oio = new IndexedEntity(keyFields, o);
    		if (oio.equals(ie)) return false;
    	}

    	boolean changed = false;
    	Iterator<Device> keyiter = this.queryByEntity(entity);
    	while (keyiter.hasNext()) {
    		Long key = keyiter.next().getDeviceKey();
    		
    		if (key.equals(device.getDeviceKey())) {
    			removeEntity(entity, device);
    			changed = true; 
    			break;
    		}
    	}
    	return changed ; 
    }

}
