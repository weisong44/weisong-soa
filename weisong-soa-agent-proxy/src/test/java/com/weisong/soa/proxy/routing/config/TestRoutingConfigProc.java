package com.weisong.soa.proxy.routing.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.weisong.soa.proxy.RequestContext;
import com.weisong.soa.proxy.connection.ConnectionPool;
import com.weisong.soa.proxy.routing.config.RRoutingConfig.SelectedTarget;
import com.weisong.soa.service.ServiceDescriptor;

public class TestRoutingConfigProc extends TestRoutingBase {
	@Test
	public void testWithConnections() throws IOException {
		RRoutingConfig config = parse(files[0]);
		ServiceDescriptor desc = new ServiceDescriptor("test", "test", "0.0.1");
		RequestContext ctx = new RequestContext(null, null, 
				UUID.randomUUID().toString(), null, desc, 1000, null);
		
		config.getProc().start();
		
		for(RTarget target : config.getAllTargets()) {
			ConnectionPool pool = mock(ConnectionPool.class);
			when(pool.getSize()).thenReturn(5); // Mock some connections
			RTarget.Proc proc = ((RTarget.Proc) target.getProc());
			proc.connected(null, pool);
		}

		SelectedTarget result = config.getProc().select(ctx);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getTarget());
	}

	@Test
	public void testWithoutConnections() throws IOException {
		RRoutingConfig config = parse(files[0]);
		ServiceDescriptor desc = new ServiceDescriptor("test", "test", "0.0.1");
		RequestContext ctx = new RequestContext(null, null, 
				UUID.randomUUID().toString(), null, desc, 1000, null);
		
		config.getProc().start();
		
		SelectedTarget result = config.getProc().select(ctx);
		Assert.assertNotNull(result);
		Assert.assertNull(result.getTarget());
	}
}
