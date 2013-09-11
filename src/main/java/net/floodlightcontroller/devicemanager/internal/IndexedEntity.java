/**
 *    Copyright 2013, Big Switch Networks, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;

import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.IDeviceService.DeviceField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bonafide.datastore.util.Serializer;

import com.google.common.base.Function;


/**
 * This is a thin wrapper around {@link Entity} that allows overriding
 * the behavior of {@link Object#hashCode()} and {@link Object#equals(Object)}
 * so that the keying behavior in a hash map can be changed dynamically
 * @author readams
 */
public class IndexedEntity implements Serializable {
	public static Serializer<IndexedEntity> SERIALIZER   = new  Serializer<IndexedEntity>(){
		

		/* (non-Javadoc)
		 * @see bonafide.datastore.util.Serializer#serialize(java.lang.Object)
		 */
		@Override
		public byte[] serialize(IndexedEntity c) {
			try {
		        ByteArrayOutputStream b = new ByteArrayOutputStream();
		        ObjectOutputStream out = new ObjectOutputStream(b);
		
				out.writeObject(c.getKeyFields());
				for (DeviceField f : c.getKeyFields()){
					
					switch(f){
					case IPV4:
						out.writeObject(c.getEntity().getIpv4Address());
						break;
					case MAC:
						out.writeObject(new Long(c.getEntity().getMacAddress()));
						break;
					case PORT:
						out.writeObject(c.getEntity().getSwitchPort());
						break;
					case SWITCH:
						out.writeObject(c.getEntity().getSwitchDPID());
						break;
					case VLAN:
						out.writeObject(c.getEntity().getVlan());
						break;
					}
					
				}
				out.flush(); 
				out.close();
				return b.toByteArray(); 
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null; 
	}


		/* (non-Javadoc)
		 * @see bonafide.datastore.util.Serializer#deserialize(byte[])
		 */
		@Override
		public IndexedEntity deserialize(byte[] c) {
			try {
		        ByteArrayInputStream b = new ByteArrayInputStream(c);
		        ObjectInputStream o = new ObjectInputStream(b);
				EnumSet<DeviceField> fields = (EnumSet<DeviceField>) o.readObject();
				IndexedEntity ie = new IndexedEntity();
				ie.setKeyFields(fields);
				Long macAddress=null;  
				Short vlan =null;
		        Integer ipv4Address=null; 
				Long switchDPID = null; 
				Integer switchPort = null; 
		        Date lastSeenTimestamp =null;
				for (DeviceField f: fields){
					switch(f){
					case IPV4:
						ipv4Address = (Integer) o.readObject(); 
						break;
					case MAC:
						macAddress = (Long) o.readObject(); 
						break;
					case PORT:
						switchPort = (Integer) o.readObject(); 
						break;
					case SWITCH:
						switchDPID = (Long) o.readObject(); 
						break;
					case VLAN:
						vlan = (Short) o.readObject(); 
						break;
					
					}
				}
				Entity e = new Entity(macAddress, vlan, ipv4Address, switchDPID, switchPort, lastSeenTimestamp);
				ie.setEntity(e);
				return ie; 
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null; 
		}

	
	};
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	protected EnumSet<DeviceField> keyFields;
    protected Entity entity;

    
    @Override
	public String toString() {
		return "IndexedEntity [keyFields=" + printKeyField(keyFields) + ", entity=" + entity
				+ ", hashCode=" + hashCode + "]";
	}

	private String printKeyField(EnumSet<DeviceField> keyFields2) {
		StringBuilder s = new StringBuilder(); 
		
		Iterator<DeviceField> it = keyFields2.iterator(); 
		while (it.hasNext()){
			s.append(it.next()); 
			if (it.hasNext())
				s.append(","); 
		}
		return s.toString(); 
	}

	private int hashCode = 0;
    
    
    public EnumSet<DeviceField> getKeyFields() {
		return keyFields;
	}

	public void setKeyFields(EnumSet<DeviceField> keyFields) {
		this.keyFields = keyFields;
	}

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	protected static Logger logger =
            LoggerFactory.getLogger(IndexedEntity.class);
    /**
     * Create a new {@link IndexedEntity} for the given {@link Entity} using 
     * the provided key fields.
     * @param keyFields The key fields that will be used for computing
     * {@link IndexedEntity#hashCode()} and {@link IndexedEntity#equals(Object)}
     * @param entity the entity to wrap
     */
    public IndexedEntity(EnumSet<DeviceField> keyFields, Entity entity) {
        super();
        this.keyFields = keyFields;
        this.entity = entity;
    }

    public IndexedEntity() {
		// TODO Auto-generated constructor stub
	}

	/**
     * Check whether this entity has non-null values in any of its key fields
     * @return true if any key fields have a non-null value
     */
    public boolean hasNonNullKeys() {
        for (DeviceField f : keyFields) {
            switch (f) {
                case MAC:
                    return true;
                case IPV4:
                    if (entity.ipv4Address != null) return true;
                    break;
                case SWITCH:
                    if (entity.switchDPID != null) return true;
                    break;
                case PORT:
                    if (entity.switchPort != null) return true;
                    break;
                case VLAN:
                    if (entity.vlan != null) return true;
                    break;
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
    	
        if (hashCode != 0) {
        	return hashCode;
        }

        final int prime = 31;
        hashCode = 1;
        for (DeviceField f : keyFields) {
            switch (f) {
                case MAC:
                    hashCode = prime * hashCode
                        + (int) (entity.macAddress ^ 
                                (entity.macAddress >>> 32));
                    break;
                case IPV4:
                    hashCode = prime * hashCode
                        + ((entity.ipv4Address == null) 
                            ? 0 
                            : entity.ipv4Address.hashCode());
                    break;
                case SWITCH:
                    hashCode = prime * hashCode
                        + ((entity.switchDPID == null) 
                            ? 0 
                            : entity.switchDPID.hashCode());
                    break;
                case PORT:
                    hashCode = prime * hashCode
                        + ((entity.switchPort == null) 
                            ? 0 
                            : entity.switchPort.hashCode());
                    break;
                case VLAN:
                    hashCode = prime * hashCode 
                        + ((entity.vlan == null) 
                            ? 0 
                            : entity.vlan.hashCode());
                    break;
            }
        }
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
       if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        IndexedEntity other = (IndexedEntity) obj;
        
        if (!keyFields.equals(other.keyFields))
            return false;

        for (IDeviceService.DeviceField f : keyFields) {
            switch (f) {
                case MAC:
                    if (entity.macAddress != other.entity.macAddress)
                        return false;
                    break;
                case IPV4:
                    if (entity.ipv4Address == null) {
                        if (other.entity.ipv4Address != null) return false;
                    } else if (!entity.ipv4Address.
                            equals(other.entity.ipv4Address)) return false;
                    break;
                case SWITCH:
                    if (entity.switchDPID == null) {
                        if (other.entity.switchDPID != null) return false;
                    } else if (!entity.switchDPID.
                            equals(other.entity.switchDPID)) return false;
                    break;
                case PORT:
                    if (entity.switchPort == null) {
                        if (other.entity.switchPort != null) return false;
                    } else if (!entity.switchPort.
                            equals(other.entity.switchPort)) return false;
                    break;
                case VLAN:
                    if (entity.vlan == null) {
                        if (other.entity.vlan != null) return false;
                    } else if (!entity.vlan.
                            equals(other.entity.vlan)) return false;
                    break;
            }
        }
        
        return true;
    }
    
    
}
