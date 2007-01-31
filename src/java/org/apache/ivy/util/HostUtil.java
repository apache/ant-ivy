package org.apache.ivy.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostUtil {
    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
}
