package com.weisong.soa.core.zk.service;

import java.util.List;
import java.util.Properties;

import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import com.weisong.soa.core.zk.ZkClient;
import com.weisong.soa.service.ServiceDescriptor;

public interface ZkServiceHandlerReadOnly {
	
	ZkClient getZkClient();
	
	Properties readConfig(ServiceDescriptor desc)
		throws Exception;
	
	Properties readConfig(ServiceDescriptor desc,
			NodeCacheListener listener) throws Exception;
		
	List<String> getRegisteredEndpoints(String domain, String serviceName) 
			throws Exception;
	
	List<String> getRegisteredEndpoints(String domain, String serviceName,
			PathChildrenCacheListener listener) throws Exception;
	
}
