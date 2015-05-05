package com.weisong.soa.core.zk;

import java.util.Properties;

import junit.framework.Assert;

import org.junit.Test;

import com.weisong.soa.core.zk.config.ZkPropertyChangeRegistry.Listener;

public class TestZkPropertyPlaceholderConfigurerDisabled extends TestZkPropertyPlaceholderConfigurerBase {
	
	static {
		// Disable ZK
		System.setProperty(ZkConst.ZK_DISABLED, "true");
	}

	@Test
	public void testDisabled() {
		Assert.assertNotSame("zkValue", prop1);
	}
	
	@Test
	public void testUpdateDisabled() throws Exception {
		final String[] result = new String[1];
		changeReg.addPropertyChangeListener(new Listener() {
			@Override
			public void propertyChanged(Properties props) {
				Assert.fail("Should never reach here!");
			}
		});
		// Update the prop node
		final String content = "prop1=12345";
		helperZkClients.update(path, content);
		
		Thread.sleep(200);

		Assert.assertNull(result[0]);
	}
}
