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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.devicemanager.IDeviceService.DeviceField;
import net.floodlightcontroller.util.IterableIterator;
import bonafide.datastore.ColumnProxy;
import bonafide.datastore.KeyValueProxy;
import bonafide.datastore.tables.AnnotatedColumnObject;
import bonafide.datastore.tables.KeyValueTable_;
import bonafide.datastore.util.JavaSerializer;

import com.google.common.collect.Sets;

import datastore.Datastore;
import datastore.Table;

/**
 * An index that maps key fields of an entity to device keys, with multiple
 * device keys allowed per entity
 */
public class DeviceMultiIndex extends DeviceIndex {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
     * The index
     */
    private KeyValueTable_<IndexedEntity, HashSet<Long>> index;

    /**
     * @param keyFields
     */
    
    public DeviceMultiIndex(EnumSet<DeviceField> keyFields, Datastore ds) {
        super(keyFields);
        index = KeyValueTable_.getTable(
        		new KeyValueProxy((int) Thread.currentThread().getId())
        		, "DEVICE_MULTI_INDEX_" + getControllerID(),  
        		IndexedEntity.SERIALIZER, 
        		JavaSerializer.<HashSet<Long>>getJavaSerializer()); 
    }

    // ***********
    // DeviceIndex
    // ***********

    /**
	 * @return
	 */
	private String getControllerID() {
		return "Id"; 
	}

	@Override
    public Iterator<Long> queryByEntity(Entity entity) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        Collection<Long> devices = index.get(ie);
        if (devices != null)
            return devices.iterator();
        
        return Collections.<Long>emptySet().iterator();
    }
    
    @Override
    public Iterator<Long> getAll() {
        Iterator<HashSet<Long>> iter = index.values().iterator();
        return new IterableIterator<Long>(iter);
    }
    
    @Override
    public boolean updateIndex(Device device, Long deviceKey) {
        for (Entity e : device.getEntities()) {
            updateIndex(e, deviceKey);
        }
        return true;
    }
    
    @Override
    public void updateIndex(Entity entity, Long deviceKey) {
        HashSet<Long> devices = null;

        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        if (!ie.hasNonNullKeys()) return;

        devices = index.get(ie);
        if (devices == null) {
            Map<Long,Boolean> chm = new ConcurrentHashMap<Long,Boolean>();
            devices = Sets.newHashSet(Collections.newSetFromMap(chm));
            devices.add(deviceKey);
            HashSet<Long> r = index.putIfAbsent(ie, devices);
            //TODO - race condition devices may have been created meanwhile. 
            return; 
        }
        devices.add(deviceKey);
        index.put(ie, devices); 
    }

    @Override
    public void removeEntity(Entity entity) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        index.remove(ie);        
    }

    @Override
    public void removeEntity(Entity entity, Long deviceKey) {
        IndexedEntity ie = new IndexedEntity(keyFields, entity);
        Collection<Long> devices = index.get(ie);
        if (devices != null)
            devices.remove(deviceKey);
    }
}
