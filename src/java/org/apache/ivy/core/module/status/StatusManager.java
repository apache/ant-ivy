/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;
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

    private final List<Status> statuses = new ArrayList<>();

    private String defaultStatus;

    // for easier querying only
    private Map<String, Integer> statusPriorityMap;

    private Map<String, Boolean> statusIntegrationMap;

    private String deliveryStatusListString;

    public StatusManager(Status[] status, String defaultStatus) {
        this.statuses.addAll(Arrays.asList(status));
        this.defaultStatus = defaultStatus;

        computeMaps();
    }

    public StatusManager() {
    }

    public void addStatus(Status status) {
        this.statuses.add(status);
    }

    public void setDefaultStatus(String defaultStatus) {
        this.defaultStatus = defaultStatus;
    }

    public List<Status> getStatuses() {
        return statuses;
    }

    private void computeMaps() {
        if (statuses.isEmpty()) {
            throw new IllegalStateException("badly configured statuses: none found");
        }
        statusPriorityMap = new HashMap<>();
        for (Status status : statuses) {
            statusPriorityMap.put(status.getName(), statuses.indexOf(status));
        }
        statusIntegrationMap = new HashMap<>();
        for (Status status : statuses) {
            statusIntegrationMap.put(status.getName(), status.isIntegration());
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
        Integer priority = statusPriorityMap.get(status);
        if (priority == null) {
            Message.debug("unknown status " + status + ": assuming lowest priority");
            return Integer.MAX_VALUE;
        }
        return priority;
    }

    public boolean isIntegration(String status) {
        if (statusIntegrationMap == null) {
            computeMaps();
        }
        Boolean isIntegration = statusIntegrationMap.get(status);
        if (isIntegration == null) {
            Message.debug("unknown status " + status + ": assuming integration");
            return true;
        }
        return isIntegration;
    }

    public String getDeliveryStatusListString() {
        if (deliveryStatusListString == null) {
            StringBuilder ret = new StringBuilder();
            for (Status status : statuses) {
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
            if (statuses.isEmpty()) {
                throw new IllegalStateException("badly configured statuses: none found");
            }
            defaultStatus = statuses.get(statuses.size() - 1).getName();
        }
        return defaultStatus;
    }
}
