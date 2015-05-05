package com.weisong.soa.core.zk;

import junit.framework.Assert;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.weisong.common.javaconfig.CommonPropertyJavaConfig;
import com.weisong.soa.core.zk.bootstrap.BootstrapZkClient;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
	CommonPropertyJavaConfig.class
})
public class TestBootstrapZkClient {

	static private int n = 4;
	static private BootstrapZkClient staticBootstrapZkClient;
	static private TestingServer[] servers = new TestingServer[n];
	static private DefaultZkClient[] zkClients = new DefaultZkClient[n];

	static private String[] rootPaths = new String[n];
	static private String[] paths = new String[rootPaths.length];
	static private String[] contents = new String[rootPaths.length];
	
	static {
		try {
			for(int i = 0; i < rootPaths.length; i++) {
				servers[i] = new TestingServer();
				zkClients[i] = DefaultZkClient.create(servers[i].getConnectString()); 
				rootPaths[i] = "/path" + i;
				paths[i] = rootPaths[i] + "/some/data";
				contents[i] = paths[i] + "=data" + i;
			}
			
			BootstrapZkClient.overrideBootstrapConnStr = servers[0].getConnectString();
			
			prepareData();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static private void prepareData() throws Exception {
		// Path mapping
		String data = "";
		for(int i = 1; i < 3; i++) {
			data += String.format("%s=%s\n", rootPaths[i], servers[i].getConnectString());
			zkClients[i].create(paths[i], contents[i], CreateMode.EPHEMERAL);
		}
		// Create path data mapping
		zkClients[0].create("/bootstrap/data", data, CreateMode.PERSISTENT);
	}
	
	private BootstrapZkClient bootstrapZkClient;
	
	@AfterClass
	static public void cleanup() throws Exception {
		for(ZkClient c : zkClients) {
			c.close();
		}
		for(TestingServer s : servers) {
			s.close();
		}
		staticBootstrapZkClient.close();
	}
	
	@Before
	public void prepare() throws Exception {
		bootstrapZkClient = new BootstrapZkClient();
	}

	@Test
	public void testBootstrap() throws Exception {
		// Remember for cleanup
		staticBootstrapZkClient = bootstrapZkClient;
		// Default path data
		String data = bootstrapZkClient.get(paths[0]);
		Assert.assertNull(data);
		// Path data
		data = bootstrapZkClient.get(paths[1]);
		Assert.assertEquals(contents[1], data);
		// Path2
		data = bootstrapZkClient.get(paths[2]);
		Assert.assertEquals(contents[2], data);
	}
	
	@Test
	public void testCrud() throws Exception {
		String path = rootPaths[3] + "/test";
		String data = "Data003";
		// Create
		bootstrapZkClient.create(path, data, CreateMode.EPHEMERAL);
		// Read
		String curData = bootstrapZkClient.get(path);
		Assert.assertEquals(curData, data);
		// Update
		data = "Data003-new";
		bootstrapZkClient.update(path, data);
		curData = bootstrapZkClient.get(path);
		Assert.assertEquals(curData, data);
		// Delete
		bootstrapZkClient.delete(path);
		curData = bootstrapZkClient.get(path);
		Assert.assertNull(curData);
	}
	
	@Test
	public void testWatch() throws Exception {
		final String[] result = new String[2];
		String path = rootPaths[2] + "/test";
		String data = "Data002";
		bootstrapZkClient.createAndWatch(path, CreateMode.PERSISTENT, new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				result[0] = "OK";
			}
		});

		bootstrapZkClient.watch(path, new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				result[1] = "OK";
			}
		});
		
		bootstrapZkClient.update(path, data);
		delay(100); // Wait for result
		Assert.assertEquals("OK", result[0]);
		Assert.assertEquals("OK", result[1]);
		
		result[0] = null;
		result[1] = null;
		bootstrapZkClient.createAndWatchChildren(path, CreateMode.PERSISTENT, new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
					throws Exception {
				result[0] = "OK";
			}
		});
		
		bootstrapZkClient.watchChildren(path, new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
					throws Exception {
				result[1] = "OK";
			}
		});
		
		bootstrapZkClient.create(path + "/1", data, CreateMode.PERSISTENT);
		delay(100); // Wait for result
		Assert.assertEquals("OK", result[0]);
		Assert.assertEquals("OK", result[1]);
	}

	@Test
	public void testMappingChange() throws Exception {
		// Path mapping
		String data = "";
		for(int i = 1; i < 3; i++) {
			// Shift one
			data += String.format("%s=%s\n", rootPaths[i], servers[i+1].getConnectString());
		}
		zkClients[0].update("/bootstrap/data", data);
		
		delay(100); // Wait for change to propagate
		
		// Path data
		data = bootstrapZkClient.get(paths[1]);
		Assert.assertNull(data);

		zkClients[2].create(paths[1], contents[3], CreateMode.EPHEMERAL);
		
		data = bootstrapZkClient.get(paths[1]);
		Assert.assertEquals(contents[3], data);
	}
	
	private void delay(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
