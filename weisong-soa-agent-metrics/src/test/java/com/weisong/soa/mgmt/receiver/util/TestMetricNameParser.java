package com.weisong.soa.mgmt.receiver.util;

import junit.framework.Assert;

import org.junit.Test;

public class TestMetricNameParser {
	
	String data = "response-time-99[address=192.168.1.96][port=4567][path=/hello][method=GET]";
	
	private MetricNameParser parser = new MetricNameParser();
	
	@Test
	public void testNameParsing() throws Exception {
		MetricNameParser.Result result = parser.parse(data);
		Assert.assertEquals("response-time-99", result.name);
		Assert.assertEquals(4,  result.tags.size());
		Assert.assertEquals("192.168.1.96", result.tags.get("address"));
		Assert.assertEquals("4567", result.tags.get("port"));
		Assert.assertEquals("/hello", result.tags.get("path"));
		Assert.assertEquals("GET", result.tags.get("method"));
	}
}
