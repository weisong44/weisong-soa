package com.weisong.soa.proxy.routing.config;

import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.service.ServiceDescriptor;

public class TestTargetGroupProc extends TestRoutingBase {
	@Test
	public void testProc() throws IOException {
		RRoutingConfig config = parse(files[0]);
		ServiceDescriptor desc = new ServiceDescriptor("test", "test", "0.0.1");
		RequestContext ctx = new RequestContext(null, null, 
				UUID.randomUUID().toString(), null, desc, 1000, null);
		
		config.getProc().start();
		RTarget target = config.getProc().selectTarget(ctx);
		Assert.assertNull(target);
	}
}
