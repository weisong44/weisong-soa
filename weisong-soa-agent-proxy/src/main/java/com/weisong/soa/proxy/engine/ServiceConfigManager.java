package com.weisong.soa.proxy.engine;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.weisong.soa.core.zk.service.ZkServiceHandlerReadOnly;
import com.weisong.soa.core.zk.util.ZkServiceUitl;
import com.weisong.soa.service.ServiceDescriptor;

public class ServiceConfigManager {
	
	static public interface Listener {
		void serviceConfigChanged(ServiceDescriptor desc, Properties props);
	};
	
	static private class ServiceData {
		private Properties props;
		private List<Listener> listeners = new LinkedList<>();
		private ServiceData(Properties props) {
			this.props = props;
		}
	}
	
	final private Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	@Autowired 
	private ZkServiceHandlerReadOnly zkHandler;
	
	private Map<ServiceDescriptor, ServiceData> serviceToConfigMap =
			new ConcurrentHashMap<>();
	
	public Properties getServiceConfig(ServiceDescriptor desc) 
			throws Exception {
		ServiceData service = serviceToConfigMap.get(desc);
		if(service == null) {
			synchronized (serviceToConfigMap) {
				if(service == null) {
					Properties props = zkHandler.readConfig(desc);
					service = new ServiceData(props);
					serviceToConfigMap.put(desc, service);
				}
			}
		}
		return service.props;
	}
	
	public Properties getServiceConfig(ServiceDescriptor desc, Listener listener) 
			throws Exception {
		Properties props = getServiceConfig(desc);
		if(listener != null) {
			watchServiceConfig(desc, listener);
		}
		return props;
	}
	
	private void watchServiceConfig(final ServiceDescriptor desc, Listener listener) 
			throws Exception {
		// Read the routing configuration
		String path = ZkServiceUitl.getConfigNodePath(desc);
		final ServiceData service = serviceToConfigMap.get(desc);
		if(service == null) {
			throw new RuntimeException("Service not found in cache!");
		}
		
		service.listeners.add(listener);
		
		zkHandler.getZkClient().watch(path, new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				logger.info(String.format(
					"Configuration for %s changed on ZK, read again", desc));
				Properties props = zkHandler.readConfig(desc);
				ServiceData service = serviceToConfigMap.get(desc);
				service.props = props;
				for(Listener l : service.listeners) {
					l.serviceConfigChanged(desc, props);
				}
			}
		});
		
	}
}

