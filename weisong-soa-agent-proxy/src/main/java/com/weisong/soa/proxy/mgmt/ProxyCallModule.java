package com.weisong.soa.proxy.mgmt;

import com.weisong.soa.agent.Module;
import com.weisong.soa.agent.impl.BaseCallModule;

public class ProxyCallModule extends BaseCallModule {

	public ProxyCallModule(Module parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "proxy-call-stats";
	}

	@Override
	public String getType() {
		return "proxy-call-stats";
	}

	@Override
	public String getDescription() {
		return "The proxy call statistics";
	}
}
