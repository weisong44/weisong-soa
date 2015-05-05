package com.weisong.soa.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostUtil {

	final static private String ipAddr;
	final static private String hostname;
	
	final static public Logger logger = LoggerFactory.getLogger(HostUtil.class);
	
	static {
		hostname = getHostname();
		ipAddr = getHostIpAddress();
	}
	
	static public String getHostname() {
		if(hostname != null) {
			return hostname;
		}
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "unknown";
		}
	}

	static public String getHostIpAddress() {
		if(ipAddr != null) {
			return ipAddr;
		}
		try {
			logger.debug("Trying to find the host IP address:");
			InetAddress local = null, global = null;
			Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
			for (; n.hasMoreElements();) {
				NetworkInterface e = n.nextElement();
				Enumeration<InetAddress> a = e.getInetAddresses();
				for (; a.hasMoreElements();) {
					InetAddress addr = a.nextElement();
					if(addr instanceof Inet6Address) {
						logger.debug(String.format("  %s/%s: IPv6 address, skip.", 
								e.getName(), addr.getHostAddress()));
						continue;
					}
					if(addr.isReachable(100) == false) {
						logger.debug(String.format("  %s/%s: unreachable, skip.", 
								e.getName(), addr.getHostAddress()));
						continue;
					}
					if(addr.isLoopbackAddress()) {
						logger.debug(String.format("  %s/%s: loopback address, skip.", 
								e.getName(), addr.getHostAddress()));
						continue;
					}
					if(addr.isSiteLocalAddress()) {
						logger.debug(String.format("  %s/%s: site local address, use as candidate.", 
								e.getName(), addr.getHostAddress()));
						local = addr;
					}
					else {
						logger.debug(String.format("  %s/%s: global address, use as candidate.", 
								e.getName(), addr.getHostAddress()));
						global = addr;
					}
				}
			}

			if(global != null) {
				logger.debug(String.format("  Using global address %s", global.getHostAddress()));
				return global.getHostAddress();
			} else if(local != null) {
				logger.debug(String.format("  Using site local address %s", local.getHostAddress()));
				return local.getHostAddress();
			}
			else {
				logger.warn("  Can't find an usable address, using 'localhost'");
				return "localhost";
			}
					
		} catch (Exception e) {
			logger.warn("  Failed to find an usable address, using 'localhost'");
			return "localhost";
		}
	}
}