package com.weisong.soa.proxy.routing.config;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.weisong.soa.util.JsonUtil;

public class TestRoutingConfigFactory extends TestRoutingBase {
	
	@Test
	public void testParsing() throws IOException {
		for(String fname : files) {
			RRoutingConfig config = parse(fname);
			System.out.println(JsonUtil.toJsonString(config));
			Assert.assertNotNull(config);
		}
	}
}
