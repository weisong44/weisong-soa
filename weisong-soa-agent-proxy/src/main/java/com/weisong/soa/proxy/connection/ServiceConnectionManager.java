package com.weisong.soa.proxy.connection;

import io.netty.channel.Channel;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import lombok.Getter;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weisong.soa.core.zk.config.ZkPropertyChangeRegistry;
import com.weisong.soa.core.zk.service.ZkServiceHandlerReadOnly;
import com.weisong.soa.core.zk.util.ZkServiceUitl;
import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.proxy.engine.ProxyEngine;
import com.weisong.soa.proxy.routing.config.RRoutingConfig;
import com.weisong.soa.proxy.routing.config.RRoutingConfigFactory;
import com.weisong.soa.proxy.routing.config.RTarget;
import com.weisong.soa.service.ServiceConst;
import com.weisong.soa.service.ServiceDescriptor;

class ServiceConnectionManager {

	final private Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private ConnectionManager connMgr;
	
	@Getter private ServiceDescriptor desc;
	
	private ProxyEngine engine;
	
	private ZkServiceHandlerReadOnly zkHandler;
	private String zkPath;
	
	private RRoutingConfigFactory routingConfigFactory;
	private RRoutingConfig routingConfig;
	
    private Map<String, ConnectionPool> targetToPoolMap = new HashMap<>();
    private Set<String> availTargetSet = new HashSet<>();
    
	public ServiceConnectionManager(ConnectionManager connMgr, ServiceDescriptor desc, 
			ZkPropertyChangeRegistry propsChangeRegistry, 
			ProxyEngine engine, ZkServiceHandlerReadOnly zkHandler) 
			throws Exception {
		this.connMgr = connMgr;
		this.desc = desc;
		this.engine = engine;
		this.zkHandler = zkHandler;
		this.zkPath = ZkServiceUitl.getRegistryParentPath(
				desc.getDomain(), desc.getService());
		this.routingConfigFactory = new RRoutingConfigFactory();
		
		// Read the routing configuration
		String path = ZkServiceUitl.getConfigNodePath(desc);
		zkHandler.getZkClient().watch(path, new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				logger.info(String.format(
					"Configuration for %s changed on ZK, read again", getDesc()));
				readConfig();
			}
		});
		
		// Read registered nodes
		zkHandler.getZkClient().watchChildren(zkPath, new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
					throws Exception {
				logger.info("Target registration changed on ZK, read again");
				readTargets();
			}
		});
		
		logger.info(String.format("Service connection manager for %s created", desc));
		
	}
	
	private void readConfig() throws Exception  {
		Properties props = zkHandler.readConfig(desc);
		String config = props.getProperty(ServiceConst.PROP_ROUTING_CONFIG);
		if(config == null) {
			throw new RuntimeException(String.format(
					"Routing config not found in ZK for %s", desc));
		}
		if(config.contains("\n") == false) {
			config = config.replace(";", "\n");
		}
		ByteArrayInputStream in = new ByteArrayInputStream(config.getBytes());
		RRoutingConfig newRoutingConfig = routingConfigFactory.parse(in);
		
		// Start the new config
		newRoutingConfig.getProc().start();
		
		// Retain all required pools
		for(RTarget t : newRoutingConfig.getAllTargets()) {
			String connStr = t.getTarget();
			synchronized (this) {
				ConnectionPool pool = targetToPoolMap.get(connStr);
				if(pool == null) {
					pool = new ConnectionPool(engine, connStr, 5);
					targetToPoolMap.put(connStr, pool);
					pool.addListener(connMgr);
					if(availTargetSet.contains(connStr)) {
						pool.setRegistered(true);
					}
				}
				else {
					// TODO: Simulate connection, is this the best way ?!?!
					((RTarget.Proc) t.getProc()).connected(null, pool);
				}
				pool.addListener((RTarget.Proc) t.getProc());
			}
		}

		// Delay for new pools to build up
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Switch to new config
		RRoutingConfig oldRoutingConfig = routingConfig;
		routingConfig = newRoutingConfig;
		
		if(oldRoutingConfig != null) {
			oldRoutingConfig.getProc().stop();
		}
		
		// Release any unused pools
		if(oldRoutingConfig != null) {
			for(RTarget t : oldRoutingConfig.getAllTargets()) {
				String connStr = t.getTarget();
				synchronized (this) {
					ConnectionPool pool = targetToPoolMap.get(connStr);
					pool.removeListener((RTarget.Proc) t.getProc());
					if(pool.getListeners() <= 1) {
						targetToPoolMap.remove(connStr);
						pool.close();
					}
				}
			}
		}
	}
	
	private void readTargets() 
			throws Exception {
		
		List<String> targets = zkHandler.getZkClient().getChildren(zkPath);
		synchronized (this) {
			availTargetSet.clear();
			for(String target : targets) {
				String[] tokens = target.split(":");
				final String connStr = tokens[0] + ":" + tokens[1];
				availTargetSet.add(connStr);
				if(targetToPoolMap.containsKey(connStr) == false) {
					logger.warn(String.format(
							"Target %s is not included in routing config", connStr));
				}
			}
			for(Map.Entry<String, ConnectionPool> e : targetToPoolMap.entrySet()) {
				String target = e.getKey();
				ConnectionPool pool = e.getValue();
				boolean isReg = availTargetSet.contains(target);
				pool.setRegistered(isReg);
			}
		}
	}
	
	public Channel getConnection(RequestContext ctx) {
		if(routingConfig == null || targetToPoolMap.isEmpty()) {
			return null;
		}
		RTarget target = routingConfig.getProc().selectTarget(ctx);
		if(target == null) {
			return null;
		}
		synchronized (this) {
			ConnectionPool pool = targetToPoolMap.get(target.getTarget());
			return pool.getConnection();
		}
	}
}
