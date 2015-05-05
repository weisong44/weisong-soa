package com.weisong.soa.core.zk;

import java.util.Properties;

import junit.framework.Assert;

import org.junit.Test;

import com.weisong.soa.core.zk.config.ZkPropertyChangeRegistry.Listener;

public class TestZkPropertyPlaceholderConfigurerEnabled extends TestZkPropertyPlaceholderConfigurerBase {
	
	@Test
	public void testInitialConfig() {
		Assert.assertEquals("zkValue", prop1);
	}
	
	@Test
	public void testConfigUpdate() throws Exception {
		final String[] result = new String[1];
		changeReg.addPropertyChangeListener(new Listener() {
			@Override
			public void propertyChanged(Properties props) {
				result[0] = props.getProperty("prop1");
			}
		});
		// Update the prop node
		final String content = "prop1=12345";
		helperZkClients.update(path, content);
		
		Thread.sleep(200);

		Assert.assertEquals(result[0], "12345");
	}
}
