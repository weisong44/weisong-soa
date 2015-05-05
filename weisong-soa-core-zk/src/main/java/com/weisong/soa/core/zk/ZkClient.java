package com.weisong.soa.core.zk;

import java.io.Closeable;
import java.util.List;

import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;

public interface ZkClient extends Closeable {
	
	public void close();
	
	public boolean create(String path, String data, CreateMode mode) throws Exception;
	
	public String get(String path) throws Exception;
	
	public List<String> getChildren(String path) throws Exception;
	
	public void update(String path, String data) throws Exception;
	
	public void delete(String path) throws Exception;
	
	public void watch(String path, NodeCacheListener listener) 
			throws Exception;
	
	public void createAndWatch(String path, CreateMode mode, NodeCacheListener listener) 
			throws Exception;
	
	public void createAndWatch(String path, String data, CreateMode mode, 
			NodeCacheListener listener) 
			throws Exception;
	
	public void watchChildren(String path, PathChildrenCacheListener listener) 
			throws Exception;
	
	public void createAndWatchChildren(String path,  
			CreateMode mode, PathChildrenCacheListener listener) 
			throws Exception;
	
	public void createAndWatchChildren(String path, String data, 
			CreateMode mode, PathChildrenCacheListener listener) 
			throws Exception;

}
