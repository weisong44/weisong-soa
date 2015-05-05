package com.weisong.soa.core.zk.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.weisong.soa.core.zk.ZkClient;
import com.weisong.soa.core.zk.util.ZkServiceUitl;
import com.weisong.soa.service.ServiceDescriptor;
import com.weisong.soa.util.HostUtil;

public class ZkServiceHandlerImpl implements ZkServiceHandlerReadOnly, ZkServiceHandlerWriteOnly, Closeable {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private ZkClient zkClient;
	
	public ZkServiceHandlerImpl() throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				ZkServiceHandlerImpl.this.zkClient.close();
			}
		});
	}
	
	@Override
	public void writeConfig(ServiceDescriptor desc, 
			Properties properties) throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		properties.store(os, String.format("%s.%s", desc.getDomain(), desc.getService()));
		String config = new String(os.toByteArray());
		String path = ZkServiceUitl.getConfigNodePath(desc);
		if(zkClient.get(path) == null) {
			zkClient.create(path, config, CreateMode.PERSISTENT);
			logger.info(String.format("Created %s with %d properties", 
					path, properties.size()));
		}
		else {
			zkClient.update(path, config);
			logger.info(String.format("Updated %s with %d properties", 
				path, properties.size()));
		}
	}

	@Override
	public void register(ServiceDescriptor desc, int port) throws Exception {
		String node = String.format("%s:%d:%s", 
				HostUtil.getHostIpAddress(), port, desc.getVersion());
		String path = ZkServiceUitl.getRegistryNodePath(desc, port);
		if(zkClient.get(path) != null) {
			throw new Exception(String.format(
					"Weird, endpoint %s alredy registered under %s, "
					+ "something is wrong.", 
					node, path));
		}
		else {
			zkClient.create(path, "", CreateMode.EPHEMERAL);
			logger.info(String.format("Created ephemeral node %s", path));
		}
	}

	@Override
	public Properties readConfig(ServiceDescriptor desc) 
			throws Exception {
		String path = ZkServiceUitl.getConfigNodePath(desc);
		String data = zkClient.get(path);
		if(data == null) {
			throw new Exception(String.format(
				"ZK node %s doesn't exist", path));
		}
		Properties props = new Properties();
		props.load(new ByteArrayInputStream(data.getBytes()));
		logger.info(String.format("Loaded %d properties from %s", 
			props.size(), path));
		return props;
	}

	@Override
	public Properties readConfig(ServiceDescriptor desc, NodeCacheListener listener) 
			throws Exception {
		final String path = ZkServiceUitl.getConfigNodePath(desc);
		zkClient.watch(path, listener);
		logger.info(String.format("Registered node watch for %s", path)); 
		return readConfig(desc);
	}

	@Override
	public List<String> getRegisteredEndpoints(String domain, String serviceName) 
			throws Exception {
		String path = ZkServiceUitl.getRegistryParentPath(domain, serviceName);
		List<String> children = zkClient.getChildren(path);
		logger.info(String.format("Loaded %d children from %s", children.size(), path));
		return children;
	}
	
	@Override
	public List<String> getRegisteredEndpoints(String domain,
			String serviceName, PathChildrenCacheListener listener) throws Exception {
		final String path = ZkServiceUitl.getRegistryParentPath(domain, serviceName);
		zkClient.watchChildren(path, listener);
		logger.info(String.format("Registered children watch for %s", path)); 
		return getRegisteredEndpoints(domain, serviceName);
	}
	
	@Override
	public void close() throws IOException {
		if(zkClient != null) {
			zkClient.close();
		}
	}

	static public class JavaConfig {
		@Bean ZkServiceHandlerImpl serviceHandler() throws Exception {
			return new ZkServiceHandlerImpl();
		}
	}


	@Override
	public ZkClient getZkClient() {
		return zkClient;
	}
}
