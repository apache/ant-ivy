/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core.module.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.util.Message;

/**
 * Note: update methods (such as addStatus) should only be called BEFORE any call to accessor
 * methods
 */
public class StatusManager {
    public static StatusManager newDefaultInstance() {
        return new StatusManager(new Status[] {new Status("release", false),
                new Status("milestone", false), new Status("integration", true)}, "integration");
    }

    public static StatusManager getCurrent() {
        return IvyContext.getContext().getSettings().getStatusManager();
    }

    private List<Status> status = new ArrayList<Status>();

    private String defaultStatus;

    // for easier querying only
    private Map<String, Integer> statusPriorityMap;

    private Map<String, Boolean> statusIntegrationMap;

    private String deliveryStatusListString;

    public StatusManager(Status[] status, String defaultStatus) {
        this.status.addAll(Arrays.asList(status));
        this.defaultStatus = defaultStatus;

        computeMaps();
    }

    public StatusManager() {
    }

    public void addStatus(Status status) {
        this.status.add(status);
    }

    public void setDefaultStatus(String defaultStatus) {
        this.defaultStatus = defaultStatus;
    }

    public List<Status> getStatuses() {
        return status;
    }

    private void computeMaps() {
        if (status.isEmpty()) {
            throw new IllegalStateException("badly configured statuses: no status found");
        }
        statusPriorityMap = new HashMap<String, Integer>();
        for (ListIterator<Status> iter = status.listIterator(); iter.hasNext();) {
            Status status = iter.next();
            statusPriorityMap.put(status.getName(), new Integer(iter.previousIndex()));
        }
        statusIntegrationMap = new HashMap<String, Boolean>();
        for (Iterator<Status> iter = status.iterator(); iter.hasNext();) {
            Status status = iter.next();
            statusIntegrationMap.put(status.getName(), Boolean.valueOf(status.isIntegration()));
        }
    }

    public boolean isStatus(String status) {
        if (statusPriorityMap == null) {
            computeMaps();
        }
        return statusPriorityMap.containsKey(status);
    }

    public int getPriority(String status) {
        if (statusPriorityMap == null) {
            computeMaps();
        }
        Integer priority = (Integer) statusPriorityMap.get(status);
        if (priority == null) {
            Message.debug("unknown status " + status + ": assuming lowest priority");
            return Integer.MAX_VALUE;
        }
        return priority.intValue();
    }

    public boolean isIntegration(String status) {
        if (statusIntegrationMap == null) {
            computeMaps();
        }
        Boolean isIntegration = (Boolean) statusIntegrationMap.get(status);
        if (isIntegration == null) {
            Message.debug("unknown status " + status + ": assuming integration");
            return true;
        }
        return isIntegration.booleanValue();
    }

    public String getDeliveryStatusListString() {
        if (deliveryStatusListString == null) {
            StringBuffer ret = new StringBuffer();
            for (Status status : this.status) {
                if (!status.isIntegration()) {
                    ret.append(status.getName()).append(",");
                }
            }
            if (ret.length() > 0) {
                ret.deleteCharAt(ret.length() - 1);
            }
            deliveryStatusListString = ret.toString();
        }
        return deliveryStatusListString;
    }

    public String getDefaultStatus() {
        if (defaultStatus == null) {
            if (status.isEmpty()) {
                throw new IllegalStateException("badly configured statuses: no status found");
            }
            defaultStatus = ((Status) status.get(status.size() - 1)).getName();
        }
        return defaultStatus;
    }
}
