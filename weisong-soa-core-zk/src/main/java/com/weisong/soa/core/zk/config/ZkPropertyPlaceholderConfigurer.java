package com.weisong.soa.core.zk.config;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import lombok.Getter;

import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weisong.common.ReadablePropertyPlaceholderConfigurer;
import com.weisong.soa.core.zk.ZkClient;
import com.weisong.soa.core.zk.ZkConst;
import com.weisong.soa.core.zk.util.ZkServiceUitl;
import com.weisong.soa.service.ServiceDescriptor;
import com.weisong.soa.util.GenericUtil;

public class ZkPropertyPlaceholderConfigurer extends ReadablePropertyPlaceholderConfigurer 
		implements ZkPropertyChangeRegistry, Closeable {

	final private Logger logger = LoggerFactory.getLogger(getClass());
	
	private ZkClient zkClient;
	
	@Getter String zkNodePath;
	
	private List<Listener> listeners = new LinkedList<>();
	
	public ZkPropertyPlaceholderConfigurer(ZkClient zkClient) throws Exception {
		this.zkClient = zkClient;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected Properties mergeProperties() throws IOException {
		// Merge all local perperties
		Properties properties = super.mergeProperties();

		String disabled = System.getProperty(ZkConst.ZK_DISABLED);
		if("true".equalsIgnoreCase(disabled)) {
			logger.info("ZK configuration source is disabled");
			return properties;
		}
		
		mergeFromZk(properties);
		
		return properties;
	}
	
	private void mergeFromZk(Properties properties) {
		// Set the ZK node path
		setZkNodePath(properties);
		
		// Merge ZK properties into the set
		try {
			Properties newProps = readFromZk();
			for (@SuppressWarnings("rawtypes")
			Enumeration en = newProps.propertyNames(); en.hasMoreElements();) {
				String key = (String) en.nextElement();
				String value = newProps.getProperty(key);
				properties.setProperty(key, value);
			}
		}
		catch (Exception e) {
			String errMsg = String.format(
					"Failed to load properties: %s", 
					e.getMessage());
				logger.error(errMsg);
				System.err.println(errMsg);
				System.exit(-1);
		}

		// Subscribe to node changes
		watchZkNodePath();
	}
	
	private void setZkNodePath(Properties properties) {
        Object o = System.getProperties();
		zkNodePath = System.getProperty(ZkConst.ZK_NODE_PATH);
		if(zkNodePath == null) {
			String type = properties.getProperty("app.type");
			String domain = properties.getProperty("app.domain");
			String name = properties.getProperty("app.name");
			String version = properties.getProperty("app.version");
			if(GenericUtil.isAnyEmpty(type, domain, name, version)) {
				String errMsg = String.format(
					"Failed to construct ZK path for configuration, "
				  + "system property %s must be set", ZkConst.ZK_NODE_PATH);
				logger.error(errMsg);
				System.err.println(errMsg);
				System.exit(-1);
			}
			
			if("service".equals(type)) {
				ServiceDescriptor desc = new ServiceDescriptor(
						domain, name, version);
				zkNodePath = ZkServiceUitl.getConfigNodePath(desc);
			}
			else {
				zkNodePath = String.format("%s/%s/%s/%s/%s", ZkConst.ZK_CONFIG_ROOT, 
						type, domain, name, version);
			}
		}
	}
	
	private void watchZkNodePath() {
		try {
			zkClient.watch(zkNodePath, new NodeCacheListener() {
				@Override
				public void nodeChanged() throws Exception {
					Properties props = readFromZk();
					for(Listener listener : listeners) {
						listener.propertyChanged(props);
					}
				}
			});
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Properties readFromZk() throws Exception {

		String connStr = System.getProperty(ZkConst.ZK_CONN_STR);		
		logger.info(String.format("Loading properties from URL [zk://%s%s]", 
				connStr, zkNodePath));
		
		String data = zkClient.get(zkNodePath);
		if(data == null) {
			throw new Exception(String.format(
				"ZK node %s doesn't exist", zkNodePath));
		}
		
		Properties props = new Properties();
		props.load(new ByteArrayInputStream(data.getBytes()));
		logger.info(String.format("Loaded %d properties from ZK", props.size()));
		return props;
	}

	@Override
	public void addPropertyChangeListener(Listener listener) {
		if(listeners.contains(listener)) {
			return;
		}
		listeners.add(listener);
	}
	
	@Override
	public void close() throws IOException {
		if(zkClient != null) {
			zkClient.close();
		}
	}
}
