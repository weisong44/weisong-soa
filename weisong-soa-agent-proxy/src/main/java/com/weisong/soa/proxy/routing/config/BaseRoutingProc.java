package com.weisong.soa.proxy.routing.config;

import com.weisong.soa.proxy.RequestContext;

abstract public class BaseRoutingProc {

	abstract public RTarget selectTarget(RequestContext ctx);
	abstract public boolean isAvailable();
	
	public void start() {}
	public void stop() {}
	
}
