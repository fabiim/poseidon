/**
 *    Copyright 2011,2012 Big Switch Networks, Inc.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService.DeviceField;
import net.floodlightcontroller.devicemanager.IEntityClass;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.devicemanager.SwitchPort.ErrorStatus;
import net.floodlightcontroller.devicemanager.web.DeviceSerializer;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.topology.ITopologyService;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bonafide.datastore.tables.Column;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import datastore.column.TrackableColumns;

/**
 * Concrete implementation of {@link IDevice}
 * @author readams
 */

/*
 * Changes: 
 *  	* Avoiding direct referencing of private fields in this implementation was done only for ease of programming correctness while developing the datastore columns feature.
 *  
 * Cluttering: 
 *      * XXX A lot of logic based on collection fields being null. That creeps me out.  
 */
@JsonSerialize(using=DeviceSerializer.class)
public final class Device implements IDevice,Serializable{
	
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/*
	 * Shared map to optimize space usage in device instances. 
	 * Devices used to keep a reference to the entityClass used to classify them. 
	 * Now, with this optimization instances keep a string reference to the shared map with all EntityClasses known. 
	 * 
	 * TODO At the moment i can only guess that different controllers instances would have the same EntityClass, but maybe
	 * we would have to have some service to deploy and start different entityclasses making sure that they would be shared through the datastore.
	 * 
	 * XXX Maybe this is not necessary per se. Something like keeping reference in the DeviceManager could be cleaner even if worse in space complexity (Entity -> Devices map)
	 * 	   Also, not sure how much relevant this info is for device manager. Check newest floodlight source, where it is used etc,
	 * 
	 *  @see #entityClass 
	 */
	private static Map<String,IEntityClass> entitiesClasses = Maps.newConcurrentMap();
	
	//private String entityClassName;
	
	public static DeviceManagerImpl deviceManager; 
	protected static Logger log =
            LoggerFactory.getLogger(Device.class);

	// Changed from original source: DeviceManagerImpl is addressed statically to optimize space. 
	// This was actually done first because I wanted to serialize the Device class and I had not learned the transient property at the time to avoid marshalling this reference. 
	public static DeviceManagerImpl getDeviceManager() {
			return deviceManager;
	}
	
	public static void setDeviceManager(DeviceManagerImpl deviceManager) {
			Device.deviceManager = deviceManager;
	}
	



    private Long deviceKey;
    
    private Entity[] entities;
    
    private String entityClass;
    
    private String macAddressString;
    // the vlan Ids from the entities of this device
    private Short[] vlanIds;
    private String dhcpClientName;
    
    
    public Device(){
    	
    }
    
    
    /**
     * These are the old attachment points for the device that were
     * valid no more than INACTIVITY_TIME ago.
     */
    private List<AttachmentPoint> oldAPs;
    /**
     * The current attachment points for the device.
     */
    private List<AttachmentPoint> attachmentPoints;
  
    // ************
    // Constructors
    // ************

    /**
     * Create a device from an entities
     * @param deviceManager the device manager for this device
     * @param deviceKey the unique identifier for this device object
     * @param entity the initial entity for the device
     * @param entityClass the entity classes associated with the entity
     */
    public Device(Long deviceKey,
                  Entity entity,
                  IEntityClass entityClass) {

    	setEntityClass(entityClass); 
        setDeviceKey(deviceKey);
        setEntities(new Entity[] {entity});
        setMACAddressString(HexString.toHexString(entity.getMacAddress(), 6));
        setEntityClass(entityClass.getName());
        
        //Commented out from original source:  Doesn't entities contains one element only?  
        //Arrays.sort(this.entities);

        setDhcpClientName(null);
        setOldAPs(null);
        setAttachmentPoints(null);

        if (entity.getSwitchDPID() != null &&
                entity.getSwitchPort() != null){
            long sw = entity.getSwitchDPID();
            short port = entity.getSwitchPort().shortValue();

            if (deviceManager.isValidAttachmentPoint(sw, port)) {
                AttachmentPoint ap;
                ap = new AttachmentPoint(sw, port,entity.getLastSeenTimestamp().getTime());
                setAttachmentPoints(new ArrayList<AttachmentPoint>());
                addAttachmentPoint(ap);
            }
        }
        computeVlandIds();
    }
	
	/**
     * Create a device from a set of entities
     * @param deviceManager the device manager for this device
     * @param deviceKey the unique identifier for this device object
     * @param entities the initial entities for the device
     * @param entityClass the entity class associated with the entities
     */
    public Device(Long deviceKey,
                  String dhcpClientName,
                  Collection<AttachmentPoint> oldAPs,
                  Collection<AttachmentPoint> attachmentPoints,
                  Collection<Entity> entities,
                  IEntityClass entityClass) {

        setDeviceKey( deviceKey);
        setDhcpClientName(dhcpClientName);
        setEntities(entities.toArray(new Entity[entities.size()]));
        setOldAPs(null);
        setAttachmentPoints( null);
        if (oldAPs != null) {
            setOldAPs(
                    new ArrayList<AttachmentPoint>(oldAPs));
        }
        if (attachmentPoints != null) {
            setAttachmentPoints(
                    new ArrayList<AttachmentPoint>(attachmentPoints));
        }
        
        setMACAddressString (
                HexString.toHexString(this.getEntity(0).getMacAddress(), 6));
        setEntityClass( entityClass.getName());
        
        setEntityClass(entityClass);
        Arrays.sort(this.entities);
        computeVlandIds();
    }

    /**
     * Construct a new device consisting of the entities from the old device
     * plus an additional entity.
     * The caller needs to ensure that the additional entity is not already 
     * present in the array
     * @param device the old device object
     * @param newEntity the entity to add. newEntity must be have the same
     *        entity class as device
     * @param insertionpoint if positive indicates the index in the entities array were the
     *        new entity should be inserted. If negative we will compute the
     *        correct insertion point
     */
    public Device(Device device,
                  Entity newEntity,
                  int insertionpoint) {
        setDeviceKey(device.getDeviceKey());
        setDhcpClientName(device.getDhcpClientName()); 
        
        Entity[] srcEntities = device.getEntities();
		Entity[] dstEntities = new Entity[srcEntities.length + 1]; 
        
        
        if (insertionpoint < 0) {
            insertionpoint = -(Arrays.binarySearch(srcEntities, 
                                                   newEntity)+1);
        }
        
        
        if (insertionpoint > 0) {
            // insertion point is not the beginning:
            // copy up to insertion point
        	
        	System.arraycopy(srcEntities, 0, 
                             dstEntities, 0,
                             insertionpoint);
        }
        
        if (insertionpoint < srcEntities.length) {
            // insertion point is not the end 
            // copy from insertion point
            System.arraycopy(srcEntities, insertionpoint, 
                             dstEntities, insertionpoint+1,
                             srcEntities.length-insertionpoint);
        }
        dstEntities[insertionpoint] = newEntity;
        setEntities(dstEntities);
        
        /*
        this.entities = Arrays.<Entity>copyOf(device.entities,
                                              device.entities.length + 1);
        this.entities[this.entities.length - 1] = newEntity;
        Arrays.sort(this.entities);
        */
        setOldAPs(null);
        
        if (device.getOldAPs() != null) {
            setOldAPs(
                    new ArrayList<AttachmentPoint>(device.getOldAPs()));
        }
        setAttachmentPoints(null);
        if (device.getAps() != null) {

            setAttachmentPoints( 
                    new ArrayList<AttachmentPoint>(device.getAps()));
        }

        setMACAddressString(
                HexString.toHexString(this.getEntity(0).getMacAddress(), 6));

        setEntityClass(device.getEntityClass());
        computeVlandIds();
    }
    

    
    /*
     * Deep copy constructor.
     * Actually it only deep copys entities right now... XXX 
     * @param device the device to clone 
     * TODO do not think i need this anymore 
     */
	public Device(Device device) {
		setAttachmentPoints(device.getAps());
		setDeviceKey(device.getDeviceKey());
		setDhcpClientName(device.getDhcpClientName());
		setEntityClass(device.getEntityClass());
		setMACAddressString(device.getMACAddressString());
		setOldAPs(device.getOldAPs());
		setVlanIds(device.getVlanId());

		if (device.getEntities() != null){
			Entity[] entities = Arrays.copyOf(device.getEntities(), device.getEntities().length); 
			for (int i = 0 ; i < entities.length ; i++){
				entities[i] = entities[i].clone(); 
			}
			setEntities(entities); 
		}
		// ?????? else device.entities = null; 
	}

	/*XXX interface getter uses MAC naming 
	 * public String getMacAddressString() {
		return macAddressString;
	}
*/
	
	/// Getters and Setters 
	
	public void setMACAddressString(String macAddressString) {
		this.macAddressString = macAddressString;
	}
	@Column
	public Short[] getVlanIds() {
		return vlanIds;
	}
	
	public void setVlanIds(Short[] vlanIds) {
		this.vlanIds = vlanIds;
	}
	@Column
	public String getDhcpClientName() {
		return dhcpClientName;
	}

	public void setDhcpClientName(String dhcpClientName) {
		this.dhcpClientName = dhcpClientName;
	}

	@Column
	public List<AttachmentPoint> getOldAPs() {
		return oldAPs;
	}

	public void setOldAPs(List<AttachmentPoint> oldAPs) {
		this.oldAPs = oldAPs;
	}
	
	public static EnumSet<DeviceField> getIpv4fields() {
		return ipv4Fields;
	}

	public void setDeviceKey(Long deviceKey) {
		this.deviceKey = deviceKey;
	}

	
	public void setEntityClass(String entityClass) {
		this.entityClass = entityClass;
	}

	public void setAttachmentPoints(List<AttachmentPoint> attachmentPoints) {
		this.attachmentPoints = attachmentPoints;
	}
	
	public void addAttachmentPoint(AttachmentPoint ap){
		attachmentPoints.add(ap); 
	}
	
	
	private void computeVlandIds() {
		Entity[] entities = getEntities(); 
        if (entities.length == 1) {
        	Short[] vlanIds;
            if (entities[0].getVlan() != null) {
                vlanIds = new Short[]{ entities[0].getVlan() };
                
            } else {
                vlanIds = new Short[] { Short.valueOf((short)-1) };
            }
            setVlanIds(vlanIds);
        }

        TreeSet<Short> vals = new TreeSet<Short>();
        for (Entity e : entities) {
            if (e.getVlan() == null)
                vals.add((short)-1);
            else
                vals.add(e.getVlan());
        }
        setVlanIds(vals.toArray(new Short[vals.size()]));
    }


    /**
     * Given a list of attachment points (apList), the procedure would return
     * a map of attachment points for each L2 domain.  L2 domain id is the key.
     * XXX Not sure why i am not static . 
     * @param apList
     * @return
     */
    private  Map<Long, AttachmentPoint> getAPMap(List<AttachmentPoint> apList) {

    	if (apList == null) return null;
    	ITopologyService topology = deviceManager.topology; 
        // Get the old attachment points and sort them.
        List<AttachmentPoint>oldAP = new ArrayList<AttachmentPoint>();
        if (apList != null) oldAP.addAll(apList);

        // Remove invalid attachment points before sorting.
        List<AttachmentPoint>tempAP =
                new ArrayList<AttachmentPoint>();
        for(AttachmentPoint ap: oldAP) {
            if (deviceManager.isValidAttachmentPoint(ap.getSw(), ap.getPort())){
                tempAP.add(ap);
            }
        }
        oldAP = tempAP;

        Collections.sort(oldAP, deviceManager.apComparator);

        // Map of attachment point by L2 domain Id.
        Map<Long, AttachmentPoint> apMap = new HashMap<Long, AttachmentPoint>();

        for(int i=0; i<oldAP.size(); ++i) {
            AttachmentPoint ap = oldAP.get(i);
            // if this is not a valid attachment point, continue
            if (!deviceManager.isValidAttachmentPoint(ap.getSw(),
                                                      ap.getPort()))
                continue;

            long id = topology.getL2DomainId(ap.getSw());
            apMap.put(id, ap);
        }

        if (apMap.isEmpty()) return null;
        return apMap;
    }

    /**
     * Remove all attachment points that are older than INACTIVITY_INTERVAL
     * from the list.
     * XXX not sure why i am not static. 
     * @param apList
     * @return
     */
    private boolean removeExpiredAttachmentPoints(List<AttachmentPoint>apList) {

        List<AttachmentPoint> expiredAPs = new ArrayList<AttachmentPoint>();

        if (apList == null) return false;

        for(AttachmentPoint ap: apList) {
            if (ap.getLastSeen() + AttachmentPoint.INACTIVITY_INTERVAL <
                    System.currentTimeMillis())
                expiredAPs.add(ap);
        }
        if (expiredAPs.size() > 0) {
            apList.removeAll(expiredAPs);
            return true;
        } else return false;
    }

    /**
     * Get a list of duplicate attachment points, given a list of old attachment
     * points and one attachment point per L2 domain. Given a true attachment
     * point in the L2 domain, say trueAP, another attachment point in the
     * same L2 domain, say ap, is duplicate if:
     * 1. ap is inconsistent with trueAP, and
     * 2. active time of ap is after that of trueAP; and
     * 3. last seen time of ap is within the last INACTIVITY_INTERVAL
     * XXX someone forgot about my modifier
     * XXX not sure why i am not static 
     * @param oldAPList
     * @param apMap
     * @return
     */
    List<AttachmentPoint> getDuplicateAttachmentPoints(List<AttachmentPoint>oldAPList,
                                                       Map<Long, AttachmentPoint>apMap) {
        ITopologyService topology = deviceManager.topology;
        List<AttachmentPoint> dupAPs = new ArrayList<AttachmentPoint>();
        long timeThreshold = System.currentTimeMillis() -
                AttachmentPoint.INACTIVITY_INTERVAL;

        if (oldAPList == null || apMap == null)
            return dupAPs;

        for(AttachmentPoint ap: oldAPList) {
            long id = topology.getL2DomainId(ap.getSw());
            AttachmentPoint trueAP = apMap.get(id);

            if (trueAP == null) continue;
            boolean c = (topology.isConsistent(trueAP.getSw(), trueAP.getPort(),
                                              ap.getSw(), ap.getPort()));
            boolean active = (ap.getActiveSince() > trueAP.getActiveSince());
            boolean last = ap.getLastSeen() > timeThreshold;
            if (!c && active && last) {
                dupAPs.add(ap);
            }
        }

        return dupAPs;
    }

    /**
     * Update the known attachment points.  This method is called whenever
     * topology changes. The method returns true if there's any change to
     * the list of attachment points -- which indicates a possible device
     * move.
     * @return
     */
    protected boolean updateAttachmentPoint() {
        boolean moved = false;
        Collection<AttachmentPoint> attachmentPoints = getAps(); 
        if (attachmentPoints == null || attachmentPoints.isEmpty())
            return false;

        List<AttachmentPoint> apList = new ArrayList<AttachmentPoint>();
        if (attachmentPoints != null) apList.addAll(attachmentPoints);
        Map<Long, AttachmentPoint> newMap = getAPMap(apList);
        if (newMap == null || newMap.size() != apList.size()) {
            moved = true;
        }

        // Prepare the new attachment point list.
        if (moved) {
            List<AttachmentPoint> newAPList =
                    new ArrayList<AttachmentPoint>();
            if (newMap != null) newAPList.addAll(newMap.values());
            setAttachmentPoints(newAPList);
        }

        // Set the oldAPs to null. Fabio: really ? Not empty, just null? 
        setOldAPs(null); 
        return moved;
    }

    /**
     * Update the list of attachment points given that a new packet-in
     * was seen from (sw, port) at time (lastSeen).  The return value is true
     * if there was any change to the list of attachment points for the device
     * -- which indicates a device move.
     * @param sw
     * @param port
     * @param lastSeen
     * @return
     */
    protected boolean updateAttachmentPoint(long sw, short port, long lastSeen){
        ITopologyService topology = deviceManager.topology;
        List<AttachmentPoint> oldAPList;
        List<AttachmentPoint> apList;
        boolean oldAPFlag = false;
        if (!deviceManager.isValidAttachmentPoint(sw, port)) return false;
        AttachmentPoint newAP = new AttachmentPoint(sw, port, lastSeen);

        Collection<AttachmentPoint> attachmentPoints = getAps(); 
        Collection<AttachmentPoint> oldAPs = getOldAPs();
        
        //Copy the oldAP and ap list.
        apList = new ArrayList<AttachmentPoint>();
        if (attachmentPoints != null) apList.addAll(attachmentPoints);
        oldAPList = new ArrayList<AttachmentPoint>();
        if (oldAPs != null) oldAPList.addAll(oldAPs);

        // if the sw, port is in old AP, remove it from there
        // and update the lastSeen in that object.
        if (oldAPList.contains(newAP)) {
            int index = oldAPList.indexOf(newAP);
            newAP = oldAPList.remove(index);
            newAP.setLastSeen(lastSeen);
            setOldAPs( oldAPList);
            oldAPFlag = true;
        }

        // newAP now contains the new attachment point.

        // Get the APMap is null or empty.
        Map<Long, AttachmentPoint> apMap = getAPMap(apList);
        if (apMap == null || apMap.isEmpty()) {
            apList.add(newAP);
            attachmentPoints = apList;
            return true;
        }

        long id = topology.getL2DomainId(sw);
        AttachmentPoint oldAP = apMap.get(id);

        if (oldAP == null) // No attachment on this L2 domain.
        {
            apList = new ArrayList<AttachmentPoint>();
            apList.addAll(apMap.values());
            apList.add(newAP);
            setAttachmentPoints( apList);
            return true; // new AP found on an L2 island.
        }

        // There is already a known attachment point on the same L2 island.
        // we need to compare oldAP and newAP.
        if (oldAP.equals(newAP)) {
            // nothing to do here. just the last seen has to be changed.
            if (newAP.lastSeen > oldAP.lastSeen) {
                oldAP.setLastSeen(newAP.lastSeen);
            }
            setAttachmentPoints(
                    new ArrayList<AttachmentPoint>(apMap.values()));
            return false; // nothing to do here.
        }
        
        int x = deviceManager.apComparator.compare(oldAP, newAP);
        
        if (x < 0) {
            // newAP replaces oldAP.
            apMap.put(id, newAP);
            setAttachmentPoints(
                    new ArrayList<AttachmentPoint>(apMap.values()));

            oldAPList = new ArrayList<AttachmentPoint>();
            if (oldAPs != null) oldAPList.addAll(oldAPs);
            oldAPList.add(oldAP);
            setOldAPs(oldAPList);
            if (!topology.isInSameBroadcastDomain(oldAP.getSw(), oldAP.getPort(),
                                                  newAP.getSw(), newAP.getPort()))
                return true; // attachment point changed.
        } else  if (oldAPFlag) {
            // retain oldAP  as is.  Put the newAP in oldAPs for flagging
            // possible duplicates.
            oldAPList = new ArrayList<AttachmentPoint>();
            if (oldAPs != null) oldAPList.addAll(oldAPs);
            // Add to oldAPList only if it was picked up from the oldAPList
            oldAPList.add(newAP);
            setOldAPs(oldAPList);
            if (!topology.isInSameBroadcastDomain(oldAP.getSw(), oldAP.getPort(),
                                                  newAP.getSw(), newAP.getPort()))
                return true; // attachment point changed.
        }
        return false;
    }

    /**
     * Delete (sw,port) from the list of list of attachment points
     * and oldAPs.
     * @param sw
     * @param port
     * @return
     */
    public boolean deleteAttachmentPoint(long sw, short port) {
        AttachmentPoint ap = new AttachmentPoint(sw, port, 0);
        Collection<AttachmentPoint> oldAPs = getOldAPs(); 
        if (oldAPs != null) {
            ArrayList<AttachmentPoint> apList = new ArrayList<AttachmentPoint>();
            apList.addAll(oldAPs);
            int index = apList.indexOf(ap);
            if (index > 0) {
                apList.remove(index);
                setOldAPs( apList);
                
            }
        }
        Collection<AttachmentPoint> attachmentPoints = getAps(); 
        if (attachmentPoints != null) {
            ArrayList<AttachmentPoint> apList = new ArrayList<AttachmentPoint>();
            apList.addAll(attachmentPoints);
            int index = apList.indexOf(ap);
            if (index > 0) {
                apList.remove(index);
                attachmentPoints = apList;
                return true;
            }
        }
        return false;
    }

    public boolean deleteAttachmentPoint(long sw) {
        boolean deletedFlag;
        ArrayList<AttachmentPoint> apList;
        ArrayList<AttachmentPoint> modifiedList;

        // Delete the APs on switch sw in oldAPs.
        deletedFlag = false;
        apList = new ArrayList<AttachmentPoint>();
        Collection<AttachmentPoint> oldAPs = getOldAPs(); 
        if (oldAPs != null)
            apList.addAll(oldAPs);
        modifiedList = new ArrayList<AttachmentPoint>();

        for(AttachmentPoint ap: apList) {
            if (ap.getSw() == sw) {
                deletedFlag = true;
            } else {
                modifiedList.add(ap);
            }
        }

        if (deletedFlag) {
        	setOldAPs(modifiedList); 
            
        }

        // Delete the APs on switch sw in attachmentPoints.
        deletedFlag = false;
        apList = new ArrayList<AttachmentPoint>();
        Collection<AttachmentPoint> attachmentPoints = getAps(); 
        if (attachmentPoints != null)
            apList.addAll(attachmentPoints);
        modifiedList = new ArrayList<AttachmentPoint>();

        for(AttachmentPoint ap: apList) {
            if (ap.getSw() == sw) {
                deletedFlag = true;
            } else {
                modifiedList.add(ap);
            }
        }

        if (deletedFlag) {
            setAttachmentPoints( modifiedList);
            return true;
        }

        return false;
    }


    ///XXX getAps setter is setAttachmentPoints
    
    @Column(setter="setAttachmentPoints")
    public List<AttachmentPoint> getAps(){
    	return this.attachmentPoints; 
    }
    
    
    @Override
    public SwitchPort[] getAttachmentPoints() {
        return getAttachmentPoints(false);
    }

    
    @Override
    public SwitchPort[] getAttachmentPoints(boolean includeError) {
        List<SwitchPort> sp = new ArrayList<SwitchPort>();
        SwitchPort [] returnSwitchPorts = new SwitchPort[] {};
        Collection<AttachmentPoint> attachmentPoints = getAps(); 
        if (attachmentPoints == null || attachmentPoints.isEmpty()) return returnSwitchPorts;
        
        
        // copy ap list.
        List<AttachmentPoint> apList;
        apList = new ArrayList<AttachmentPoint>();
        if (attachmentPoints != null) apList.addAll(attachmentPoints);
        // get AP map.
        Map<Long, AttachmentPoint> apMap = getAPMap(apList);

        if (apMap != null) {
            for(AttachmentPoint ap: apMap.values()) {
                SwitchPort swport = new SwitchPort(ap.getSw(),
                                                   ap.getPort());
                    sp.add(swport);
            }
        }

        if (!includeError)
            return sp.toArray(new SwitchPort[sp.size()]);

        List<AttachmentPoint> oldAPList;
        oldAPList = new ArrayList<AttachmentPoint>();
        Collection<AttachmentPoint> oldAPs = getOldAPs(); 
        if (oldAPs != null) oldAPList.addAll(oldAPs);

        if (removeExpiredAttachmentPoints(oldAPList))
        	setOldAPs(oldAPList);
            

        List<AttachmentPoint> dupList;
        dupList = this.getDuplicateAttachmentPoints(oldAPList, apMap);
        if (dupList != null) {
            for(AttachmentPoint ap: dupList) {
                SwitchPort swport = new SwitchPort(ap.getSw(),
                                                   ap.getPort(),
                                                   ErrorStatus.DUPLICATE_DEVICE);
                    sp.add(swport);
            }
        }
        return sp.toArray(new SwitchPort[sp.size()]);
    }

    // *******
    // IDevice
    // *******

    @Column
    @Override
    public Long getDeviceKey() {
        return deviceKey;
    }

    
    @Override
    public long getMACAddress() {
        // we assume only one MAC per device for now.
        return entities[0].getMacAddress();
    }

    @Column
    @Override
    public String getMACAddressString() {
        return macAddressString;
    }

    @Column(setter="setVlanIds")
    @Override
    public Short[] getVlanId() {
        return Arrays.copyOf(vlanIds, vlanIds.length);
    }
    
    static final EnumSet<DeviceField> ipv4Fields = EnumSet.of(DeviceField.IPV4);

    
    @Override
    public Integer[] getIPv4Addresses() {
        // XXX - TODO we can cache this result.  Let's find out if this
        // is really a performance bottleneck first though.

        TreeSet<Integer> vals = new TreeSet<Integer>();
        
        Entity[] entities = getEntities();
        for (Entity e : entities) {
        	
            if (e.getIpv4Address() == null) continue;

            // We have an IP address only if among the devices within the class
            // we have the most recent entity with that IP.
            boolean validIP = true;
            //FIXME : well this has been cut since : 1) right now it does not matter 2) it triggers invocations to the data store resulting in an infinite loop. Right known a device is sure to have to have that ip. No device mobility. 
            /*Iterator<Device> devices =
                    deviceManager.queryClassByEntity(getEntityClass(), ipv4Fields, e);
            while (devices.hasNext()) {
            	
                Device d = devices.next();
                if (getDeviceKey().equals(d.getDeviceKey())) 
                    continue;
                for (Entity se : d.getEntities()) {
                    if (se.getIpv4Address() != null &&
                            se.getIpv4Address().equals(e.getIpv4Address()) &&
                            se.getLastSeenTimestamp() != null &&
                            0 < se.getLastSeenTimestamp().
                            compareTo(e.getLastSeenTimestamp())) {
                        validIP = false;
                        break;
                    }
                }
                if (!validIP)
                    break;
            }*/

            if (validIP)
                vals.add(e.getIpv4Address());
        }

        return vals.toArray(new Integer[vals.size()]);
    }

    
    @Override
    public Short[] getSwitchPortVlanIds(SwitchPort swp) {
        TreeSet<Short> vals = new TreeSet<Short>();
        for (Entity e : getEntities()) {
            if (e.switchDPID == swp.getSwitchDPID() 
                    && e.switchPort == swp.getPort()) {
                if (e.getVlan() == null)
                    vals.add(Ethernet.VLAN_UNTAGGED);
                else
                    vals.add(e.getVlan());
            }
        }
        return vals.toArray(new Short[vals.size()]);
    }
    
    @Override
    public Date getLastSeen() {
        Date d = null;
        Entity[] entities = getEntities(); 
        for (int i = 0, len = entities.length; i < len; i++) {
            if (d == null ||
                    entities[i].getLastSeenTimestamp().compareTo(d) > 0)
                d = entities[i].getLastSeenTimestamp();
        }
        return d;
    }

    // ***************
    // Getters/Setters
    // ***************

    
    @Column
    @Override
    public IEntityClass getEntityClass() {
        return Device.entitiesClasses.get(entityClass);
    }

    public void setEntityClass(IEntityClass clazz){
    	entityClass = clazz.getName();
    	Device.entitiesClasses.put(entityClass, clazz);
    }
    
    @Column
    public Entity[] getEntities() {
        return entities;
    }
    
    public void setEntities(Entity[] entities){
    	this.entities = entities;  
    }
    
    public Entity getEntity(int index){
    	return entities[index];
    }
    
    public void setEntity(Entity e, int index){
    	this.entities[index] = e;
    }
    

    // ***************
    // Utility Methods
    // ***************

    /**
     * Check whether the device contains the specified entity
     * @param entity the entity to search for
     * @return the index of the entity, or <0 if not found
     */
    protected int entityIndex(Entity entity) {
        return Arrays.binarySearch(entities, entity);
    }

    // ******
    // Object
    // ******

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(entities);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Device other = (Device) obj;
        if (!getDeviceKey().equals(other.getDeviceKey())) return false;
        if (!Arrays.equals(getEntities(), other.getEntities())) return false;
        return true;
    }
    public static int v =0; 

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Device [deviceKey=");
        builder.append(getDeviceKey());
        builder.append(", entityClass=");
        builder.append(getEntityClass());
        builder.append(", MAC=");
        builder.append(getMACAddressString());
        builder.append(", IPs=[");
        boolean isFirst = true;
        
        
        for (Integer ip: getIPv4Addresses()) {
            if (!isFirst)
                builder.append(", ");
            isFirst = false;
            builder.append(IPv4.fromIPv4Address(ip));
        }
        builder.append("], APs=");
        builder.append(Arrays.toString(getAttachmentPoints(true)));
        builder.append("]");
        
        
        builder.append(", Entities = [");
        Iterator<Entity> it = Lists.newArrayList(this.getEntities()).iterator(); 
        while(it.hasNext()){
        	builder.append(it.next());
        	if (it.hasNext()){
        		builder.append(", ");
        	}
        }
        builder.append("]"); 
        //return builder.toString();
        return deviceKey + ""; 
    }
    
    public Device clone(){
    	return new Device(this); 
    }
    
}
