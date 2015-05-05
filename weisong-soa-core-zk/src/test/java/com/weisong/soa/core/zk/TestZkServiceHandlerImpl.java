package com.weisong.soa.core.zk;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.weisong.soa.core.zk.bootstrap.BootstrapZkClient;
import com.weisong.soa.core.zk.config.ZkPropertyJavaConfig;
import com.weisong.soa.core.zk.service.ZkServiceHandlerImpl;
import com.weisong.soa.service.ServiceDescriptor;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
		ZkPropertyJavaConfig.class
	  , ZkServiceHandlerImpl.JavaConfig.class
})
public class TestZkServiceHandlerImpl {
	
	static protected TestingServer server;
	static protected DefaultZkClient helperZkClients;
	static protected String path = "/test/data";

	static {
		try {
			server = new TestingServer();
			helperZkClients = DefaultZkClient.create(server.getConnectString()); 
			BootstrapZkClient.overrideBootstrapConnStr = server.getConnectString();
			
			prepareData();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static protected void prepareData() throws Exception {
		// Create the prop node
		System.setProperty(ZkConst.ZK_NODE_PATH, path);
		String content = "prop1=zkValue";
		helperZkClients.create(path, content, CreateMode.EPHEMERAL);
		// Path mapping
		String data = String.format("%s\n%s\n", 
				String.format("%s=%s\n", ZkConst.ZK_CONFIG_ROOT, server.getConnectString())
			  ,	String.format("%s=%s\n", ZkConst.ZK_REGISTRY_ROOT, server.getConnectString()));
		// Create path data mapping
		helperZkClients.create("/bootstrap/data", data, CreateMode.PERSISTENT);
	}

	
	@Autowired private ZkServiceHandlerImpl handler;
	
	@Test
	public void testServiceHandler() throws Exception {
		
		final ServiceDescriptor desc = new ServiceDescriptor(
				"test", "my-service", "1.1.1a");
		final int port1 = 1001;
		final int port2 = 1002;
		
		final Properties props = new Properties();
		props.setProperty("name", "John");
		props.setProperty("address", "My new addrss");
		
		// Create listener data structure
		final Properties newProps = new Properties();
		final List<String> newEndpoints = new LinkedList<>();
		
		// Test write and read back
		handler.writeConfig(desc, props);
		newProps.putAll(handler.readConfig(desc));
		Assert.assertEquals(props, newProps);

		// Test watch
		props.clear();
		props.put("1", "100");
		handler.writeConfig(desc, props);
		props.clear();
		props.putAll(handler.readConfig(desc, new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				newProps.clear();
				newProps.putAll(handler.readConfig(desc));
			}
		}));
		Thread.sleep(200);
		Assert.assertEquals(props, newProps);
		
		// Test children
		handler.register(desc, port1);
		List<String> endpoints = handler.getRegisteredEndpoints(desc.getDomain(), desc.getService());
		Assert.assertEquals(1, endpoints.size());
		
		// Test watch children
		endpoints = handler.getRegisteredEndpoints(desc.getDomain(), desc.getService(), new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
					throws Exception {
				newEndpoints.clear();
				newEndpoints.addAll(handler.getRegisteredEndpoints(desc.getDomain(), desc.getService()));
			}
		});
		handler.register(desc, port2);
		Thread.sleep(200);
		Assert.assertEquals(2, newEndpoints.size());
		
		// Test error case
		try {
			handler.register(desc, port2);
		} 
		catch (Exception e) {
			newEndpoints.clear();
		}
		Thread.sleep(200);
		Assert.assertEquals(0, newEndpoints.size());
	}
}
