package com.weisong.soa.core.zk.util;

import com.weisong.soa.core.zk.ZkConst;
import com.weisong.soa.service.ServiceDescriptor;
import com.weisong.soa.util.HostUtil;

public class ZkServiceUitl {
	static public String getConfigNodePath(ServiceDescriptor desc) {
		return String.format("%s/service/%s/%s/%s", 
				ZkConst.ZK_CONFIG_ROOT, desc.getDomain(), desc.getService(), desc.getVersion());
	}
	
	static public String getRegistryParentPath(String domain, String serviceName) {
		return String.format("%s/service/%s/%s", 
				ZkConst.ZK_REGISTRY_ROOT, domain, serviceName);
	}
	
	static public String getRegistryNodePath(ServiceDescriptor desc, int port) {
		String node = String.format("%s:%d:%s", 
				HostUtil.getHostIpAddress(), port, desc.getVersion());
		return String.format("%s/%s", 
				getRegistryParentPath(desc.getDomain(), desc.getService()), node);
	}
}
