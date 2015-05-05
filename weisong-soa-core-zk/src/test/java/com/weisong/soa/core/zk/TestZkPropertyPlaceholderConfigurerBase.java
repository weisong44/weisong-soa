package com.weisong.soa.core.zk;

import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.weisong.soa.core.zk.bootstrap.BootstrapZkClient;
import com.weisong.soa.core.zk.config.ZkPropertyChangeRegistry;
import com.weisong.soa.core.zk.config.ZkPropertyJavaConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
	ZkPropertyJavaConfig.class
})
abstract public class TestZkPropertyPlaceholderConfigurerBase {
	
	static protected String path = "/test/data";
	static protected TestingServer server;
	static protected DefaultZkClient helperZkClients;

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
		// Path mapping
		System.setProperty(ZkConst.ZK_NODE_PATH, path);
		String data = String.format("%s=%s\n", path, server.getConnectString());
		// Create path data mapping
		helperZkClients.create("/bootstrap/data", data, CreateMode.PERSISTENT);
		// Create the prop node
		String content = "prop1=zkValue";
		helperZkClients.create(path, content, CreateMode.EPHEMERAL);
	}
	
	@Autowired protected ZkPropertyChangeRegistry changeReg;
	
	@Value("${prop1}") String prop1;
	
	@AfterClass
	static public void cleanup() throws Exception {
		helperZkClients.close();
	}
}
