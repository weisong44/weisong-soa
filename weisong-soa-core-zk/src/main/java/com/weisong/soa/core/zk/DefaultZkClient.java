package com.weisong.soa.core.zk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultZkClient implements ZkClient {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private CuratorFramework curator;

	private Map<String, NodeCache> watchMap = new HashMap<>();
	private Map<String, PathChildrenCache> childrenWatchMap = new HashMap<>();
	
	static public DefaultZkClient create(String zkConnStr) {
		return create(zkConnStr, 1000, 3);
	}
	
	static public DefaultZkClient create(String zkConnStr, Integer baseSleepTimeMs, Integer maxRetries) {
		RetryPolicy retry = new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries);
		CuratorFramework curator = CuratorFrameworkFactory.newClient(zkConnStr, retry);
		curator.start();
		DefaultZkClient zkClient = new DefaultZkClient(curator);
		return zkClient;
	}
	
	public DefaultZkClient(CuratorFramework curator) {
		this.curator = curator;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				close();
			}
		});
	}
	
	@Override
	public void close() {
		for(NodeCache node : watchMap.values()) {
			try {
				node.close();
			} catch (Throwable t) {
				t.printStackTrace(System.err);
			}
		}
		for(PathChildrenCache node : childrenWatchMap.values()) {
			try {
				node.close();
			} catch (Throwable t) {
				t.printStackTrace(System.err);
			}
		}
		if(curator != null) {
			curator.close();
		}
	}
	
	@Override
	public boolean create(String path, String data, CreateMode mode) throws Exception {
        try {
			curator.create()
				.creatingParentsIfNeeded()
				.withMode(mode)
				.forPath(path, data.getBytes());
			return true;
		} catch (NodeExistsException ex) {
			// Ignore
			logger.info(String.format("Create: node %s already exists, update.", path));
			update(path, data);
			return false;
		}
	}
	
	@Override
	public String get(String path) throws Exception {
		try {
			byte[] bytes = curator.getData().forPath(path);
			if(bytes != null) {
				return new String(bytes);
			}
			return null;
		} catch (NoNodeException e) {
			return null;
		}
	}

	public List<String> getChildren(String path) throws Exception {
		return curator.getChildren().forPath(path);
	}
	
	@Override
	public void update(String path, String data) throws Exception {
		curator.setData()
			.forPath(path, data.getBytes());
	}

	@Override
	public void delete(String path) throws Exception {
		try {
			curator.delete()
				.deletingChildrenIfNeeded()
				.forPath(path);
		} catch (NoNodeException ex) {
			// Ignore
			logger.info(String.format("Delete: node %s doesn't exist, do nothing.", path));
		}
	}

	@Override
	public void watch(String path, NodeCacheListener listener) throws Exception {
		NodeCache node = watchMap.get(path);
		if(node == null) {
			node = new NodeCache(curator, path);
			node.start();
			watchMap.put(path, node);
		}
		node.getListenable().addListener(listener);
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
		// Create the node
		boolean result = create(path, data, mode);
		if(result == false) {
			update(path, data);
		}
		watch(path, listener);
	}

	public boolean isConnected() {
		return curator.getZookeeperClient().isConnected();
	}

	@Override
	public void watchChildren(String path, PathChildrenCacheListener listener)
			throws Exception {
		PathChildrenCache node = childrenWatchMap.get(path);
		if(node == null) {
	        node = new PathChildrenCache(curator, path, true);
	        node.start();
	        childrenWatchMap.put(path, node);
		}
        node.getListenable().addListener(listener);
	}

	@Override
	public void createAndWatchChildren(String path,  
			CreateMode mode, PathChildrenCacheListener listener) 
			throws Exception {
		createAndWatchChildren(path, "", mode, listener);
	}
	
	public void createAndWatchChildren(String path, String data, 
			CreateMode mode, PathChildrenCacheListener listener) 
			throws Exception {
		boolean result = create(path, data, mode);
		if(result == false) {
			update(path, data);
		}
		watchChildren(path, listener);
	}
}
