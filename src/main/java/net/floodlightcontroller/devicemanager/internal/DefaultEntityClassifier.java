/**
*    Copyright 2011,2012, Big Switch Networks, Inc. 
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.IEntityClass;
import net.floodlightcontroller.devicemanager.IEntityClassListener;
import net.floodlightcontroller.devicemanager.IEntityClassifierService;
import net.floodlightcontroller.devicemanager.IDeviceService.DeviceField;

/**
 * This is a default entity classifier that simply classifies all
 * entities into a fixed entity class, with key fields of MAC and VLAN.
 * @author readams
 */

class DefaultEntityClass implements IEntityClass, Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String name;

    public DefaultEntityClass(String name) {
        this.name = name;
    }

    protected EnumSet<DeviceField> keyFields= EnumSet.of(DeviceField.MAC, DeviceField.VLAN);

    @Override
    public EnumSet<IDeviceService.DeviceField> getKeyFields() {
        return keyFields;
    }

    @Override
    public String getName() {
        return name;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((keyFields == null) ? 0 : keyFields.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultEntityClass other = (DefaultEntityClass) obj;
		if (keyFields == null) {
			if (other.keyFields != null)
				return false;
		} else if (!keyFields.equals(other.keyFields))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

    
	
    
}

public class DefaultEntityClassifier implements
        IEntityClassifierService,
        IFloodlightModule 
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * A default fixed entity class
     */
    
	protected static EnumSet<DeviceField> keyFields;
    static {
        keyFields = EnumSet.of(DeviceField.MAC, DeviceField.VLAN);
    }
    protected static DefaultEntityClass entityClass =
        new DefaultEntityClass("DefaultEntityClass");

    @Override
    public IEntityClass classifyEntity(Entity entity) {
        return entityClass;
    }

    @Override
    public IEntityClass reclassifyEntity(IDevice curDevice,
                                                     Entity entity) {
        return entityClass;
    }

    @Override
    public void deviceUpdate(IDevice oldDevice, 
                             Collection<? extends IDevice> newDevices) {
        // no-op
    }

    @Override
    public EnumSet<DeviceField> getKeyFields() {
        return keyFields;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l = 
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IEntityClassifierService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>,
        IFloodlightService> m = 
        new HashMap<Class<? extends IFloodlightService>,
                    IFloodlightService>();
        // We are the class that implements the service
        m.put(IEntityClassifierService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>>
            getModuleDependencies() {
        // No dependencies
        return null;
    }

    @Override
    public void init(FloodlightModuleContext context)
                                                 throws FloodlightModuleException {
        // no-op
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        // no-op
    }

    @Override
    public void addListener(IEntityClassListener listener) {
        // no-op
        
    }
}
