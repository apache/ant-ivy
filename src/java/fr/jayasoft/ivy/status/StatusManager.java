/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.util.Message;

/**
 * Note: update methods (such as addStatus) should only be called BEFORE any call to accessor methods
 * @author x.hanin
 *
 */
public class StatusManager {
    public static StatusManager newDefaultInstance() {
        return new StatusManager(new Status[] {
                new Status("release", false), 
                new Status("milestone", false), 
                new Status("integration", true)}, "integration");
    }
    
    public static StatusManager getCurrent() {
        return Ivy.getCurrent().getStatusManager();
    }
    
    private List _status = new ArrayList();
    private String _defaultStatus;
    
    // for easier querying only
    private Map _statusPriorityMap;
    private Map _statusIntegrationMap;
    private String _deliveryStatusListString;

    public StatusManager(Status[] status, String defaultStatus) {
        _status.addAll(Arrays.asList(status));
        _defaultStatus = defaultStatus;

        computeMaps();
    }
    
    public StatusManager() {
    }
    
    public void addStatus(Status status) {
        _status.add(status);
    }

    public void setDefaultStatus(String defaultStatus) {
        _defaultStatus = defaultStatus;
    }
    
    public List getStatuses() {
        return _status;
    }
    
    private void computeMaps() {
        if (_status.isEmpty()) {
            throw new IllegalStateException("badly configured statuses: no status found");
        }
        _statusPriorityMap = new HashMap();
        for (ListIterator iter = _status.listIterator(); iter.hasNext();) {
            Status status = (Status)iter.next();
            _statusPriorityMap.put(status.getName(), new Integer(iter.previousIndex()));
        }
        _statusIntegrationMap = new HashMap();
        for (Iterator iter = _status.iterator(); iter.hasNext();) {
            Status status = (Status)iter.next();
            _statusIntegrationMap.put(status.getName(), Boolean.valueOf(status.isIntegration()));
        }
    }
    
    public boolean isStatus(String status) {
        if (_statusPriorityMap == null) {
            computeMaps();
        }
        return _statusPriorityMap.containsKey(status);
    }

    public int getPriority(String status) {
        if (_statusPriorityMap == null) {
            computeMaps();
        }
        Integer priority = (Integer)_statusPriorityMap.get(status);
        if (priority == null) {
            Message.debug("unknown status "+status+": assuming lowest priority");
            return Integer.MAX_VALUE;
        }
        return priority.intValue();
    }
    
    public boolean isIntegration(String status) {
        if (_statusIntegrationMap == null) {
            computeMaps();
        }
        Boolean isIntegration = (Boolean)_statusIntegrationMap.get(status);
        if (isIntegration == null) {
            Message.debug("unknown status "+status+": assuming integration");
            return true;
        }
        return isIntegration.booleanValue();
    }

    public String getDeliveryStatusListString() {
        if (_deliveryStatusListString == null) {
            StringBuffer ret = new StringBuffer();
            for (Iterator iter = _status.iterator(); iter.hasNext();) {
                Status status = (Status)iter.next();
                if (!status.isIntegration()) {
                    ret.append(status.getName()).append(","); 
                }
            }
            if (ret.length() > 0) {
                ret.deleteCharAt(ret.length()-1);
            }
            _deliveryStatusListString = ret.toString();
        }
        return _deliveryStatusListString;
    }

    public String getDefaultStatus() {
        if (_defaultStatus == null) {
            if (_status.isEmpty()) {
                throw new IllegalStateException("badly configured statuses: no status found");
            }
            _defaultStatus = ((Status)_status.get(_status.size() - 1)).getName();
        }
        return _defaultStatus;
    }
}
