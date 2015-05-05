package com.weisong.soa.core.zk.bootstrap;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weisong.soa.core.zk.DefaultZkClient;
import com.weisong.soa.core.zk.ZkClient;
import com.weisong.soa.core.zk.ZkConst;
import com.weisong.soa.util.GenericUtil;

public class BootstrapZkClient implements ZkClient {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private String bootstrapNodePath;
	
	private ZkClient bootstrapZkClient;
	private Map<String, String> pathToConnStrMap = new HashMap<>();
	private Map<String, ZkClient> connStrToZkClientMap = new HashMap<>();
	
	public BootstrapZkClient() throws Exception {
		// Internal ZK client
		String connStr = overrideBootstrapConnStr;
		if(connStr == null) {
			connStr = System.getProperty(ZkConst.ZK_CONN_STR);
			if(GenericUtil.isEmpty(connStr)) {
				String errMsg = "System property not set: " + ZkConst.ZK_CONN_STR;
				logger.error(errMsg);
				System.err.println(errMsg);
				System.exit(-1);
			}
		}
		bootstrapZkClient = DefaultZkClient.create(connStr);

		// Bootstrap path
		bootstrapNodePath = System.getProperty(ZkConst.ZK_BOOTSTRAP_NODE_PATH);
		if(bootstrapNodePath == null) {
			bootstrapNodePath = "/bootstrap/data";
		}
		
		init();
	}

	protected void init() throws Exception {
		readZkMappingConfig();
		bootstrapZkClient.watch(bootstrapNodePath, new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				readZkMappingConfig();
			}
		});
	}

	synchronized private void readZkMappingConfig() throws Exception {
		// Clear path map
		pathToConnStrMap.clear();
		// Setup new mapping
		String data = bootstrapZkClient.get(bootstrapNodePath);
		if(data == null) {
			return;
		}
		BufferedReader reader = new BufferedReader(new StringReader(data));
		String line = null;
		while((line = reader.readLine()) != null) {
			if(line.trim().length() <= 0) {
				continue;
			}
			String[] tokens = line.split("=");
			String path = tokens[0];
			String connStr = tokens[1];
			pathToConnStrMap.put(path, connStr);
		}
		logger.info("Read path to ZkCluster mapping");
	}
	
	synchronized private ZkClient getZkClient(String path) {
		for(String zkPath : pathToConnStrMap.keySet()) {
			if(zkPath != null && path.startsWith(zkPath)) {
				String connStr = pathToConnStrMap.get(zkPath);
				ZkClient zkClient = connStrToZkClientMap.get(connStr);
				if(zkClient == null) {
					zkClient = DefaultZkClient.create(connStr);
					connStrToZkClientMap.put(connStr, zkClient);
				}
				return zkClient;
			}
		}
		return bootstrapZkClient;
	}
	
	@Override
	public void close() {
		for(ZkClient c : connStrToZkClientMap.values()) {
			c.close();
		}
		pathToConnStrMap.clear();
		connStrToZkClientMap.clear();
		bootstrapZkClient.close();
	}

	@Override
	public boolean create(String path, String data, CreateMode mode) throws Exception {
		ZkClient zkClient = getZkClient(path);
		return zkClient.create(path, data, mode);
		
	}
	
	@Override
	public String get(String path) throws Exception {
		ZkClient zkClient = getZkClient(path);
		return zkClient.get(path);
	}

	@Override
	public List<String> getChildren(String path) throws Exception {
		ZkClient zkClient = getZkClient(path);
		return zkClient.getChildren(path);
	}

	@Override
	public void update(String path, String data) throws Exception {
		ZkClient zkClient = getZkClient(path);
		zkClient.update(path, data);
	}

	@Override
	public void delete(String path) throws Exception {
		ZkClient zkClient = getZkClient(path);
		zkClient.delete(path);
	}

	@Override
	public void watch(String path, NodeCacheListener listener) throws Exception {
		ZkClient zkClient = getZkClient(path);
		zkClient.watch(path, listener);
	}

	@Override
	public void createAndWatch(String path,  
			CreateMode mode, NodeCacheListener listener) 
			throws Exception {
		createAndWatch(path, "", mode, listener);
	}
	
	@Override
	public void createAndWatch(String path, String data, 
			CreateMode mode, NodeCacheListener listener) 
			throws Exception {
		ZkClient zkClient = getZkClient(path);
		zkClient.createAndWatch(path, data, mode, listener);
	}

	@Override
	public void watchChildren(String path, PathChildrenCacheListener listener)
			throws Exception {
		ZkClient zkClient = getZkClient(path);
		zkClient.watchChildren(path, listener);
	}

	@Override
	public void createAndWatchChildren(String path,  
			CreateMode mode, PathChildrenCacheListener listener) 
			throws Exception {
		createAndWatchChildren(path, "", mode, listener);
	}
	
	@Override
	public void createAndWatchChildren(String path, String data, 
			CreateMode mode, PathChildrenCacheListener listener) 
			throws Exception {
		ZkClient zkClient = getZkClient(path);
		zkClient.createAndWatchChildren(path, data, mode, listener);
	}

	static public String overrideBootstrapConnStr;
	
}
