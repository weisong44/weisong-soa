package com.weisong.soa.core.zk.service;

import java.util.Properties;

import com.weisong.soa.core.zk.ZkClient;
import com.weisong.soa.service.ServiceDescriptor;

public interface ZkServiceHandlerWriteOnly {
	
	ZkClient getZkClient();
	
	void writeConfig(ServiceDescriptor desc, 
		Properties properties) throws Exception;
	
	void register(ServiceDescriptor desc, int port) throws Exception;
	
}
