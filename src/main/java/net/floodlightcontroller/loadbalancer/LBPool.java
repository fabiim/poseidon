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

package net.floodlightcontroller.loadbalancer;

import java.io.Serializable;
import java.util.ArrayList;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.collect.Lists;

import net.floodlightcontroller.loadbalancer.IPClient;

/**
 * Data structure for Load Balancer based on
 * Quantum proposal http://wiki.openstack.org/LBaaS/CoreResourceModel/proposal 
 * 
 * @author KC Wang
 */


@JsonSerialize(using=LBPoolSerializer.class)
public class LBPool implements Serializable{
    protected String id;
    protected String name;
    protected String tenantId;
    protected String netId;
    protected short lbMethod;
    protected byte protocol;
    protected ArrayList<String> members;
    protected ArrayList<String> monitors;
    protected short adminState;
    protected short status;    
    
    protected String vipId;
    
    protected int previousMemberIndex;
    
    
    public LBPool() {
        id = String.valueOf((int) (Math.random()*10000));
        name = null;
        tenantId = null;
        netId = null;
        lbMethod = 0;
        protocol = 0;
        members = new ArrayList<String>();
        monitors = new ArrayList<String>();
        adminState = 0;
        status = 0;
        previousMemberIndex = -1;
    }
    
    public LBPool(LBPool lbPool) {
    	id = lbPool.id; 
    	name = lbPool.id; 
    	tenantId = lbPool.id; 
    	netId = lbPool.id; 
    	lbMethod = lbPool.lbMethod; 
    	protocol = lbPool.protocol;
    	members = Lists.newArrayList(lbPool.members);
    	monitors = Lists.newArrayList(lbPool.monitors);
    	adminState = lbPool.adminState; 
    	status = lbPool.status;
    	vipId = lbPool.vipId; 
    	previousMemberIndex = lbPool.previousMemberIndex; 
	}
    
	public String pickMember(IPClient client) {
        // simple round robin for now; add different lbmethod later
        if (members.size() > 0) {
            previousMemberIndex = (previousMemberIndex + 1) % members.size();
            return members.get(previousMemberIndex);
        } else {
            return null;
        }
    }

    @Override
    public LBPool clone(){
    	return new LBPool(this);
    }
}
