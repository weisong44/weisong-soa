package com.weisong.soa.proxy.routing.config;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;

abstract public class TestRoutingBase {
	
	final static public String[] files = new String[] {
	  	"/routing/routing_more_forward_to.txt"
	  ,	"/routing/routing_dropall.txt"	
	  ,	"/routing/routing_passall.txt"	
	  ,	"/routing/routing_test.txt"	
	};
	
	private RRoutingConfigFactory factory;
	
	@Before
	public void setup() {
		factory = new RRoutingConfigFactory();
	}
	
	public RRoutingConfig parse(String fname) throws IOException {
		System.out.println("==> File " + fname);
		InputStream in = getClass().getResourceAsStream(fname);
		return factory.parse(in); 
	}
}
