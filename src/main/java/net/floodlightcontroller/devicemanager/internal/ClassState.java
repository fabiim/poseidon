package net.floodlightcontroller.devicemanager.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

import net.floodlightcontroller.devicemanager.IDeviceService.DeviceField;
import net.floodlightcontroller.devicemanager.IEntityClass;
import net.floodlightcontroller.devicemanager.IEntityClassifierService;
import net.floodlightcontroller.devicemanager.internal.original.DeviceUniqueIndex;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import datastore.Datastore;

/**
 * Used to cache state about specific entity classes
 */
public class ClassState implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	/**
     * The class index
     */
    protected net.floodlightcontroller.devicemanager.internal.original.DeviceUniqueIndex classIndex;
    

    
	@Override
	public String toString() {
		return "ClassState [clName=" + clName + ", classIndex=" + printClassIndex(classIndex)  
				+ ", secondaryIndexMap=" + printSecondaryIndex(secondaryIndexMap) + "]";
	}

	private String printSecondaryIndex(
			Map<EnumSet<DeviceField>, DeviceIndex> secondaryIndexMap2) {
		if (secondaryIndexMap2 == null) return "null"; 
		StringBuilder s = new StringBuilder();
		for (Map.Entry<EnumSet<DeviceField>,DeviceIndex> en : secondaryIndexMap2.entrySet()){
			s.append(print(en.getKey()) + "-> ");
			s.append(print(en.getValue().getAll())); 
		}
		return s.toString(); 
	}

	private String print(Iterator<Long> all) {
			ArrayList<Long> l = Lists.newArrayList(all);
			if (l.size() == 0 ) return "empty"; 
			Collections.sort(l); 
			StringBuilder s = new StringBuilder(); 
			Iterator<Long> it = l.iterator();
			Long last = it.next();
			s.append(last);
			boolean inInterval = false; 
			while (it.hasNext()){
				Long current = it.next();
				if (current != (last +1)){
					s.append(inInterval? ("-" + last) :  "");
					s.append("," + current );
					inInterval = false; 
				}
				else {
					inInterval = true; 
				}
				if (!it.hasNext() ){
					
					s.append(inInterval? ("-" + current) : "");
				}
				last = current; 
			}
			return s.toString(); 
		}


	private String print(EnumSet<DeviceField> key) {
		StringBuilder s = new StringBuilder(); 
		Iterator<DeviceField> it = key.iterator();
		while(it.hasNext()){
			s.append(it.next().name());
			if (it.hasNext()){
				s.append((",")); 
			}
		}
		s.append(" "); 
		return s.toString(); 
	}

	private String printClassIndex(DeviceUniqueIndex classIndex2) {
		if (classIndex2 == null) return "null"; 
		StringBuilder s = new StringBuilder();
		s.append(classIndex2.keyFields); 
		s.append(" -> ");
		print( classIndex2.getAll()); 
		return s.toString(); 
	}


	//Needed to know the key from the ClassState object in classStateMap. 
    protected String clName; 
    
    /**
     * This stores secondary indices over the fields in the device for the
     * class
     */
    protected Map<EnumSet<DeviceField>, DeviceIndex> secondaryIndexMap;
    
    /**
     * Allocate a new {@link ClassState} object for the class
     * @param clazz the class to use for the state
     */
    public ClassState(IEntityClass clazz, IEntityClassifierService entityClassifier, Collection<EnumSet<DeviceField>> sets , Datastore ds) {
    	clName = clazz.getName(); 
        EnumSet<DeviceField> keyFields = clazz.getKeyFields();
        EnumSet<DeviceField> primaryKeyFields =
                entityClassifier.getKeyFields();
        boolean keyFieldsMatchPrimary =
                primaryKeyFields.equals(keyFields);

        if (!keyFieldsMatchPrimary)
            classIndex = new net.floodlightcontroller.devicemanager.internal.original.DeviceUniqueIndex(keyFields);
        
        secondaryIndexMap = Maps.newHashMap(); 

        for (EnumSet<DeviceField> fields : sets) {
            secondaryIndexMap.put(fields,
                                  new DeviceMultiIndex(fields, ds));
        }
    }
}