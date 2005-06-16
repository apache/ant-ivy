/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author x.hanin
 *
 */
public class Status {
    public static final String DEFAULT_STATUS = "integration";
    private static final String[] STATUSES = new String[] {"release", "milestone", "integration"};
    private static final boolean[] IS_INTEGRATION = new boolean[] {false, false, true};
    private static Map _statusPriorityMap;
    private static Map _statusIntegrationMap;
    static {
        _statusPriorityMap = new HashMap();
        for (int i = 0; i < STATUSES.length; i++) {
            _statusPriorityMap.put(STATUSES[i], new Integer(i));
        }
        _statusIntegrationMap = new HashMap();
        for (int i = 0; i < IS_INTEGRATION.length; i++) {
            _statusIntegrationMap.put(STATUSES[i], new Boolean(IS_INTEGRATION[i]));
        }
    }
    
    public static int getPriority(String status) {
        Integer priority = (Integer)_statusPriorityMap.get(status);
        if (priority == null) {
            throw new IllegalArgumentException("unknown status "+status);
        }
        return priority.intValue();
    }
    
    public static boolean isIntegration(String status) {
        Boolean isIntegration = (Boolean)_statusIntegrationMap.get(status);
        if (isIntegration == null) {
            throw new IllegalArgumentException("unknown status "+status);
        }
        return isIntegration.booleanValue();
    }

    public static String getDeliveryStatusListString() {
        // TODO : use constants
        return "milestone,release";
    }
}
