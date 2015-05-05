package com.weisong.soa.core.zk;

import junit.framework.Assert;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.junit.AfterClass;
import org.junit.Test;

public class TestDefaultZkClient {

	final static private String path = "/path/to/test";
	final static private String data = "Some data here ...";
	
	static private TestingServer server;
	static private DefaultZkClient zkClient;
	
	static {
		try {
			server = new TestingServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
		zkClient = DefaultZkClient.create(server.getConnectString(), 1000, 3);
	}
		
	private boolean nodeEventReceived;
	private boolean childAdded, childDeleted, childUpdated;

	@AfterClass
	static public void cleanup() throws Exception {
		zkClient.delete(path);
		zkClient.close();
	}
	
	@Test
	public void testServerConnection() throws Exception {
		server.stop();
		for(int i = 0; i < 100; i++) {
			if(zkClient.isConnected() == false) {
				break;
			}
			delay(100);
		}
		server.restart();
		for(int i = 0; i < 100; i++) {
			if(zkClient.isConnected()) {
				break;
			}
			delay(100);
		}
		Assert.assertTrue(zkClient.isConnected());
	}
		
	@Test
	public void testCreateAndWatch() throws Exception {

		final String newData = "New data";
		
		zkClient.createAndWatch(path, data, CreateMode.PERSISTENT, new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				String curData = zkClient.get(path);
				System.out.println("Received nodeChanged event, data: " + curData);
				nodeEventReceived = true;
			}
		});
		
		String curData = zkClient.get(path);
		Assert.assertEquals(data, curData);
		
		Thread worker = new Thread() {
			public void run() {
				try {
					zkClient.update(path, newData);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		worker.start();
		worker.join();
		
		for(int i = 0; i < 10; i++) {
			if(nodeEventReceived) {
				break;
			}
			delay(100);
		}
		Assert.assertTrue(nodeEventReceived);
		
	}

	@Test
	public void testCreateAndWatchClient() throws Exception {
		
		zkClient.createAndWatchChildren(path, data, CreateMode.PERSISTENT, new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
					throws Exception {
				System.out.println("Received event: " + event.getType());
				switch(event.getType()) {
				case CHILD_ADDED:
					childAdded = true;
					break;
				case CHILD_UPDATED:
					childUpdated = true;
					break;
				case CHILD_REMOVED:
					childDeleted = true;
					break;
				default:
					break;
				}
			}
		});

		String curData = zkClient.get(path);
		Assert.assertEquals(data, curData);
		
		Thread worker = new Thread() {
			public void run() {
				try {
					String path2 = path + "/a";
					zkClient.create(path2, "Some other data", CreateMode.EPHEMERAL);
					delay(100); // Wait for next watch to register
					zkClient.update(path2, "New data");
					delay(100); // Wait for next watch to register
					zkClient.delete(path2);
					delay(100); // Wait for next watch to register
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		worker.start();
		worker.join();
		
		for(int i = 0; i < 10; i++) {
			if(childDeleted) {
				break;
			}
			delay(100);
		}
		
		Assert.assertTrue(childAdded);
		Assert.assertTrue(childUpdated);
		Assert.assertTrue(childDeleted);
		
	}

	private void delay(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
